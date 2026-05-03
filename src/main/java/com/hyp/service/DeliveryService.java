package com.hyp.service;

import com.hyp.client.PidgeClient;
import com.hyp.constants.Constants;
import com.hyp.delivery.DeliveryOrchestrator;
import com.hyp.delivery.adloggs.AdloggsStatusMapper;
import com.hyp.delivery.adloggs.AdloggsWebhookPayload;
import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.DeliveryQuoteRecord;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.entity.RiderRecord;
import com.hyp.enums.*;
import com.hyp.event.OrderEventPublisher;
import com.hyp.exception.DeliveryException;
import com.hyp.model.*;
import com.hyp.model.DeliveryOrderStatus.DeliveryFulfillment;
import com.hyp.model.DeliveryOrderStatus.DeliveryOrderData;
import com.hyp.model.DeliveryOrderStatus.Rider;
import com.hyp.model.DeliveryQuote.DeliveryNetworks;
import com.hyp.observability.*;
import com.hyp.repository.DeliveryQuoteRepository;
import com.hyp.repository.DeliveryRepository;
import com.hyp.repository.RiderRecordRepository;
import com.hyp.request.DeliveryOrderRequest;
import com.hyp.request.DeliveryQuoteRequest;
import com.hyp.request.PosRiderUpdateRequest.RiderDetails;
import com.hyp.temporal.service.DeliveryWorkflowService;
import com.hyp.translation.DeliveryRequestTranslation;
import com.hyp.util.CommonUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DeliveryService extends BaseServiceImpl<Delivery, String> {

    @Autowired
    DeliveryRepository deliveryRepository;

    @Autowired
    DeliveryQuoteRepository deliveryQuoteRepository;

    @Autowired
    RestaurantService restaurantService;

    @Autowired
    PosServiceFactory posServiceFactory;

    @Autowired
    OrderService orderService;

    @Autowired
    PidgeClient pidgeClient;

    @Autowired
    RedisService redisService;

    @Autowired
    CustomerService customerService;

    @Autowired
    AddressService addressService;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private RiderRecordRepository riderRecordRepository;

    @Autowired
    OneSignalAlertService oneSignalAlertService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    ApplicationMetrics metrics;

    @Value("${app.domain}")
    private String appDomain;

    @Autowired
    DeliveryOrchestrator deliveryOrchestrator;

    @Autowired
    DeliveryWorkflowService deliveryWorkflowService;

    @Autowired
    com.hyp.delivery.DeliveryLifecycleService deliveryLifecycleService;

    @Autowired
    com.hyp.delivery.DeliveryPartnerEvaluator partnerEvaluator;

    @Autowired
    com.hyp.delivery.PartnerScoreTracker scoreTracker;

    @Autowired
    DeliveryRequestTranslation deliveryRequestTranslation;

    public Delivery findByOrderId(String orderId) {
        return deliveryRepository.findFirstByOrderIdAndIsDeletedFalseOrderByCreatedAtDesc(orderId);
    }

    public Delivery findByOrderIdIncludingDeleted(String orderId) {
        return deliveryRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId);
    }

    public List<Delivery> findByOrderIds(List<String> orderIds) {
        return deliveryRepository.findByOrderIdInAndIsDeletedFalse(orderIds);
    }

    public Delivery findByDeliveryOrderId(String deliveryOrderId) {
        return deliveryRepository.findByDeliveryOrderIdAndIsDeletedFalse(deliveryOrderId);
    }

    public Delivery findByDeliveryOrderIdIncludingDeleted(String deliveryOrderId) {
        return deliveryRepository.findByDeliveryOrderId(deliveryOrderId);
    }

    public void createDeliveryForOrder(Order order) {
        ObservabilityContext.setOrderContext(order.getId(), order.getRestaurantId());
        try {
            Restaurant restaurant = restaurantService.findById(order.getRestaurantId());

            if (restaurant.getDeliveryModel() == DeliveryModel.SELF) {
                log.info("Restaurant {} handles own delivery — skipping provider routing", order.getRestaurantId());
                return;
            }

            if (restaurant.getDeliveryModel() == DeliveryModel.DEDICATED) {
                log.info("Dedicated rider flow not yet implemented for orderId={}", order.getId());
                return;
            }

            CompletableFuture<Customer> customerFuture =
                    CompletableFuture.supplyAsync(() -> customerService.findById(order.getCustomerId()));
            CompletableFuture<Address> addressFuture = CompletableFuture.supplyAsync(
                    () -> addressService.findById(order.getDeliveryDetails().getAddressId()));
            CompletableFuture.allOf(customerFuture, addressFuture).join();

            Customer customer = customerFuture.join();
            Address address = addressFuture.join();

            Delivery delivery = deliveryOrchestrator.createDelivery(order, restaurant, address, customer);
            save(delivery);

            // Adloggs starts rider search immediately on creation — start assignment watch now.
            // Pidge watch starts after the explicit fulfill step (triggered at ACCEPTED).
            if (delivery.getProvider() == DeliveryPartner.ADLOGGS) {
                deliveryWorkflowService.startAssignmentWatch(
                        order.getId(), delivery.getId(), delivery.getProvider().name());
            }

            metrics.count(MetricsEvent.DELIVERY, MetricTag.ACTION, "create", MetricTag.RESULT, "success");
            metrics.incrementGauge("delivery_pending");
        } catch (DeliveryException e) {
            log.error("Delivery creation failed orderId={}", order.getId(), e);
            metrics.count(MetricsEvent.DELIVERY, MetricTag.ACTION, "create", MetricTag.RESULT, "failed");
            orderService.updateOrderStatus(order.getId(), OrderStatusType.DELIVERY_ERROR);
            oneSignalAlertService.notifyErrorResponseAlert(
                    order.getId(), "Delivery creation failed: " + e.getMessage(), "DELIVERY");
        } finally {
            ObservabilityContext.clear();
        }
    }

    /**
     * Switch the active delivery to the alternate provider.
     * Called by DeliveryAssignmentActivitiesImpl on timeout or manually via API.
     */
    public void switchDeliveryProvider(String orderId, String deliveryId) {
        switchDeliveryProvider(orderId, deliveryId, false);
    }

    public void switchDeliveryProvider(String orderId, String deliveryId, boolean forceOverride) {
        ObservabilityContext.setOrderId(orderId);
        try {
            Delivery activeDelivery = deliveryId != null ? findById(deliveryId) : findByOrderId(orderId);

            if (activeDelivery == null) {
                log.warn("switchDeliveryProvider: no active delivery for orderId={}", orderId);
                return;
            }

            if (!activeDelivery.isSwitchable()) {
                log.info("switchDeliveryProvider: delivery {} not switchable (post-pickup)", activeDelivery.getId());
                return;
            }

            Order order = orderService.findById(orderId);

            int maxSwitches = redisService
                    .getRedisData(Constants.REDIS_KEY_MAX_DELIVERY_SWITCHES)
                    .map(Integer::parseInt)
                    .orElse(2);

            if (order.getDeliverySwitchCount() >= maxSwitches) {
                log.warn("Max delivery switches ({}) reached for orderId={}", maxSwitches, orderId);
                orderService.updateOrderStatus(orderId, OrderStatusType.DELIVERY_ERROR);
                notificationService.sendInternalGroupNotification(
                        Constants.META_GENERIC_ALERT_TEMPLATE,
                        List.of(
                                "Delivery switch limit reached",
                                orderId,
                                "Switched " + order.getDeliverySwitchCount()
                                        + " times — manual intervention required"));
                oneSignalAlertService.notifyErrorResponseAlert(
                        orderId, "Max delivery switches reached (" + maxSwitches + ")", "DELIVERY");
                return;
            }

            Restaurant restaurant = restaurantService.findById(order.getRestaurantId());

            // autoFallback=false — automatic switch not allowed; alert ops for manual intervention
            Restaurant.DeliveryConfig deliveryConfig = restaurant.getDeliveryConfig();
            if (!forceOverride && deliveryConfig != null && !deliveryConfig.isAutoFallback()) {
                log.warn("autoFallback disabled — switch blocked for orderId={}, alerting ops", orderId);
                oneSignalAlertService.notifyErrorResponseAlert(
                        orderId,
                        "Rider not assigned — autoFallback is disabled for this restaurant. Manual intervention required.",
                        "DELIVERY");
                return;
            }

            Address address = addressService.findById(order.getDeliveryDetails().getAddressId());
            Customer customer = customerService.findById(order.getCustomerId());

            Delivery newDelivery =
                    deliveryOrchestrator.switchDelivery(activeDelivery, order, restaurant, address, customer);

            activeDelivery.setDeleted(true);
            save(activeDelivery);
            save(newDelivery);

            order.setDeliverySwitchCount(order.getDeliverySwitchCount() + 1);
            orderService.save(order);

            // Pidge requires a separate fulfillment step after order creation
            if (newDelivery.getProvider() == DeliveryPartner.PIDGE) {
                String fulfillMode =
                        redisService.getRedisData(Constants.REDIS_KEY_FULFILL).orElse("smart");
                if (fulfillMode.equalsIgnoreCase("smart")) {
                    processDeliverySmartFulfill(newDelivery, Constants.API);
                } else {
                    processDeliveryStandardFulfill(newDelivery, Constants.API);
                }
            }

            deliveryWorkflowService.startAssignmentWatch(
                    orderId,
                    newDelivery.getId(),
                    newDelivery.getProvider() != null
                            ? newDelivery.getProvider().name()
                            : null);
            log.info(
                    "Provider switched orderId={} from {} to {}",
                    orderId,
                    activeDelivery.getProvider(),
                    newDelivery.getProvider());
        } catch (Exception e) {
            log.error("switchDeliveryProvider failed for orderId={}: {}", orderId, e.getMessage(), e);
            orderService.updateOrderStatus(orderId, OrderStatusType.DELIVERY_ERROR);
        } finally {
            ObservabilityContext.clear();
        }
    }

    public DeliveryQuoteRecord fetchAndSaveEstimates(
            String restaurantId, String addressId, Restaurant restaurant, Address address) throws DeliveryException {

        Restaurant.DeliveryConfig config = restaurant.getDeliveryConfig();
        DeliveryPartner primary = restaurant.getEffectivePrimaryPartner();
        DeliveryPartner secondary = config != null ? config.getSecondaryPartner() : null;

        CompletableFuture<DeliveryEstimate> primaryFuture = CompletableFuture.supplyAsync(
                () -> deliveryOrchestrator.getProvider(primary).getEstimate(restaurant, address));
        CompletableFuture<DeliveryEstimate> secondaryFuture = secondary != null && secondary != primary
                ? CompletableFuture.supplyAsync(
                        () -> deliveryOrchestrator.getProvider(secondary).getEstimate(restaurant, address))
                : CompletableFuture.completedFuture(null);

        CompletableFuture.allOf(primaryFuture, secondaryFuture).join();

        List<DeliveryEstimate> available = new ArrayList<>();
        DeliveryEstimate primaryEst = primaryFuture.join();
        if (primaryEst != null && primaryEst.isAvailable()) available.add(primaryEst);
        DeliveryEstimate secondaryEst = secondaryFuture.join();
        if (secondaryEst != null && secondaryEst.isAvailable()) available.add(secondaryEst);

        if (available.isEmpty()) {
            throw new DeliveryException("fetchEstimates", "No delivery provider available for this location");
        }

        DeliveryQuoteRecord record = new DeliveryQuoteRecord();
        record.setAddressId(addressId);
        record.setEstimates(available);
        record.setPrimary(available.get(0).getProvider());
        record.setSecondary(available.size() > 1 ? available.get(1).getProvider() : null);
        record.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        // Backward compat: keep Pidge network token in legacy field
        available.stream()
                .filter(e -> e.getProvider() == DeliveryPartner.PIDGE
                        && e.getRawData() instanceof DeliveryQuote.DeliveryNetworks)
                .findFirst()
                .ifPresent(e -> record.setNetwork((DeliveryQuote.DeliveryNetworks) e.getRawData()));

        log.info(
                "Saved delivery estimates restaurantId={} primary={} secondary={}",
                restaurantId,
                record.getPrimary(),
                record.getSecondary());
        return deliveryQuoteRepository.save(record);
    }

    public Delivery createManualDelivery(Order order, DeliveryPartner partner) throws DeliveryException {
        if (findByOrderId(order.getId()) != null) {
            throw new DeliveryException("Active delivery already exists for order: " + order.getId());
        }
        Customer customer = customerService.findById(order.getCustomerId());
        if (customer == null) throw new DeliveryException("Customer not found: " + order.getCustomerId());

        Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
        if (restaurant == null) throw new DeliveryException("Restaurant not found: " + order.getRestaurantId());

        Address address = addressService.findById(order.getDeliveryDetails().getAddressId());
        if (address == null)
            throw new DeliveryException(
                    "Address not found: " + order.getDeliveryDetails().getAddressId());

        DeliveryOrderRequest deliveryOrderRequest =
                deliveryRequestTranslation.getDeliveryOrderRequest(restaurant, address, customer, order);

        Delivery delivery = deliveryRequestTranslation.getDeliveryEntity(deliveryOrderRequest);
        delivery.setId(CommonUtils.genId());
        delivery.setDeliveryOrderId(order.getId());
        delivery.setChannel(DeliveryPartner.MANUAL.name());
        delivery.setService(partner.name().toLowerCase());
        delivery.setStatus(DeliveryOrderStatusType.PENDING);
        return save(delivery);
    }

    public Delivery updateManualDeliveryStatus(Delivery delivery, Order order) {
        DeliveryFulfillment fulfillment = delivery.getFulfillment();
        DeliveryFulfillStatusType fulfillStatus = fulfillment != null ? fulfillment.getStatus() : null;

        if (fulfillment != null && fulfillStatus != null) {
            DeliveryOrderStatus.Log log = new DeliveryOrderStatus.Log();
            log.setTimestamp(java.time.Instant.now().toString());
            log.setStatus(fulfillStatus.name());
            log.setAttemptType("FORWARD");
            if (fulfillment.getLogs() == null) {
                fulfillment.setLogs(new ArrayList<>());
            }
            fulfillment.getLogs().add(log);
            delivery.setFulfillment(fulfillment);
        }

        if (fulfillStatus == DeliveryFulfillStatusType.DELIVERED) {
            delivery.setStatus(DeliveryOrderStatusType.COMPLETED);
        }

        save(delivery);

        if (fulfillStatus != null) {
            OrderStatusType orderStatus = OrderStatusType.getOrderStatusByDeliveryStatus(fulfillStatus);
            orderService.updateOrderStatus(order.getId(), orderStatus);

            if (posServiceFactory.forRestaurant(order.getRestaurantId()).isPosUpdateRequired(fulfillStatus)) {
                posServiceFactory.forRestaurant(order.getRestaurantId()).updatePosRiderStatus(delivery, order);
            }
        }
        return delivery;
    }

    public DeliveryQuote getDeliveryQuote(DeliveryQuoteRequest deliveryQuoteRequest) throws DeliveryException {
        return pidgeClient.getDeliveryQuote(deliveryQuoteRequest);
    }

    public DeliveryQuoteRecord saveDeliveryQuote(String restaurantId, String addressId, DeliveryNetworks network) {
        DeliveryQuoteRecord record = new DeliveryQuoteRecord();
        record.setRestaurantId(restaurantId);
        record.setAddressId(addressId);
        record.setNetwork(network);
        record.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        return deliveryQuoteRepository.save(record);
    }

    public DeliveryQuoteRecord findDeliveryQuoteById(String quoteId) {
        return deliveryQuoteRepository.findById(quoteId).orElse(null);
    }

    public DeliveryQuote getServiceability(String deliveryOrderId) throws DeliveryException {
        return pidgeClient.getServiceability(deliveryOrderId);
    }

    public void processDeliveryCallback(Delivery delivery, DeliveryOrderData deliveryOrderData)
            throws DeliveryException {
        ObservabilityContext.setOrderId(delivery.getOrderId());
        log.info(
                "Delivery callback received orderId={} deliveryStatus={} fulfillmentStatus={}",
                delivery.getOrderId(),
                deliveryOrderData.getStatus(),
                deliveryOrderData.getFulfillment() != null
                        ? deliveryOrderData.getFulfillment().getStatus()
                        : "N/A");
        try {
            DeliveryOrderStatusType newStatus =
                    DeliveryOrderStatusType.getDeliveryOrderStatus(deliveryOrderData.getStatus());

            Order order = orderService.findById(delivery.getOrderId());
            delivery.setStatus(newStatus);

            if (newStatus == DeliveryOrderStatusType.PENDING) {
                handlePendingStatus(delivery, deliveryOrderData, order);
            } else if (newStatus == DeliveryOrderStatusType.CANCELLED) {
                orderService.updateOrderStatus(order.getId(), OrderStatusType.DELIVERY_CANCELLED);
                delivery.setDeleted(true);
            } else if (newStatus == DeliveryOrderStatusType.FULFILLED
                    || newStatus == DeliveryOrderStatusType.COMPLETED) {
                handleFulfillmentStatus(delivery, deliveryOrderData, order);
            }

            if (deliveryOrderData.getFulfillment() != null
                    && deliveryOrderData.getFulfillment().getStatus() != null) {
                recordFulfillmentEvent(
                        delivery,
                        order.getRestaurantId(),
                        deliveryOrderData.getFulfillment().getStatus());
            }

            delivery.setFulfillmentHistory(deliveryOrderData.getFulfillmentHistory());
            save(delivery);
        } catch (Exception e) {
            handleDeliveryError("processDeliveryCallback", delivery, e);
        } finally {
            ObservabilityContext.clear();
        }
    }

    private void handlePendingStatus(Delivery delivery, DeliveryOrderData deliveryOrderData, Order order) {
        DeliveryFulfillment fulfillment = deliveryOrderData.getFulfillment();
        if (fulfillment == null) {
            return;
        }
        DeliveryFulfillStatusType fulfillmentStatus = fulfillment.getStatus();
        log.info("Fulfillment update orderStatus={} fulfillmentStatus={}", order.getStatus(), fulfillmentStatus);

        if (fulfillmentStatus == DeliveryFulfillStatusType.CANCELLED) {
            OrderStatusType targetStatus =
                    OrderStatusType.getOrderStatusByDeliveryStatus(DeliveryFulfillStatusType.CANCELLED);
            if (!targetStatus.equals(order.getStatus())) {
                log.info("Rider cancelled delivery");
                orderService.updateOrderStatus(order.getId(), targetStatus);
                orderEventPublisher.publishDeliveryFulfillEvent(delivery);
            }
        }
    }

    private void handleFulfillmentStatus(Delivery delivery, DeliveryOrderData deliveryOrderData, Order order) {
        DeliveryFulfillment deliveryFulfill = deliveryOrderData.getFulfillment();
        DeliveryFulfillStatusType fullFillStatus = deliveryFulfill.getStatus();

        log.info(
                "Processing fulfillment orderStatus={} deliveryStatus={} fulfillmentStatus={}",
                order.getStatus(),
                delivery.getStatus(),
                fullFillStatus);

        if (OrderStatusType.getOrderStatusByDeliveryStatus(fullFillStatus).equals(order.getStatus())) {
            log.debug("Duplicate delivery status - skipping");
            return;
        }

        if (fullFillStatus.equals(DeliveryFulfillStatusType.OUT_FOR_PICKUP)
                || fullFillStatus.equals(DeliveryFulfillStatusType.CREATED)) {
            String redisKey = "delivery:" + delivery.getOrderId() + ":" + fullFillStatus;
            String deliveryDelay = redisService
                    .getRedisData(Constants.REDIS_KEY_DELIVERY_DELAY)
                    .orElse("12");
            redisService.setRedisData(
                    redisKey,
                    fullFillStatus,
                    Duration.ofMinutes(Long.parseLong(deliveryDelay)).toSeconds());
        }

        if (fullFillStatus.equals(DeliveryFulfillStatusType.OUT_FOR_PICKUP)) {
            String locationKey = "rider_location:" + delivery.getOrderId() + ":" + fullFillStatus;
            String riderDelay = redisService
                    .getRedisData(Constants.REDIS_KEY_RIDER_LOCATION_DELAY)
                    .orElse("5");
            redisService.setRedisData(
                    locationKey,
                    fullFillStatus,
                    Duration.ofMinutes(Long.parseLong(riderDelay)).toSeconds());
            // Signal assignment workflow — rider is confirmed assigned
            deliveryWorkflowService.signalRiderAssigned(delivery.getId());
        }

        if (deliveryFulfill.getChannel() != null) {
            try {
                delivery.setNetworkId(
                        Integer.parseInt(deliveryFulfill.getChannel().getId()));
            } catch (NumberFormatException ignored) {
                log.warn(
                        "Non-numeric channel id '{}' for order {}",
                        deliveryFulfill.getChannel().getId(),
                        delivery.getOrderId());
            }
            delivery.setService(deliveryFulfill.getChannel().getName());
        }
        delivery.setPickupNow(true);
        delivery.setFulfillment(deliveryFulfill);

        String trackCode = deliveryFulfill.getTrackCode();
        if (trackCode != null) {
            delivery.getFulfillment().setTrackCode(trackCode);
            delivery.setTrackingUrl("https://t.pidge.in?t=" + trackCode);
        }
        // TODO: Need to Move this saveOrder some where - unwanted save operation
        if (order.getDeliveryTrackingLink() == null) {
            order.setDeliveryTrackingLink(appDomain + "/api/v2/order/track/" + order.getId());
        }
        if (fullFillStatus.equals(DeliveryFulfillStatusType.DELIVERED)) {
            order.setDeliveryPartner(delivery.getProvider().name());
        }
        orderService.save(order);
        orderService.updateOrderStatus(order.getId(), OrderStatusType.getOrderStatusByDeliveryStatus(fullFillStatus));

        // Decrement in-transit gauge when delivery reaches terminal state
        if (isDeliveryTerminalStatus(fullFillStatus)) {
            metrics.decrementGauge("delivery_in_transit");
        }

        // Async: POS rider status update and fraud check are non-critical side effects
        CompletableFuture.runAsync(() -> {
                    try {
                        if (posServiceFactory.forRestaurant(order.getRestaurantId()).isPosUpdateRequired(fullFillStatus)) {
                            posServiceFactory.forRestaurant(order.getRestaurantId()).updatePosRiderStatus(delivery, order);
                        }
                    } catch (Exception e) {
                        log.error("Async POS rider status update failed for order {}", order.getId(), e);
                    }

                    try {
                        if (DeliveryFulfillStatusType.OUT_FOR_PICKUP.equals(fullFillStatus)) {
                            if (delivery.getFulfillment() != null) {
                                Rider rider = delivery.getFulfillment().getRider();
                                if (rider != null) {
                                    RiderDetails riderDetails = new RiderDetails(rider.getName(), rider.getMobile());
                                    checkAndAlertFraudRider(riderDetails, delivery);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("Async fraud rider check failed for order {}", order.getId(), e);
                    }
                })
                .orTimeout(10, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("Async side-effect timed out or failed for order {}: {}", order.getId(), e.getMessage());
                    return null;
                });
    }

    private void checkAndAlertFraudRider(RiderDetails riderDetails, Delivery delivery) {
        if (riderDetails == null) return;

        String riderContact = riderDetails.getRiderContact();
        String riderName = riderDetails.getRiderName();

        if (riderContact == null || riderContact.isBlank()) {
            log.debug("Empty rider contact - skipping fraud check");
            return;
        }

        try {
            RiderRecord record = riderRecordRepository.findByRiderContact(riderContact);

            // don't create — if not present, skip and continue
            if (record == null) {
                log.debug("No RiderRecord found for {} - skipping (won't create).", riderContact);
                return;
            }

            boolean modified = false;

            // increment fraud count
            Integer currentCount = record.getFraudCount();
            int newCount = (currentCount == null ? 0 : currentCount) + 1;
            record.setFraudCount(newCount);
            modified = true;

            // update channel list if present and new
            String channel = delivery == null ? null : delivery.getService();
            if (channel != null && !channel.isBlank()) {
                List<String> channels = record.getChannels();
                if (channels == null) {
                    channels = new ArrayList<>();
                    channels.add(channel);
                    record.setChannels(channels);
                    modified = true;
                } else if (!channels.contains(channel)) {
                    channels.add(channel);
                    modified = true;
                }
            }

            // persist and notify only when changed
            if (modified) {
                riderRecordRepository.save(record);
                log.info("Fraud rider detected riderContact={} fraudCount={}", riderContact, record.getFraudCount());
                oneSignalAlertService.notifyFraudRiderAlert(riderContact, riderName);
            } else {
                log.debug("No updates needed for RiderRecord {} - skipping save/notify", riderContact);
            }
        } catch (Exception e) {
            log.error("Error checking/alerting fraud rider {}: {}", riderContact, e.getMessage(), e);
        }
    }

    /**
     * Process Adloggs webhook callback.
     * Maps integer status → DeliveryFulfillStatusType and re-uses existing fulfillment logic.
     */
    public void processAdloggsCallback(Delivery delivery, AdloggsWebhookPayload payload) throws DeliveryException {
        ObservabilityContext.setOrderId(delivery.getOrderId());
        log.info("Adloggs callback orderId={} statusId={}", delivery.getOrderId(), payload.getOrderStatusId());
        try {
            DeliveryFulfillStatusType fulfillStatus = AdloggsStatusMapper.map(payload.getOrderStatusId());

            Order order = orderService.findById(delivery.getOrderId());

            if (fulfillStatus == DeliveryFulfillStatusType.CANCELLED) {
                delivery.setStatus(DeliveryOrderStatusType.CANCELLED);
                delivery.setDeleted(true);
                save(delivery);
                orderService.updateOrderStatus(order.getId(), OrderStatusType.DELIVERY_CANCELLED);
                return;
            }

            DeliveryFulfillment fulfillment =
                    delivery.getFulfillment() != null ? delivery.getFulfillment() : new DeliveryFulfillment();
            fulfillment.setStatus(fulfillStatus);

            if (payload.getDeliveryStaffDetails() != null) {
                DeliveryOrderStatus.Rider rider = new DeliveryOrderStatus.Rider();
                rider.setName(payload.getDeliveryStaffDetails().getName());
                rider.setMobile(payload.getDeliveryStaffDetails().getPhone());
                fulfillment.setRider(rider);
            }

            // Append fulfillment log entry with location and rider
            DeliveryOrderStatus.Log logEntry = new DeliveryOrderStatus.Log();
            logEntry.setTimestamp(java.time.Instant.now().toString());
            logEntry.setStatus(fulfillStatus.name());
            logEntry.setAttemptType("FORWARD");
            if (payload.getDeliveryStaffDetails() != null
                    && payload.getDeliveryStaffDetails().getCurrentLocation() != null) {
                AdloggsWebhookPayload.CurrentLocation loc =
                        payload.getDeliveryStaffDetails().getCurrentLocation();
                try {
                    Location location = Location.builder()
                            .latitude(Double.parseDouble(loc.getLat()))
                            .longitude(Double.parseDouble(loc.getLng()))
                            .build();
                    logEntry.setLocation(location);
                } catch (NumberFormatException ignored) {
                    log.warn("Invalid location in Adloggs callback orderId={}", delivery.getOrderId());
                }
            }
            if (fulfillment.getRider() != null) {
                logEntry.setRider(fulfillment.getRider());
            }
            if (fulfillment.getLogs() == null) {
                fulfillment.setLogs(new ArrayList<>());
            }
            fulfillment.getLogs().add(logEntry);

            // Save rider platform as fulfillment channel
            if (payload.getRiderPlatform() != null) {
                DeliveryOrderStatus.Channel channel = new DeliveryOrderStatus.Channel();
                channel.setName(payload.getRiderPlatform().getName());
                channel.setId(payload.getRiderPlatform().getLspUniqId());
                fulfillment.setChannel(channel);
            }

            DeliveryOrderStatusType deliveryStatus = fulfillStatus.equals(DeliveryFulfillStatusType.DELIVERED)
                    ? DeliveryOrderStatusType.COMPLETED
                    : DeliveryOrderStatusType.FULFILLED;
            delivery.setStatus(deliveryStatus);
            delivery.setSwitchable(!isDeliveryTerminalStatus(fulfillStatus)
                    && fulfillStatus != DeliveryFulfillStatusType.PICKED_UP
                    && fulfillStatus != DeliveryFulfillStatusType.IN_TRANSIT
                    && fulfillStatus != DeliveryFulfillStatusType.OUT_FOR_DELIVERY);
            delivery.setFulfillment(fulfillment);

            DeliveryOrderData deliveryOrderData = new DeliveryOrderData();
            deliveryOrderData.setStatus(deliveryStatus.name());
            deliveryOrderData.setReferenceId(delivery.getOrderId());
            deliveryOrderData.setFulfillment(fulfillment);

            handleFulfillmentStatus(delivery, deliveryOrderData, order);

            recordFulfillmentEvent(delivery, order.getRestaurantId(), fulfillStatus);

            // Signal assignment workflow on first rider assignment
            if (fulfillStatus == DeliveryFulfillStatusType.OUT_FOR_PICKUP) {
                deliveryWorkflowService.signalRiderAssigned(delivery.getId());
            }

            save(delivery);
        } catch (Exception e) {
            handleDeliveryError("processAdloggsCallback", delivery, e);
        } finally {
            ObservabilityContext.clear();
        }
    }

    public void processDeliveryOrderFulfill(Delivery delivery, String triggeredBy, String fulfillType)
            throws DeliveryException {
        try {
            if (delivery == null) {
                log.error("Cannot process fulfillment: Delivery object is null.");
                return;
            }
            if (!DeliveryOrderStatusType.PENDING.equals(delivery.getStatus())) {
                log.error(
                        "Cannot process fulfillment: Delivery status is not PENDING for delivery ID {}",
                        delivery.getId());
                return;
            }
            // Adloggs assigns riders automatically on order creation — fulfillment is a no-op
            if (delivery.getProvider() == DeliveryPartner.ADLOGGS) {
                log.info("Adloggs delivery {} — skipping manual fulfillment (auto-assigned)", delivery.getId());
                return;
            }
            if ("smart".equalsIgnoreCase(fulfillType)) {
                log.info("Processing fulfillment type=smart");
                processDeliverySmartFulfill(delivery, triggeredBy);
            } else {
                log.info("Processing fulfillment type=standard");
                processDeliveryStandardFulfill(delivery, triggeredBy);
            }
        } catch (Exception e) {
            handleDeliveryError("processDeliveryOrderFulfill", delivery, e);
        }
    }

    public void processDeliveryStandardFulfill(Delivery delivery, String triggeredBy) throws DeliveryException {
        DeliveryNetworks selectedNetwork = getServiceabilityToken(delivery);
        if (selectedNetwork != null) {
            try {
                String token = selectedNetwork.getToken();
                delivery.setNetworkToken(token);
                delivery.setStatus(DeliveryOrderStatusType.FULFILLED);
                delivery.setNetworkId(selectedNetwork.getNetworkId());
                delivery.setService(selectedNetwork.getService());
                delivery.setPickupNow(selectedNetwork.isPickupNow());
                delivery.setFulfillmentType(triggeredBy);
                delivery.setFulfillmentAt(LocalDateTime.now());
                save(delivery);
                pidgeClient
                        .fulfillDeliveryOrder(deliveryRequestTranslation.getOrderFulfillRequest(delivery))
                        .subscribe();
                // Rider search starts now — begin assignment timeout watch
                deliveryWorkflowService.startAssignmentWatch(
                        delivery.getOrderId(),
                        delivery.getId(),
                        delivery.getProvider() != null ? delivery.getProvider().name() : null);
                metrics.count(
                        MetricsEvent.DELIVERY_FULFILLMENT,
                        MetricTag.ACTION,
                        "fulfillment",
                        MetricTag.RESULT,
                        "success",
                        MetricTag.TYPE,
                        "standard",
                        MetricTag.SERVICE,
                        selectedNetwork.getService());
                // Move from pending to in-transit
                metrics.decrementGauge("delivery_pending");
                metrics.incrementGauge("delivery_in_transit");
            } catch (Exception e) {
                metrics.count(
                        MetricsEvent.DELIVERY_FULFILLMENT,
                        MetricTag.ACTION,
                        "fulfillment",
                        MetricTag.RESULT,
                        "failed",
                        MetricTag.TYPE,
                        "standard");
                throw e;
            }
        } else {
            log.info(
                    "No matching network found with the specified networkId or minimum price network - Started smart fulfillment");
            processDeliverySmartFulfill(delivery, triggeredBy);
        }
    }

    public DeliveryNetworks getServiceabilityToken(Delivery delivery) throws DeliveryException {
        DeliveryNetworks selectedNetwork = null;
        try {
            DeliveryQuote deliveryQuote = this.getServiceability(delivery.getDeliveryOrderId());
            List<DeliveryNetworks> deliveryNetworks = deliveryQuote.getData().getItems().stream()
                    .filter(DeliveryNetworks::isPickupNow)
                    .filter(items -> !items.getService().equalsIgnoreCase("loadshare"))
                    .toList();

            Optional<DeliveryNetworks> matchingNetworkOpt = deliveryNetworks.stream()
                    .filter(network -> network.getNetworkId() == delivery.getNetworkId())
                    .findFirst();

            Optional<DeliveryNetworks> minPriceNetworkOpt = deliveryNetworks.stream()
                    .filter(network -> network.getQuote() != null)
                    .min(Comparator.comparingDouble(
                            network -> network.getQuote().getPrice()));

            if (matchingNetworkOpt.isPresent() && minPriceNetworkOpt.isPresent()) {
                DeliveryNetworks matchingNetwork = matchingNetworkOpt.get();
                DeliveryNetworks minPriceNetwork = minPriceNetworkOpt.get();
                selectedNetwork = (minPriceNetwork.getQuote().getPrice()
                                < matchingNetwork.getQuote().getPrice())
                        ? minPriceNetwork
                        : matchingNetwork;
            } else if (matchingNetworkOpt.isPresent()) {
                selectedNetwork = matchingNetworkOpt.get();
            } else if (minPriceNetworkOpt.isPresent()) {
                selectedNetwork = minPriceNetworkOpt.get();
            }

        } catch (Exception e) {
            handleDeliveryError("getServicabilityToken", delivery, e);
        }
        return selectedNetwork;
    }

    public void processDeliverySmartFulfill(Delivery delivery, String triggeredBy) throws DeliveryException {
        pidgeClient
                .smartFulfillDeliveryOrder(deliveryRequestTranslation.getSmartFulfillRequest(delivery))
                .subscribe();
        delivery.setStatus(DeliveryOrderStatusType.FULFILLED);
        delivery.setFulfillmentType(triggeredBy);
        delivery.setFulfillmentAt(LocalDateTime.now());
        save(delivery);
        // Rider search starts now — begin assignment timeout watch
        deliveryWorkflowService.startAssignmentWatch(
                delivery.getOrderId(),
                delivery.getId(),
                delivery.getProvider() != null ? delivery.getProvider().name() : null);
        metrics.count(
                MetricsEvent.DELIVERY_FULFILLMENT,
                MetricTag.ACTION,
                "fulfillment",
                MetricTag.RESULT,
                "success",
                MetricTag.TYPE,
                "smart");
        // Move from pending to in-transit
        metrics.decrementGauge("delivery_pending");
        metrics.incrementGauge("delivery_in_transit");
    }

    public void cancelDeliveryOrder(Delivery delivery) throws DeliveryException {
        if (delivery == null) {
            log.error("Cannot cancel delivery: Delivery object is null.");
            return;
        }
        if (delivery.getProvider() != null) {
            deliveryOrchestrator.getProvider(delivery.getProvider()).cancelOrder(delivery);
        } else {
            // legacy/manual delivery — no external API call needed
            log.info("Skipping external cancel for delivery without provider orderId={}", delivery.getOrderId());
        }
        DeliveryOrderStatusType previousStatus = delivery.getStatus();
        delivery.setDeleted(true);
        delivery.setStatus(DeliveryOrderStatusType.CANCELLED);
        save(delivery);

        Order cancelledOrder = orderService.findById(delivery.getOrderId());
        recordFulfillmentEvent(
                delivery,
                cancelledOrder != null ? cancelledOrder.getRestaurantId() : null,
                DeliveryFulfillStatusType.CANCELLED);

        metrics.count(MetricsEvent.DELIVERY_FULFILLMENT, MetricTag.ACTION, "cancel", MetricTag.RESULT, "success");
        // Decrement appropriate gauge based on previous status
        if (previousStatus == DeliveryOrderStatusType.PENDING) {
            metrics.decrementGauge("delivery_pending");
        } else if (previousStatus == DeliveryOrderStatusType.FULFILLED) {
            metrics.decrementGauge("delivery_in_transit");
        }
    }

    public DeliveryRiderLocation getDeliveryRiderLocation(String deliveryOrderId) throws DeliveryException {
        return pidgeClient.getDeliveryRiderLocation(deliveryOrderId);
    }

    public DeliveryOrderStatus getDeliveryOrderStatus(Delivery delivery) throws DeliveryException {
        if (delivery.getProvider() == null) {
            throw new DeliveryException("getOrderStatus", "No provider set on delivery " + delivery.getId());
        }
        return deliveryOrchestrator.getProvider(delivery.getProvider()).getOrderStatus(delivery);
    }

    public void unallocateDeliveryOrder(Delivery delivery) {
        if (delivery == null) {
            log.error("Cannot unallocate delivery order: Delivery not found");
            return;
        }
        pidgeClient.unallocateDeliveryOrder(delivery.getDeliveryOrderId()).subscribe();
        delivery.setStatus(DeliveryOrderStatusType.PENDING);
        save(delivery);
    }

    private void handleDeliveryError(String action, Delivery delivery, Exception e) throws DeliveryException {
        orderService.updateOrderStatus(delivery.getOrderId(), OrderStatusType.DELIVERY_ERROR);
        log.error("Exception occurred in {} : {}", action, e.getMessage(), e);
        throw new DeliveryException(action, "Exception occurred in Delivery Service : " + e.getMessage(), e);
    }

    public void setFulfillExpiry(String orderId, int expiry) {
        String fulfillRedisKey = "order:" + orderId + ":fulfill";
        redisService.setRedisData(
                fulfillRedisKey,
                OrderStatusType.ACCEPTED,
                Duration.ofMinutes(expiry).getSeconds());
        String deliveryRedisKey = "order:" + orderId + ":delivery";
        redisService.setRedisData(
                deliveryRedisKey,
                OrderStatusType.ACCEPTED,
                Duration.ofMinutes(expiry + 2).getSeconds());
    }

    public RiderLocation getRiderLocation(String deliveryOrderId) throws DeliveryException {
        return pidgeClient.getRiderLocation(deliveryOrderId);
    }

    public RiderLocation getPorterRiderLocation(Delivery delivery) throws DeliveryException {
        String porterId = delivery.getFulfillment().getChannel().getOrderId();
        return pidgeClient.getPorterRiderLocation(porterId);
    }

    /**
     * Maps a fulfillment status update to a DeliveryLifecycleEvent, records it in both
     * DeliveryLifecycleService (MongoDB history) and DeliveryPartnerEvaluator (Redis health).
     * No-op for statuses that don't map to a trackable event.
     */
    private void recordFulfillmentEvent(
            Delivery delivery, String restaurantId, DeliveryFulfillStatusType fulfillStatus) {
        if (delivery.getProvider() == null || restaurantId == null) return;

        DeliveryLifecycleEvent event =
                switch (fulfillStatus) {
                    case OUT_FOR_PICKUP -> DeliveryLifecycleEvent.RIDER_ASSIGNED;
                    case REACHED_PICKUP -> DeliveryLifecycleEvent.RIDER_REACHED_PICKUP;
                    case PICKED_UP -> DeliveryLifecycleEvent.RIDER_PICKED_UP;
                    case DELIVERED -> DeliveryLifecycleEvent.RIDER_DELIVERED;
                    case CANCELLED -> DeliveryLifecycleEvent.CANCELLED;
                    default -> null;
                };

        if (event == null) return;

        boolean success = event != com.hyp.enums.DeliveryLifecycleEvent.CANCELLED;

        // Calculate time since previous event for this delivery
        Long durationMs = scoreTracker.computeAndUpdateTiming(delivery.getId(), event);

        deliveryLifecycleService.record(
                delivery.getOrderId(),
                delivery.getId(),
                restaurantId,
                delivery.getProvider(),
                event,
                success,
                durationMs,
                null);
        partnerEvaluator.recordOutcome(
                delivery.getProvider(), restaurantId, delivery.getId(), event, success, durationMs);
    }

    private boolean isDeliveryTerminalStatus(DeliveryFulfillStatusType status) {
        return status == DeliveryFulfillStatusType.DELIVERED
                || status == DeliveryFulfillStatusType.CANCELLED
                || status == DeliveryFulfillStatusType.UNDELIVERED
                || status == DeliveryFulfillStatusType.RTO_DELIVERED
                || status == DeliveryFulfillStatusType.RTO_UNDELIVERED
                || status == DeliveryFulfillStatusType.LOST
                || status == DeliveryFulfillStatusType.DAMAGED;
    }
}
