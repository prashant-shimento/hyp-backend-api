package com.hyp.service;

import com.hyp.client.PidgeClient;
import com.hyp.constants.Constants;
import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.DeliveryQuoteRecord;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.entity.RiderRecord;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.DeliveryOrderStatusType;
import com.hyp.enums.DeliveryPartner;
import com.hyp.enums.OrderStatusType;
import com.hyp.event.OrderEventPublisher;
import com.hyp.exception.DeliveryException;
import com.hyp.model.DeliveryOrderStatus;
import com.hyp.model.DeliveryOrderStatus.DeliveryFulfillment;
import com.hyp.model.DeliveryOrderStatus.DeliveryOrderData;
import com.hyp.model.DeliveryOrderStatus.Rider;
import com.hyp.model.DeliveryQuote;
import com.hyp.model.DeliveryQuote.DeliveryNetworks;
import com.hyp.model.DeliveryRiderLocation;
import com.hyp.model.RiderLocation;
import com.hyp.observability.*;
import com.hyp.repository.DeliveryQuoteRepository;
import com.hyp.repository.DeliveryRepository;
import com.hyp.repository.RiderRecordRepository;
import com.hyp.request.DeliveryOrderRequest;
import com.hyp.request.DeliveryQuoteRequest;
import com.hyp.request.PosRiderUpdateRequest.RiderDetails;
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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
    PosService posService;

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
    ApplicationMetrics metrics;

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

    public void processDeliveryOrder(Order order) {
        ObservabilityContext.setOrderContext(order.getId(), order.getRestaurantId());
        try {
            CompletableFuture<Customer> customerFuture =
                    CompletableFuture.supplyAsync(() -> customerService.findById(order.getCustomerId()));
            CompletableFuture<Restaurant> restaurantFuture =
                    CompletableFuture.supplyAsync(() -> restaurantService.findById(order.getRestaurantId()));
            CompletableFuture<Address> addressFuture = CompletableFuture.supplyAsync(
                    () -> addressService.findById(order.getDeliveryDetails().getAddressId()));

            CompletableFuture.allOf(customerFuture, restaurantFuture, addressFuture)
                    .join();

            Customer customer = customerFuture.join();
            Restaurant restaurant = restaurantFuture.join();
            Address address = addressFuture.join();
            DeliveryOrderRequest deliveryOrderRequest =
                    DeliveryRequestTranslation.getDeliveryOrderRequest(restaurant, address, customer, order);
            log.info("Creating Delivery for OrderId {}", order.getId());
            createOrder(deliveryOrderRequest, order);
            metrics.count(MetricsEvent.DELIVERY, MetricTag.ACTION, "create", MetricTag.RESULT, "success");
            // Track pending deliveries gauge
            metrics.incrementGauge("delivery_pending");
        } catch (DeliveryException e) {
            log.error("Delivery creation failed", e);
            metrics.count(MetricsEvent.DELIVERY, MetricTag.ACTION, "create", MetricTag.RESULT, "failed");
        } finally {
            ObservabilityContext.clear();
        }
    }

    public void createOrder(DeliveryOrderRequest deliveryOrderRequest, Order order) throws DeliveryException {
        if (findByOrderId(order.getId()) != null) {
            throw new DeliveryException("Active delivery already exists for order: " + order.getId());
        }
        pidgeClient
                .createDeliveryOrder(deliveryOrderRequest)
                .flatMap(deliveryOrderId -> {
                    Delivery delivery = DeliveryRequestTranslation.getDeliveryEntity(deliveryOrderRequest);
                    delivery.setId(CommonUtils.genId());
                    delivery.setDeliveryOrderId(deliveryOrderId);
                    delivery.setStatus(DeliveryOrderStatusType.PENDING);
                    delivery.setService(order.getDeliveryDetails().getService());
                    delivery.setNetworkId(order.getDeliveryDetails().getNetworkId());
                    delivery.setPickupNow(order.getDeliveryDetails().isPickupNow());
                    return Mono.fromRunnable(() -> save(delivery));
                })
                .block(Duration.ofSeconds(10));
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
                DeliveryRequestTranslation.getDeliveryOrderRequest(restaurant, address, customer, order);

        Delivery delivery = DeliveryRequestTranslation.getDeliveryEntity(deliveryOrderRequest);
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

            if (posService.isPosUpdateRequired(fulfillStatus)) {
                posService.updatePosRiderStatus(delivery, order);
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
                orderEventPublisher.publishDeliveryEvent(delivery);
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
            order.setDeliveryTrackingLink("https://api.hyperapps.in/api/v2/order/track/" + order.getId());
        }
        if (fullFillStatus.equals(DeliveryFulfillStatusType.DELIVERED)) {
            order.setFulfilledBy(DeliveryPartner.PIDGE);
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
                        if (posService.isPosUpdateRequired(fullFillStatus)) {
                            posService.updatePosRiderStatus(delivery, order);
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

    public void processDeliveryOrderFulfill(Delivery delivery, String fulfilledBy, String fulfillType)
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
            if ("smart".equalsIgnoreCase(fulfillType)) {
                log.info("Processing fulfillment type=smart");
                processDeliverySmartFulfill(delivery, fulfilledBy);
            } else {
                log.info("Processing fulfillment type=standard");
                processDeliveryStandardFulfill(delivery, fulfilledBy);
            }
        } catch (Exception e) {
            handleDeliveryError("processDeliveryOrderFulfill", delivery, e);
        }
    }

    public void processDeliveryStandardFulfill(Delivery delivery, String fulfilledBy) throws DeliveryException {
        DeliveryNetworks selectedNetwork = getServiceabilityToken(delivery);
        if (selectedNetwork != null) {
            try {
                String token = selectedNetwork.getToken();
                delivery.setNetworkToken(token);
                delivery.setStatus(DeliveryOrderStatusType.FULFILLED);
                delivery.setNetworkId(selectedNetwork.getNetworkId());
                delivery.setService(selectedNetwork.getService());
                delivery.setPickupNow(selectedNetwork.isPickupNow());
                delivery.setFulfillmentType(fulfilledBy);
                delivery.setFulfillmentAt(LocalDateTime.now());
                save(delivery);
                pidgeClient
                        .fulfillDeliveryOrder(DeliveryRequestTranslation.getOrderFulfillRequest(delivery))
                        .subscribe();
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
            processDeliverySmartFulfill(delivery, fulfilledBy);
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

    public void processDeliverySmartFulfill(Delivery delivery, String fulfilledBy) throws DeliveryException {
        pidgeClient
                .smartFulfillDeliveryOrder(DeliveryRequestTranslation.getSmartFulfillRequest(delivery))
                .subscribe();
        delivery.setStatus(DeliveryOrderStatusType.FULFILLED);
        delivery.setFulfillmentType(fulfilledBy);
        delivery.setFulfillmentAt(LocalDateTime.now());
        save(delivery);
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
            log.error("Cannot cancel delivery order: Delivery object is null.");
            return;
        }
        if (!DeliveryPartner.MANUAL.name().equalsIgnoreCase(delivery.getChannel())) {
            pidgeClient.cancelDeliveryOrder(delivery.getDeliveryOrderId()).subscribe();
        }
        DeliveryOrderStatusType previousStatus = delivery.getStatus();
        delivery.setDeleted(true);
        delivery.setStatus(DeliveryOrderStatusType.CANCELLED);
        save(delivery);
        metrics.count(MetricsEvent.DELIVERY_FULFILLMENT, MetricTag.ACTION, "cancel", MetricTag.RESULT, "success");
        if (previousStatus == DeliveryOrderStatusType.PENDING) {
            metrics.decrementGauge("delivery_pending");
        } else if (previousStatus == DeliveryOrderStatusType.FULFILLED) {
            metrics.decrementGauge("delivery_in_transit");
        }
    }

    public DeliveryRiderLocation getDeliveryRiderLocation(String deliveryOrderId) throws DeliveryException {
        return pidgeClient.getDeliveryRiderLocation(deliveryOrderId);
    }

    public DeliveryOrderStatus getDeliveryOrderStatus(String deliveryOrderId) throws DeliveryException {
        return pidgeClient.getDeliveryOrderStatus(deliveryOrderId);
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
