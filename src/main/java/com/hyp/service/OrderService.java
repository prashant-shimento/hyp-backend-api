package com.hyp.service;

import static com.hyp.util.CommonUtils.roundToTwoDecimal;
import static com.hyp.util.ValidationUtils.isWithinDeliveryHours;

import com.hyp.constants.Constants;
import com.hyp.dto.OrderDto;
import com.hyp.dto.OrderDto.OrderAddonItem;
import com.hyp.dto.OrderDto.OrderItem;
import com.hyp.entity.*;
import com.hyp.enums.*;
import com.hyp.enums.OrderType;
import com.hyp.event.OrderEventPublisher;
import com.hyp.exception.*;
import com.hyp.model.PreOrder;
import com.hyp.observability.ApplicationMetrics;
import com.hyp.observability.MetricTag;
import com.hyp.observability.MetricsEvent;
import com.hyp.observability.ObservabilityContext;
import com.hyp.repository.OrderRepository;
import com.hyp.request.PosCallbackRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.temporal.service.OrderWorkflowService;
import com.hyp.translation.OrderTranslation;
import com.hyp.translation.PosOrderRequestTranslation;
import com.hyp.util.CommonUtils;
import com.mongodb.client.result.UpdateResult;
import io.micrometer.core.instrument.Timer;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class OrderService extends BaseServiceImpl<Order, String> {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    RestaurantService restaurantService;

    @Autowired
    CustomerService customerService;

    @Autowired
    TaxService taxService;

    @Autowired
    DiscountService discountService;

    @Autowired
    ItemService itemService;

    @Autowired
    AddonItemService addonItemService;

    @Autowired
    VariationService variationService;

    @Autowired
    OrderTranslation orderTranslation;

    @Autowired
    AddressService addressService;

    @Autowired
    DeliveryService deliveryService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    LocationService locationService;

    @Autowired
    PartnerService partnerService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    RedisService redisService;

    @Autowired
    OrderWorkflowService orderWorkflowService;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private OneSignalAlertService oneSignalAlertService;

    @Autowired
    PosOrderRequestTranslation posOrderRequestTranslation;

    @Autowired
    PosService posService;

    @Autowired
    ApplicationMetrics metrics;

    @Autowired
    ReferralTokenService referralTokenService;

    @Autowired
    OfferService offerService;

    @Autowired
    OfferUsageService offerUsageService;

    public Order create(OrderDto orderDto) throws Exception {
        Timer.Sample timerSample = metrics.startTimer();

        Restaurant restaurant = Optional.ofNullable(restaurantService.findById(orderDto.getRestaurantId()))
                .orElseThrow(() ->
                        new EntityNotFoundException(Restaurant.class.getSimpleName(), orderDto.getRestaurantId()));

        Partner partner;
        if (orderDto.getPartnerId() != null) {
            partner = Optional.ofNullable(partnerService.findById(orderDto.getPartnerId()))
                    .orElseThrow(
                            () -> new EntityNotFoundException(Partner.class.getSimpleName(), orderDto.getPartnerId()));
        } else {
            partner = partnerService.findPartnersByRestaurantId(orderDto.getRestaurantId(), PartnerType.RESTAURANT);
            orderDto.setPartnerId(partner.getId());
        }

        if (!restaurant.isServiceable()) throw new ValidationException("Restaurant is not serviceable");

        if (!restaurant.isActive()) throw new ValidationException("Restaurant is not active");

        if (!isWithinDeliveryHours(restaurant.getDeliveryHours()))
            throw new ValidationException("Order cannot be processed: Outside delivery hours");

        Customer customer = Optional.ofNullable(customerService.findById(orderDto.getCustomerId()))
                .orElseThrow(
                        () -> new EntityNotFoundException(Customer.class.getSimpleName(), orderDto.getCustomerId()));

        boolean isPreOrder = Boolean.TRUE.equals(orderDto.getPreOrder());
        PreOrder preOrderConfig = getPreOrder(orderDto, restaurant, isPreOrder);
        if (isPreOrder) {
            validatePreOrderTime(orderDto.getPreOrderDateTime(), preOrderConfig, restaurant.getDeliveryHours());
        }

        Address address = null;
        if (OrderType.fromCode(orderDto.getOrderType()) == OrderType.H) {
            if (orderDto.getDeliveryDetails() != null) {
                address = Optional.ofNullable(addressService.findById(
                                orderDto.getDeliveryDetails().getAddressId()))
                        .orElseThrow(() -> new EntityNotFoundException(
                                Address.class.getSimpleName(),
                                orderDto.getDeliveryDetails().getAddressId()));

                if (!locationService.isLocationDeliverable(
                        address.getLocation().getLatitude(),
                        address.getLocation().getLongitude(),
                        restaurant.getLocation().getLatitude(),
                        restaurant.getLocation().getLongitude(),
                        restaurant.getDeliveryRadius())) {
                    throw new ValidationException("Location not deliverable");
                }
            } else {
                if (orderDto.getSeat() == null || orderDto.getScreen() == null) {
                    throw new ValidationException("Delivery details missing: Seat and Screen required");
                }
            }
        }

        List<String> itemIds = new ArrayList<>();
        List<String> variationIds = new ArrayList<>();
        List<String> addonIds = new ArrayList<>();
        List<String> taxIds = new ArrayList<>();

        for (OrderItem oi : orderDto.getOrderItems()) {
            if (oi.getVariationId() != null) variationIds.add(oi.getId());
            else itemIds.add(oi.getId());

            if (oi.getOrderAddonItems() != null) {
                oi.getOrderAddonItems().forEach(a -> addonIds.add(a.getAddonItemId()));
            }

            if (oi.getOrderItemTax() != null) {
                oi.getOrderItemTax().forEach(t -> taxIds.add(t.getId()));
            }
        }

        if (orderDto.getOrderTax() != null) {
            orderDto.getOrderTax().forEach(t -> taxIds.add(t.getId()));
        }

        Map<String, Item> itemMap =
                itemService.findAllByIdIn(itemIds).stream().collect(Collectors.toMap(Item::getId, i -> i));

        Map<String, Variation> variationMap = variationService.findAllByIdIn(variationIds).stream()
                .collect(Collectors.toMap(Variation::getId, v -> v));

        Map<String, AddonItem> addonMap =
                addonItemService.findAllByIdIn(addonIds).stream().collect(Collectors.toMap(AddonItem::getId, a -> a));

        Map<String, Tax> taxMap =
                taxService.findAllByIdIn(taxIds).stream().collect(Collectors.toMap(Tax::getId, t -> t));

        double itemTotalAmount = 0.0;
        double taxTotalAmount = 0.0;

        for (OrderItem oi : orderDto.getOrderItems()) {

            boolean isVariation = oi.getVariationId() != null;
            double basePrice;

            if (isVariation) {
                Variation v = variationMap.get(oi.getId());
                if (v == null) throw new ValidationException("Variation not found: " + oi.getId());
                basePrice = roundToTwoDecimal(Double.parseDouble(v.getPrice()));
            } else {
                Item item = itemMap.get(oi.getId());
                if (item == null) throw new ValidationException("Item not found: " + oi.getId());
                basePrice = roundToTwoDecimal(Double.parseDouble(item.getPrice()));
                oi.setItemAttribute(item.getItemAttributeId());
            }

            // Item price tampering check
            if (roundToTwoDecimal(oi.getPrice()) != basePrice) {
                log.warn("Invalid item price: {} restaurantId: {}", oi.getId(), orderDto.getRestaurantId());
                //                throw new ValidationException("Invalid item price: " + oi.getId());
            }

            double lineTotal = roundToTwoDecimal(basePrice * oi.getQuantity());

            // Addons
            if (oi.getOrderAddonItems() != null) {
                for (OrderAddonItem addon : oi.getOrderAddonItems()) {
                    AddonItem dbAddon = addonMap.get(addon.getAddonItemId());
                    if (dbAddon == null) throw new ValidationException("Addon not found: " + addon.getAddonItemId());

                    double addonPrice = roundToTwoDecimal(Double.parseDouble(dbAddon.getAddonItemPrice()));

                    if (roundToTwoDecimal(addon.getPrice()) != addonPrice) {
                        log.warn(
                                "Invalid addon price: {} restaurantId: {}",
                                addon.getAddonItemId(),
                                orderDto.getRestaurantId());
                        //                        throw new ValidationException("Invalid addon price: " +
                        // addon.getAddonItemId());
                    }

                    lineTotal = roundToTwoDecimal(lineTotal + (addonPrice * addon.getQuantity()));
                }
            }

            // ITEM LEVEL TAX
            if (oi.getOrderItemTax() != null) {
                for (OrderDto.OrderItemTax it : oi.getOrderItemTax()) {
                    Tax tax = taxMap.get(it.getId());
                    if (tax == null) throw new ValidationException("Tax not found: " + it.getId());

                    double taxPercent = Double.parseDouble(tax.getTax());

                    double expectedTax = roundToTwoDecimal((lineTotal * taxPercent) / 100);

                    if (roundToTwoDecimal(it.getAmount()) != expectedTax) {
                        log.warn(
                                "Invalid item tax amount for: {} restaurantId: {}",
                                tax.getTaxName(),
                                orderDto.getRestaurantId());
                        //                        throw new ValidationException("Invalid item tax amount for " +
                        // tax.getTaxName());
                    }

                    taxTotalAmount = roundToTwoDecimal(taxTotalAmount + expectedTax);
                }
            }

            itemTotalAmount = roundToTwoDecimal(itemTotalAmount + lineTotal);
        }

        if (roundToTwoDecimal(orderDto.getTaxAmount()) != taxTotalAmount) {
            log.warn(
                    "Invalid Order Tax amount: Request {}, Actual {} restaurantId: {}",
                    roundToTwoDecimal(orderDto.getTaxAmount()),
                    taxTotalAmount,
                    orderDto.getRestaurantId());
            // throw new ValidationException("Invalid Order Tax amount");
        }

        double grandTotalAmount =
                roundToTwoDecimal(itemTotalAmount + taxTotalAmount + roundToTwoDecimal(orderDto.getDeliveryCharge()));

        if (roundToTwoDecimal(orderDto.getGrandTotalAmount()) != grandTotalAmount) {
            log.warn(
                    "Invalid grand total amount: Request {}, Actual {} restaurantId: {}",
                    roundToTwoDecimal(orderDto.getGrandTotalAmount()),
                    grandTotalAmount,
                    orderDto.getRestaurantId());
            //            throw new ValidationException("Invalid grand total amount");
        }

        double appliedOfferAmount = validateAndApplyOffer(
                orderDto,
                itemTotalAmount,
                orderDto.getCustomerId(),
                orderDto.getPartnerId(),
                orderDto.getRestaurantId());

        double finalItemTotal = Math.max(0, itemTotalAmount - appliedOfferAmount);

        Order order = orderTranslation.getEntity(orderDto);
        order.setStatus(OrderStatusType.CREATED);
        order.setOrderTime(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setItemTotalAmount(finalItemTotal);

        ReferralToken referralToken = referralTokenService.validateTokenForOrder(orderDto.getReferralToken());
        if (referralToken != null) {
            order.setReferralCode(referralToken.getReferralCode());
            order.setReferralTokenId(referralToken.getId());
        }

        order = save(order);

        metrics.count(
                MetricsEvent.ORDER,
                MetricTag.ACTION,
                "create",
                MetricTag.STATUS,
                order.getStatus().name(),
                MetricTag.RESULT,
                "success",
                MetricTag.ORDER_TYPE,
                order.getOrderType());

        // Track active orders gauge
        metrics.incrementGauge("active_orders", MetricTag.ORDER_TYPE, order.getOrderType());

        metrics.stopTimer(timerSample, MetricsEvent.ORDER, MetricTag.ACTION, "create");
        // Set observability context for downstream logging
        ObservabilityContext.setOrderContext(order.getId(), order.getRestaurantId());

        try {
            log.info(
                    "Order created orderId={} type={} paymentType={} amount={}",
                    order.getId(),
                    order.getOrderType(),
                    order.getPaymentType(),
                    order.getGrandTotalAmount());
            if (order.getPaymentType() == PaymentType.COD) {
                orderEventPublisher.publishPosOrderEvent(order);
                orderEventPublisher.publishOrderStatusChangeEvent(order);
            }

            List<String> parameters = CommonUtils.buildStringList(
                    customer.getName(),
                    customer.getMobile(),
                    order.getId(),
                    order.getStatus(),
                    restaurant.getRestaurantName());
            notificationService.sendInternalGroupNotification(Constants.META_ORDER_ALERT_TEMPLATE, parameters);
            oneSignalAlertService.notifyNewOrder(
                    customer.getName(),
                    customer.getMobile(),
                    order.getId(),
                    order.getStatus(),
                    restaurant.getRestaurantName());
            return order;
        } finally {
            ObservabilityContext.clear();
        }
    }

    private double validateAndApplyOffer(
            OrderDto orderDto, double itemTotalAmount, String customerId, String partnerId, String restaurantId)
            throws Exception {

        double appliedOfferAmount = 0d;

        if (orderDto.getOfferCode() == null || orderDto.getOfferCode().isBlank()) {
            return 0d;
        }
        long createdOrderCount = orderRepository.countByCustomerIdAndStatus(customerId, "PAID");

        Offer offer = offerService.findByOfferCode(orderDto.getOfferCode());

        if (!Boolean.TRUE.equals(offer.getIsActive())) {
            throw new Exception("Offer is not active: " + orderDto.getOfferCode());
        }

        ZoneId zone = ZoneId.of("Asia/Kolkata");
        ZonedDateTime now = ZonedDateTime.now(zone);

        if (offer.getStartDate() != null) {
            ZonedDateTime start = offer.getStartDate().atZone(zone);
            if (now.isBefore(start)) {
                throw new Exception("Offer not started yet");
            }
        }

        if (offer.getEndDate() != null) {
            ZonedDateTime end = offer.getEndDate().atZone(zone);
            if (now.isAfter(end)) {
                throw new Exception("Offer expired");
            }
        }

        if (offer.getPartnerId() != null && !offer.getPartnerId().equals(partnerId)) {
            throw new Exception("Offer not valid for this partner");
        }

        if (offer.getRestaurantId() != null && !offer.getRestaurantId().equals(restaurantId)) {
            throw new Exception("Offer not valid for this restaurant");
        }

        // Get current usage count
        Integer usedCount = offerUsageService.getUsageCount(orderDto.getOfferCode(), customerId);

        int maximumRedemptionLimit = 0;
        if (offer.getMaximumRedemptionLimit() != null) {
            maximumRedemptionLimit = Integer.parseInt(offer.getMaximumRedemptionLimit());
        }
        if (usedCount >= maximumRedemptionLimit || createdOrderCount >= maximumRedemptionLimit) {
            throw new Exception("Offer usage limit exceeded. Maximum 3 attempts allowed.");
        }

        // Discount Calculation
        double discountValue = offer.getDiscountValue();

        if ("PERCENTAGE".equalsIgnoreCase(offer.getOfferType().toString())) {
            appliedOfferAmount = (itemTotalAmount * discountValue) / 100d;
        } else {
            appliedOfferAmount = discountValue;
        }

        if (appliedOfferAmount > itemTotalAmount) {
            appliedOfferAmount = itemTotalAmount;
        }

        // Save or update after validation passes
        saveOfferUsage(orderDto.getOfferCode(), customerId, partnerId, usedCount + 1);

        return appliedOfferAmount;
    }

    private void saveOfferUsage(String offerCode, String customerId, String partnerId, Integer usedCount) {
        OfferUsage existingUsage = offerUsageService.getCustomerIdAndOfferCode(customerId, offerCode);

        if (existingUsage != null) {
            existingUsage.setUsageCount(usedCount);
            offerUsageService.save(existingUsage);
        } else {
            OfferUsage offerUsage = new OfferUsage();
            offerUsage.setOfferCode(offerCode);
            offerUsage.setCustomerId(customerId);
            offerUsage.setPartnerId(partnerId);
            offerUsage.setUsageCount(usedCount);
            offerUsageService.save(offerUsage);
        }
    }

    private static PreOrder getPreOrder(OrderDto orderDto, Restaurant restaurant, boolean isPreOrder)
            throws ValidationException {
        PreOrder preOrderConfig = restaurant.getPreOrderConfig();

        if (isPreOrder) {
            if (orderDto.getPreOrderDateTime() == null) {
                throw new ValidationException("Pre-order date time is mandatory.");
            }
            if (preOrderConfig == null) {
                throw new ValidationException("Pre-order is not configured for the restaurant.");
            }
            if (!preOrderConfig.isPreOrderEnabled()) {
                throw new ValidationException("Pre-order is not enabled for the restaurant.");
            }
        }
        return preOrderConfig;
    }

    public void processOrderCallback(PosCallbackRequest posCallbackRequest) {
        String orderId = posCallbackRequest.getOrderId();
        String restaurantId = posCallbackRequest.getRestaurantId();
        ObservabilityContext.setOrderContext(orderId, restaurantId);
        log.info("POS callback received orderId={} posStatus={}", orderId, posCallbackRequest.getStatus());
        try {
            Restaurant restaurant = restaurantService.findByMenuSharingCode(restaurantId);
            if (restaurant == null) {
                throw new Exception("Restaurant not found " + posCallbackRequest.getRestaurantId());
            }
            if (!this.isExistsById(orderId)) {
                throw new Exception("Order not found " + orderId);
            }
            Order order = this.findById(orderId);

            if (order.getPaymentType() != PaymentType.COD) {
                if (paymentService.findByOrderId(orderId) == null) {
                    throw new Exception("Payment not completed" + orderId);
                }
            }

            OrderStatusType newOrderStatus = OrderStatusType.getOrderStatusByPosStatus(posCallbackRequest.getStatus());
            updateOrderStatus(orderId, newOrderStatus);

            order = this.findById(orderId);

            if (newOrderStatus == OrderStatusType.ACCEPTED && !order.isPreOrder()) {
                order.setMinDeliveryTime(posCallbackRequest.getMinDeliveryTime());
                order.setMinPrepTime(posCallbackRequest.getMinPrepTime());
                order = update(order);

                int fulfillmentDelay =
                        Optional.ofNullable(restaurant.getFulfillmentDelay()).orElse(2);

                if (fulfillmentDelay > 0) {
                    boolean isWorkflowEnabled = redisService
                            .getRedisData(Constants.FULFILLMENT_WORKFLOW_ENABLED)
                            .map(Boolean::parseBoolean)
                            .orElse(false);
                    log.info(
                            "Scheduling fulfillment delayMinutes={} workflowEnabled={}",
                            fulfillmentDelay,
                            isWorkflowEnabled);

                    if (isWorkflowEnabled) {
                        startOrderFulfillmentWorkflow(order.getId(), fulfillmentDelay);

                    } else {
                        deliveryService.setFulfillExpiry(order.getId(), fulfillmentDelay);
                    }
                    return;
                }

                Delivery delivery = deliveryService.findByOrderId(order.getId());

                if (delivery == null || !DeliveryOrderStatusType.PENDING.equals(delivery.getStatus())) {
                    log.warn("Skipping fulfillment - no pending delivery found");
                    return;
                }

                String fulfillmentMode =
                        redisService.getRedisData(Constants.KEY_FULFILL).orElse(Constants.KEY_SMART);
                deliveryService.processDeliveryOrderFulfill(delivery, Constants.PET_POOJA, fulfillmentMode);
                return;
            }

            if (newOrderStatus == OrderStatusType.CANCELLED) {
                paymentService.createRefund(
                        order.getId(),
                        order.getGrandTotalAmount(),
                        restaurant.isInstantRefund(),
                        "Cancelled by Restaurant");
                Delivery delivery = deliveryService.findByOrderId(order.getId());
                if (delivery != null
                        && (delivery.getStatus().equals(DeliveryOrderStatusType.PENDING)
                                || delivery.getStatus().equals(DeliveryOrderStatusType.FULFILLED))) {
                    deliveryService.cancelDeliveryOrder(delivery.getDeliveryOrderId());
                }
            }
        } catch (DeliveryException e) {
            throw new RuntimeException("Exception Occurred while createOrder in Delivery Service " + e.getMessage());
        } catch (Exception e) {
            updateOrderStatus(posCallbackRequest.getOrderId(), OrderStatusType.ERROR);
            throw new RuntimeException("Exception Occurred while processCallback Order " + e.getMessage());
        } finally {
            ObservabilityContext.clear();
        }
    }

    @Transactional
    public Order updateOrderStatus(String orderId, OrderStatusType newStatus) {

        Order order = findById(orderId);
        OrderStatusType oldStatus = order.getStatus();
        order.setStatus(newStatus);
        order.getOrderLogs().add(new Order.OrderLog(newStatus.name()));

        save(order);

        consumeReferralTokenIfPaid(order, newStatus);

        if (order.getStatus() == OrderStatusType.PAID || order.getStatus() == OrderStatusType.CANCELLED) {
            orderWorkflowService.signalPaymentStatusChanged(
                    order.getId(), order.getStatus().name());
        }
        metrics.count(
                MetricsEvent.ORDER,
                MetricTag.ACTION,
                "status_update",
                MetricTag.RESULT,
                "success",
                MetricTag.STATUS,
                newStatus.name().toLowerCase());

        if (newStatus == OrderStatusType.ERROR || newStatus == OrderStatusType.PAYMENT_FAILED) {
            metrics.count(
                    MetricsEvent.ORDER,
                    MetricTag.ACTION,
                    "status_update",
                    MetricTag.RESULT,
                    "failed",
                    MetricTag.STATUS,
                    newStatus.name().toLowerCase());
        }

        // Decrement active orders gauge when order reaches terminal state
        if (isTerminalStatus(newStatus)) {
            metrics.decrementGauge("active_orders", MetricTag.ORDER_TYPE, order.getOrderType());
        }

        try {
            orderEventPublisher.publishOrderStatusChangeEvent(order);

            if (OrderStatusType.DELIVERED.equals(newStatus)) {
                orderEventPublisher.publishSettlementEvent(order);
            }
        } catch (Exception e) {
            log.error("Failed to publish event status={}", newStatus, e);
        }

        log.info("Status changed from={} to={}", oldStatus, newStatus);
        return order;
    }

    public void startOrderFulfillmentWorkflow(String orderId, int fulfillmentDelay) {
        orderWorkflowService.startOrderFulfillmentWorkflow(orderId, fulfillmentDelay);
    }

    public boolean updateStatus(String orderId, OrderStatusType newStatus) {
        List<OrderStatusType> allowedStatuses = Arrays.asList(
                OrderStatusType.PAYMENT_PENDING,
                OrderStatusType.PAYMENT_FAILED,
                OrderStatusType.ERROR,
                OrderStatusType.PROCESSING);

        Query query = new Query(Criteria.where("_id").is(orderId).and("status").in(allowedStatuses));
        Update update = new Update().set("status", newStatus).push("orderLogs", new Order.OrderLog(newStatus.name()));

        UpdateResult result = mongoTemplate.updateFirst(query, update, Order.class);

        if (result.getModifiedCount() > 0) {
            log.info("Status updated atomically to={}", newStatus);
            Order updatedOrder = this.findById(orderId);

            consumeReferralTokenIfPaid(updatedOrder, newStatus);

            orderWorkflowService.signalPaymentStatusChanged(
                    updatedOrder.getId(), updatedOrder.getStatus().name());
            orderEventPublisher.publishOrderStatusChangeEvent(updatedOrder);
            return true;
        } else {
            log.debug("Status update skipped - already processed or in final state");
            return false;
        }
    }

    List<Order> findByRestaurantIdAndCreatedAt(
            String restaurantId, OrderStatusType statusType, LocalDateTime from, LocalDateTime to) {
        return orderRepository.findByRestaurantIdAndStatusInAndCreatedAtBetween(restaurantId, statusType, from, to);
    }

    public void validatePreOrderTime(
            LocalDateTime preOrderDateTime, PreOrder preOrderConfig, List<Restaurant.DeliveryHours> deliveryHours)
            throws ValidationException {

        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        ZoneId utcZone = ZoneOffset.UTC;

        ZonedDateTime preOrderIST = preOrderDateTime.atZone(istZone);
        ZonedDateTime preOrderUTC = preOrderIST.withZoneSameInstant(utcZone);

        ZonedDateTime nowUTC = ZonedDateTime.now(utcZone);

        if (!preOrderUTC.isAfter(nowUTC)) {
            throw new ValidationException("Preorder time must be in the future.");
        }

        String unit = Optional.ofNullable(preOrderConfig.getTimeUnit()).orElse("minutes");

        Duration minLead = unit.equalsIgnoreCase("minutes")
                ? Duration.ofMinutes(preOrderConfig.getMinDuration())
                : Duration.ofHours(preOrderConfig.getMinDuration());

        Duration maxLead = unit.equalsIgnoreCase("minutes")
                ? Duration.ofMinutes(preOrderConfig.getMaxDuration())
                : Duration.ofHours(preOrderConfig.getMaxDuration());

        ZonedDateTime minAllowedUTC = nowUTC.plus(minLead);
        ZonedDateTime maxAllowedUTC = nowUTC.plus(maxLead);

        if (preOrderUTC.isBefore(minAllowedUTC)) {
            throw new ValidationException("Preorder time must be at least " + preOrderConfig.getMinDuration() + " "
                    + unit + " ahead of current time.");
        }

        if (preOrderUTC.isAfter(maxAllowedUTC)) {
            throw new ValidationException(
                    "Preorder time cannot be more than " + preOrderConfig.getMaxDuration() + " " + unit + " from now.");
        }

        LocalTime preOrderTimeIST = preOrderIST.toLocalTime();
        if (!isWithinDeliveryHours(preOrderTimeIST, deliveryHours)) {
            throw new ValidationException("Preorder time must be within restaurant delivery hours.");
        }

        ZonedDateTime fulfillmentIST = preOrderIST.minusHours(1);
        LocalTime fulfillmentTimeIST = fulfillmentIST.toLocalTime();

        if (!isWithinDeliveryHours(fulfillmentTimeIST, deliveryHours)) {
            throw new ValidationException("Restaurant cannot prepare this preorder. Fulfillment time ("
                    + fulfillmentTimeIST + " IST) is outside delivery hours.");
        }
    }

    public void startPreOrderWorkflow(String orderId, int scheduledDelay) {
        orderWorkflowService.startPreOrderWorkflow(orderId, scheduledDelay);
    }

    public Order update(String orderId, OrderDto orderDto) throws OrderNotFoundException {

        try {
            Order order = findById(orderId);
            if (order == null) {
                throw new OrderNotFoundException("Order not found " + orderId);
            }

            OrderStatusType oldStatus = order.getStatus();
            OrderStatusType requestedStatus = null;

            if (orderDto.getStatus() != null && !orderDto.getStatus().isBlank()) {
                requestedStatus = OrderStatusType.valueOf(orderDto.getStatus().toUpperCase());
            }

            if (requestedStatus != null && requestedStatus == oldStatus) {
                log.debug("Duplicate status update ignored currentStatus={}", requestedStatus);
            }
            // ModelMapper updates all fields, including status
            orderTranslation.updateEntityFromDto(orderDto, order);
            // Intentionally restore status: workflow fields must not be set via DTO mapping
            order.setStatus(oldStatus);
            // Persist entity-level updates
            save(order);

            // Apply workflow transition explicitly
            if (requestedStatus != null && requestedStatus != oldStatus) {

                Restaurant restaurant = restaurantService.findById(order.getRestaurantId());

                switch (requestedStatus) {
                    case ACCEPTED -> handleAccepted(order, restaurant);
                    case DELIVERED -> handleDelivered(order);
                    case CANCELLED -> handleCancelled(order, restaurant, oldStatus);
                }

                return updateOrderStatus(orderId, requestedStatus);
            }

            return order;

        } catch (PosException | DeliveryException | PaymentException | RequestTranslationException e) {
            log.error("Order update failed", e);
            throw new RuntimeException("Order update failed", e);
        }
    }

    private void handleAccepted(Order order, Restaurant restaurant) throws DeliveryException {
        if (!restaurant.getPosPartner().equalsIgnoreCase(PosPartner.SELF.name())) return;

        String fulfillMode =
                redisService.getRedisData(Constants.REDIS_KEY_FULFILL).orElse("smart");
        Delivery delivery = deliveryService.findByOrderId(order.getId());

        if (delivery == null || delivery.getStatus() != DeliveryOrderStatusType.PENDING) return;

        if (fulfillMode.equalsIgnoreCase("smart")) {
            deliveryService.processDeliverySmartFulfill(delivery, Constants.PET_POOJA);
        } else {
            deliveryService.processDeliveryStandardFulfill(delivery, Constants.PET_POOJA);
        }
    }

    private void handleDelivered(Order order) {
        Delivery delivery = deliveryService.findByOrderId(order.getId());
        posService.updatePosRiderStatus(delivery, order);
    }

    private void handleCancelled(Order order, Restaurant restaurant, OrderStatusType oldStatus)
            throws PosException, DeliveryException, PaymentException, RequestTranslationException {

        if (!Constants.CANCELABLE_STATUSES.contains(oldStatus)) return;

        if (!restaurant.getPosPartner().equalsIgnoreCase(PosPartner.SELF.name())) {
            PosOrderUpdateRequest req =
                    posOrderRequestTranslation.getPosOrderUpdateRequest(restaurant, order, "Cancellation");
            posService.updatePosOrder(req);
        }

        if (OrderType.fromCode(order.getOrderType()) == OrderType.H) {
            Delivery delivery = deliveryService.findByOrderId(order.getId());
            if (delivery != null) {
                deliveryService.cancelDeliveryOrder(delivery.getDeliveryOrderId());
            }
        }

        paymentService.createRefund(
                order.getId(), order.getGrandTotalAmount(), restaurant.isInstantRefund(), "Cancellation");
    }

    private boolean isTerminalStatus(OrderStatusType status) {
        return status == OrderStatusType.DELIVERED
                || status == OrderStatusType.CANCELLED
                || status == OrderStatusType.DROPPED_OFF
                || status == OrderStatusType.ERROR
                || status == OrderStatusType.PAYMENT_FAILED
                || status == OrderStatusType.REFUND_COMPLETED | status == OrderStatusType.REFUND_INITIATED;
    }

    private void consumeReferralTokenIfPaid(Order order, OrderStatusType newStatus) {
        if (newStatus != OrderStatusType.PAID || order.getReferralTokenId() == null) {
            return;
        }

        boolean consumed = referralTokenService.consumeTokenForPaidOrder(order.getReferralTokenId(), order.getId());
        if (consumed) {
            log.info(
                    "Referral token consumed for paid order: orderId={}, token={}",
                    order.getId(),
                    order.getReferralTokenId());
        } else {
            // Token already used by another order - clear referral attribution
            log.warn("Referral token already consumed, clearing attribution: orderId={}", order.getId());
            order.setReferralCode(null);
            order.setReferralTokenId(null);
            save(order);
        }
    }
}
