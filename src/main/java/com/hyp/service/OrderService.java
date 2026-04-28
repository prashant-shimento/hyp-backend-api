package com.hyp.service;

import com.hyp.constants.Constants;
import com.hyp.dto.OrderDto;
import com.hyp.entity.*;
import com.hyp.enums.*;
import com.hyp.enums.OrderType;
import com.hyp.event.OrderEventPublisher;
import com.hyp.exception.*;
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
import com.hyp.validation.OrderValidator;
import com.mongodb.client.result.UpdateResult;
import io.micrometer.core.instrument.Timer;
import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    OrderTranslation orderTranslation;

    @Autowired
    DeliveryService deliveryService;

    @Autowired
    PaymentService paymentService;

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
    OrderValidator orderValidator;

    @Autowired
    RiderAvailabilityMonitor riderAvailabilityMonitor;

    public Order create(OrderDto orderDto) throws Exception {

        Timer.Sample timerSample = metrics.startTimer();
        long startTime = System.currentTimeMillis();

        // Phase 1+2: Parallel lookups + validation
        OrderValidator.OrderValidationResult validation = orderValidator.validate(orderDto);

        // Phase 3: Build and save order
        Order order = orderTranslation.getEntity(orderDto);
        order.setStatus(OrderStatusType.CREATED);
        order.setOrderTime(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        // Override client-supplied monetary values with server-calculated amounts
        // only when server correction is enabled for this partner (feature flag)
        if (validation.serverCorrectionApplied()) {
            order.setItemTotalAmount(validation.itemTotalAmount());
            order.setTotalAmount(validation.totalAmount());
            order.setGrandTotalAmount(validation.grandTotalAmount());
            order.setTaxAmount(validation.taxTotalAmount());
            order.setDiscountAmount(validation.discountAmount());
            order.setDeliveryCharge(validation.deliveryCharge());
        }
        order.getOrderLogs().add(new Order.OrderLog(OrderStatusType.CREATED.name()));

        // Referral token
        if (validation.referralToken() != null) {
            order.setReferralCode(validation.referralToken().getReferralCode());
            order.setReferralTokenId(validation.referralToken().getId());
        }
        long creationStart = System.currentTimeMillis();
        order = save(order);
        log.info("Order saved with ID: {} in {}ms", order.getId(), System.currentTimeMillis() - creationStart);

        // Phase 4: Async — notifications + metrics
        Order finalOrder = order;
        Restaurant restaurant = validation.restaurant();
        Customer customer = validation.customer();

        CompletableFuture.runAsync(() -> {
            try {
                List<String> parameters = CommonUtils.buildStringList(
                        customer.getName(),
                        customer.getMobile(),
                        finalOrder.getId(),
                        finalOrder.getStatus(),
                        restaurant.getRestaurantName());
                notificationService.sendInternalGroupNotification(Constants.META_ORDER_ALERT_TEMPLATE, parameters);
                oneSignalAlertService.notifyNewOrder(
                        customer.getName(),
                        customer.getMobile(),
                        finalOrder.getId(),
                        finalOrder.getStatus(),
                        restaurant.getRestaurantName());

                metrics.stopTimer(timerSample, MetricsEvent.ORDER, MetricTag.ACTION, "create");
                metrics.count(
                        MetricsEvent.ORDER,
                        MetricTag.ACTION,
                        "create",
                        MetricTag.STATUS,
                        "success",
                        MetricTag.ORDER_TYPE,
                        finalOrder.getOrderType());

                ObservabilityContext.setOrderContext(finalOrder.getId(), finalOrder.getRestaurantId());

            } catch (Exception e) {
                log.error("Async notification failed for order {}", finalOrder.getId(), e);
            } finally {
                ObservabilityContext.clear();
            }
        });

        log.info("Order create completed in {}ms total", System.currentTimeMillis() - startTime);
        return order;
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
            Order order = this.findById(orderId);
            if (order == null) {
                throw new Exception("Order not found " + orderId);
            }

            if (order.getPaymentType() != PaymentType.COD) {
                if (paymentService.findByOrderId(orderId) == null) {
                    throw new Exception("Payment not completed" + orderId);
                }
            }

            OrderStatusType newOrderStatus = OrderStatusType.getOrderStatusByPosStatus(posCallbackRequest.getStatus());
            order = updateOrderStatus(orderId, newOrderStatus);

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
                    deliveryService.cancelDeliveryOrder(delivery);
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

        if (newStatus == OrderStatusType.SEARCHING_RIDER) {
            riderAvailabilityMonitor.onSearchingRiderEntered(order.getRestaurantId());
        } else if (oldStatus == OrderStatusType.SEARCHING_RIDER) {
            riderAvailabilityMonitor.onSearchingRiderExited(order.getRestaurantId());
        }

        log.info("Status changed from={} to={}", oldStatus, newStatus);
        return order;
    }

    public void startOrderFulfillmentWorkflow(String orderId, int fulfillmentDelay) {
        orderWorkflowService.startOrderFulfillmentWorkflow(orderId, fulfillmentDelay);
    }

    /**
     * Atomic status update guarded by an allow-list.  Used by the payment
     * success path where a race between webhook and polling is possible.
     */
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
                deliveryService.cancelDeliveryOrder(delivery);
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
                || status == OrderStatusType.REFUND_COMPLETED
                || status == OrderStatusType.REFUND_INITIATED;
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
