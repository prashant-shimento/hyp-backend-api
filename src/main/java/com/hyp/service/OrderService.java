package com.hyp.service;

import static com.hyp.util.ValidationUtils.isWithinDeliveryHours;

import com.hyp.constants.Constants;
import com.hyp.dto.OrderDto;
import com.hyp.dto.OrderDto.OrderAddonItem;
import com.hyp.dto.OrderDto.OrderItem;
import com.hyp.dto.OrderDto.OrderTax;
import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Partner;
import com.hyp.entity.Restaurant;
import com.hyp.enums.*;
import com.hyp.event.OrderEventPublisher;
import com.hyp.exception.*;
import com.hyp.model.PreOrder;
import com.hyp.repository.OrderRepository;
import com.hyp.request.PosCallbackRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.temporal.service.OrderWorkflowService;
import com.hyp.translation.OrderTranslation;
import com.hyp.translation.PosOrderRequestTranslation;
import com.hyp.util.CommonUtils;
import com.mongodb.client.result.UpdateResult;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

    public Order create(OrderDto orderDto) throws Exception {

        Restaurant restaurant = Optional.ofNullable(restaurantService.findById(orderDto.getRestaurantId()))
                .orElseThrow(() ->
                        new EntityNotFoundException(Restaurant.class.getSimpleName(), orderDto.getRestaurantId()));

        if (orderDto.getPartnerId() != null) {
            Optional.ofNullable(partnerService.findById(orderDto.getPartnerId()))
                    .orElseThrow(
                            () -> new EntityNotFoundException(Partner.class.getSimpleName(), orderDto.getPartnerId()));
        } else {
            Partner partner =
                    partnerService.findPartnersByRestaurantId(orderDto.getRestaurantId(), PartnerType.RESTAURANT);
            orderDto.setPartnerId(partner.getId());
        }

        if (!restaurant.isServiceable()) {
            throw new Exception("Restaurant is not serviceable");
        }
        if (!isWithinDeliveryHours(restaurant.getDeliveryHours())) {
            throw new Exception("Order cannot be processed: Outside delivery hours.");
        }
        if (!restaurant.isActive()) {
            throw new Exception("Restaurant is not active");
        }
        boolean isPreOrder = Boolean.TRUE.equals(orderDto.getPreOrder());
        PreOrder preOrderConfig = getPreOrder(orderDto, restaurant, isPreOrder);

        if (isPreOrder) {
            validatePreOrderTime(orderDto.getPreOrderDateTime(), preOrderConfig, restaurant.getDeliveryHours());
        }
        Customer customer = Optional.ofNullable(customerService.findById(orderDto.getCustomerId()))
                .orElseThrow(
                        () -> new EntityNotFoundException(Customer.class.getSimpleName(), orderDto.getCustomerId()));

        Address address;

        // TODO: Need to validate orderType from DB once front end accommodate the
        // changes
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
                    throw new Exception("Location Not Deliverable");
                }
            } else { // TODO: this has to be moved to orderType Dine and needs front end changes in
                // mocoda
                if (orderDto.getSeat() == null || orderDto.getScreen() == null) {
                    throw new Exception("Delivery Details are missing, and both Seat and Screen must be provided.");
                }
            }
        }

        if (!isWithinDeliveryHours(restaurant.getDeliveryHours())) {
            throw new Exception("Order cannot be processed: Outside delivery hours.");
        }

        if (orderDto.getOrderDiscount() != null) {
            for (OrderDto.OrderDiscount discount : orderDto.getOrderDiscount()) {
                if (!discountService.isExistsById(discount.getId())) {
                    throw new Exception("Discount not found " + discount.getId());
                }
            }
        }

        if (orderDto.getOrderTax() != null) {
            for (OrderTax ordertax : orderDto.getOrderTax()) {
                if (!taxService.isExistsById(ordertax.getId())) {
                    throw new Exception("Tax not found " + ordertax.getId());
                }
            }
        }

        double itemTotalAmount = 0;
        for (OrderItem orderItem : orderDto.getOrderItems()) {
            if (orderItem.getVariationId() != null || orderItem.getVariationName() != null) {
                if (!variationService.isExistsById(orderItem.getId())) {
                    throw new Exception("Variation not found " + orderItem.getId());
                }
            } else if (!itemService.isExistsById(orderItem.getId())) {
                throw new Exception("Item not found " + orderItem.getId());
            }

            double addonTotal = 0;
            if (orderItem.getOrderAddonItems() != null) {
                for (OrderAddonItem orderAddonItem : orderItem.getOrderAddonItems()) {
                    if (!addonItemService.isExistsById(orderAddonItem.getAddonItemId())) {
                        throw new Exception("AddonItem not found " + orderAddonItem.getAddonItemId());
                    }
                    addonTotal += orderAddonItem.getPrice() * orderAddonItem.getQuantity();
                }
            }
            if (orderItem.getVariationId() == null) {
                orderItem.setItemAttribute(
                        itemService.findById(orderItem.getId()).getItemAttributeId());
            }

            itemTotalAmount += (orderItem.getPrice() * orderItem.getQuantity()) + addonTotal;
        }

        Order order = orderTranslation.getEntity(orderDto);
        order.setStatus(OrderStatusType.CREATED);
        order.setOrderTime(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.getOrderLogs().add(new Order.OrderLog(OrderStatusType.CREATED.name()));
        order.setItemTotalAmount(itemTotalAmount);
        order.setPlatformFee(paymentService.calculatePlatformFee(orderDto.getGrandTotalAmount(), restaurant));
        order = save(order);

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
                    log.info("Scheduling fulfillment for order {} after {} minutes", order.getId(), fulfillmentDelay);
                    boolean isWorkflowEnabled = redisService
                            .getRedisData(Constants.FULFILLMENT_WORKFLOW_ENABLED)
                            .map(Boolean::parseBoolean)
                            .orElse(false);

                    if (isWorkflowEnabled) {
                        startOrderFulfillmentWorkflow(order.getId(), fulfillmentDelay);

                    } else {
                        deliveryService.setFulfillExpiry(order.getId(), fulfillmentDelay);
                    }
                    return;
                }

                Delivery delivery = deliveryService.findByOrderId(order.getId());

                if (delivery == null || !DeliveryOrderStatusType.PENDING.equals(delivery.getStatus())) {
                    log.warn("No PENDING delivery found for order {}. Skipping fulfillment.", order.getId());
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
        }
    }

    @Transactional
    public Order updateOrderStatus(String orderId, OrderStatusType newStatus) {

        Order order = findById(orderId);
        OrderStatusType oldStatus = order.getStatus();

        if (oldStatus == newStatus) {
            log.info("Order {} already in status {}, skipping update.", orderId, newStatus);
            return order;
        }

        order.setStatus(newStatus);
        order.getOrderLogs().add(new Order.OrderLog(newStatus.name()));

        save(order);

        try {
            orderEventPublisher.publishOrderStatusChangeEvent(order);

            if (OrderStatusType.DELIVERED.equals(newStatus)) {
                orderEventPublisher.publishSettlementEvent(order);
            }
        } catch (Exception e) {
            log.error("Failed to publish event for order {} status {}: {}", orderId, newStatus, e.getMessage());
        }

        log.info("Order {} status updated from {} -> {}", orderId, oldStatus, newStatus);
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
            log.info("Order {} status updated to {} atomically.", orderId, newStatus);
            Order updatedOrder = this.findById(orderId);
            orderEventPublisher.publishOrderStatusChangeEvent(updatedOrder);
            return true;
        } else {
            log.info("Order {} not updated; already processed or in final state.", orderId);
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
            OrderStatusType newStatus =
                    OrderStatusType.valueOf(orderDto.getStatus().toUpperCase());
            orderTranslation.updateEntityFromDto(orderDto, order);
            Restaurant restaurant = restaurantService.findById(order.getRestaurantId());

            switch (newStatus) {
                case ACCEPTED -> handleAccepted(order, restaurant);
                case DELIVERED -> handleDelivered(order);
                case CANCELLED -> handleCancelled(order, restaurant, oldStatus);
            }

            return updateOrderStatus(orderId, newStatus);
        } catch (PosException | DeliveryException | PaymentException | RequestTranslationException e) {
            log.error("Order {} update failed: {}", orderId, e.getMessage());
            throw new RuntimeException("Order update failed");
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
}
