package com.hyp.service;

import com.hyp.client.PidgeClient;
import com.hyp.constants.Constants;
import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.entity.RiderRecord;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.DeliveryOrderStatusType;
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
        return deliveryRepository.findByOrderIdAndIsDeletedFalse(orderId);
    }

    public Delivery findByDeliveryOrderId(String deliveryOrderId) {
        return deliveryRepository.findByDeliveryOrderIdAndIsDeletedFalse(deliveryOrderId);
    }

    public void processDeliveryOrder(Order order) {
        ObservabilityContext.setOrderContext(order.getId(), order.getRestaurantId());
        try {
            Customer customer = customerService.findById(order.getCustomerId());
            Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
            Address address = addressService.findById(order.getDeliveryDetails().getAddressId());
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
                .block();
    }

    public DeliveryQuote getDeliveryQuote(DeliveryQuoteRequest deliveryQuoteRequest) throws DeliveryException {
        return pidgeClient.getDeliveryQuote(deliveryQuoteRequest);
    }

    public DeliveryQuote getServiceability(String deliveryOrderId) throws DeliveryException {
        return pidgeClient.getServiceability(deliveryOrderId);
    }

    public void processDeliveryCallback(Delivery delivery, DeliveryOrderData deliveryOrderData)
            throws DeliveryException {
        ObservabilityContext.setOrderId(delivery.getOrderId());
        log.info(
                "Delivery callback received orderId={} deliveryStatus={}",
                delivery.getOrderId(),
                deliveryOrderData.getStatus());
        try {
            DeliveryOrderStatusType status =
                    DeliveryOrderStatusType.getDeliveryOrderStatus(deliveryOrderData.getStatus());
            delivery.setStatus(status);

            Order order = orderService.findById(delivery.getOrderId());

            if (delivery.getStatus() == DeliveryOrderStatusType.PENDING) {
                DeliveryFulfillment fulfillment = deliveryOrderData.getFulfillment();
                DeliveryFulfillStatusType fulfillmentStatus = fulfillment.getStatus();

                log.info(
                        "Fulfillment update orderStatus={} fulfillmentStatus={}", order.getStatus(), fulfillmentStatus);

                if (fulfillmentStatus == DeliveryFulfillStatusType.CANCELLED) {
                    log.info("Rider cancelled delivery");
                    orderService.updateOrderStatus(
                            order.getId(),
                            OrderStatusType.getOrderStatusByDeliveryStatus(DeliveryFulfillStatusType.CANCELLED));
                    orderEventPublisher.publishDeliveryEvent(delivery);
                }
            }
            if (delivery.getStatus() == DeliveryOrderStatusType.CANCELLED) {
                orderService.updateOrderStatus(order.getId(), OrderStatusType.DELIVERY_CANCELLED);
                delivery.setDeleted(true);
            }
            if (delivery.getStatus() == DeliveryOrderStatusType.FULFILLED
                    || delivery.getStatus() == DeliveryOrderStatusType.COMPLETED) {
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

        delivery.setNetworkId(Integer.parseInt(deliveryFulfill.getChannel().getId()));
        delivery.setService(deliveryFulfill.getChannel().getName());
        delivery.setPickupNow(true);
        delivery.setFulfillment(deliveryFulfill);

        if (deliveryOrderData.getFulfillment().getTrackCode() != null) {
            delivery.getFulfillment()
                    .setTrackCode(deliveryOrderData.getFulfillment().getTrackCode());
        }
        // TODO: Need to Move this saveOrder some where - unwanted save operation
        if (order.getDeliveryTrackingLink() == null) {
            order.setDeliveryTrackingLink("https://api.hyperapps.in/api/v2/order/track/" + order.getId());
        }
        orderService.save(order);
        orderService.updateOrderStatus(order.getId(), OrderStatusType.getOrderStatusByDeliveryStatus(fullFillStatus));

        // Decrement in-transit gauge when delivery reaches terminal state
        if (isDeliveryTerminalStatus(fullFillStatus)) {
            metrics.decrementGauge("delivery_in_transit");
        }

        if (posService.isPosUpdateRequired(fullFillStatus)) {
            posService.updatePosRiderStatus(delivery, order);
        }
        try {
            if (DeliveryFulfillStatusType.OUT_FOR_PICKUP.equals(fullFillStatus)) {
                if (delivery.getFulfillment() == null) {
                    log.debug("Delivery or fulfillment is null; skipping rider fraud check.");
                } else {
                    Rider rider = delivery.getFulfillment().getRider();
                    if (rider == null) {
                        log.debug("No rider found in fulfillment; skipping rider fraud check.");
                    } else {
                        RiderDetails riderDetails = new RiderDetails(rider.getName(), rider.getMobile());
                        checkAndAlertFraudRider(riderDetails, delivery);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed while checking/alerting fraud rider", e);
        }
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
        try {
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
        } catch (DeliveryException e) {
            metrics.count(
                    MetricsEvent.DELIVERY_FULFILLMENT,
                    MetricTag.ACTION,
                    "fulfillment",
                    MetricTag.RESULT,
                    "failed",
                    MetricTag.TYPE,
                    "smart");
            handleDeliveryError("processDeliverySmartFulfill", delivery, e);
        }
    }

    public void cancelDeliveryOrder(String deliveryOrderId) throws DeliveryException {
        pidgeClient.cancelDeliveryOrder(deliveryOrderId).subscribe();
        Delivery delivery = findByDeliveryOrderId(deliveryOrderId);
        DeliveryOrderStatusType previousStatus = delivery.getStatus();
        delivery.setDeleted(true);
        delivery.setStatus(DeliveryOrderStatusType.CANCELLED);
        save(delivery);
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

    public DeliveryOrderStatus getDeliveryOrderStatus(String deliveryOrderId) throws DeliveryException {
        return pidgeClient.getDeliveryOrderStatus(deliveryOrderId);
    }

    public void unallocateDeliveryOrder(String deliveryOrderId) throws DeliveryException {
        pidgeClient.unallocateDeliveryOrder(deliveryOrderId).subscribe();
        Delivery delivery = findByDeliveryOrderId(deliveryOrderId);
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

    public RiderLocation getPorterRiderLocation(String deliveryOrderId) throws DeliveryException {
        Delivery delivery = findByDeliveryOrderId(deliveryOrderId);
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
