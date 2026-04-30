package com.hyp.listener;

import com.hyp.constants.Constants;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Partner;
import com.hyp.entity.Payment;
import com.hyp.entity.Restaurant;
import com.hyp.entity.User;
import com.hyp.enums.OrderStatusType;
import com.hyp.enums.OrderType;
import com.hyp.enums.PartnerType;
import com.hyp.enums.PosPartner;
import com.hyp.event.OrderEvent;
import com.hyp.event.OrderEventPublisher;
import com.hyp.event.OrderStatusChangeEvent;
import com.hyp.event.PaymentEvent;
import com.hyp.exception.OneSignalException;
import com.hyp.request.OneSignalNotificationAlias;
import com.hyp.request.OneSignalNotificationRequest;
import com.hyp.service.*;
import com.hyp.util.CommonUtils;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventListener {

    @Autowired
    SimpMessagingTemplate messageTemplate;

    @Autowired
    RestaurantService restaurantService;

    @Autowired
    CustomerService customerService;

    @Autowired
    DeliveryService deliveryService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private PartnerService partnerService;

    @Autowired
    UserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    OrderService orderService;

    @Value("${onesignal.partner.app.id}")
    private String partnerAppId;

    @Async
    @EventListener
    public void handleProcessOrder(OrderEvent event) {
        log.info("Order Event listener handleProcessOrder");
        Order order = event.getOrder();
        log.info("Order Type {}", order.getOrderType());
        if (OrderType.fromCode(order.getOrderType()) == OrderType.H && !order.isPreOrder()) {
            orderEventPublisher.publishDeliveryOrderEvent(order);
        }
        if (restaurantService
                .findById(order.getRestaurantId())
                .getPosPartner()
                .equalsIgnoreCase(PosPartner.PET_POOJA.name())) {
            orderEventPublisher.publishPosOrderEvent(order);
        }
        if (order.isPreOrder()) {
            LocalDateTime preOrderTime = order.getPreOrderDateTime();
            LocalDateTime preOrderUTC = CommonUtils.convertISTtoUTC(preOrderTime);
            LocalDateTime fulfillmentUTC = preOrderUTC.minusHours(1);
            long ttlSeconds = CommonUtils.calculateTTLInSeconds(fulfillmentUTC);
            if (ttlSeconds <= 0) {
                ttlSeconds = 1;
            }
            int scheduledDelayInMins = Math.toIntExact((ttlSeconds + 59) / 60);
            orderService.startPreOrderWorkflow(order.getId(), scheduledDelayInMins);

            String redisDelayKey = "order:" + order.getId() + ":delay";
            redisService.setRedisData(redisDelayKey, OrderStatusType.PAID, ttlSeconds);
            return;
        }
        String delayAlertTime =
                redisService.getRedisData(Constants.REDIS_KEY_DELAY_ALERT_TIME).orElse("45");
        String redisDelayKey = "order:" + order.getId() + ":delay";
        long delayTtl = Duration.ofMinutes(Integer.parseInt(delayAlertTime)).toSeconds();
        redisService.setRedisData(redisDelayKey, OrderStatusType.PAID, delayTtl);
    }

    @Async
    @EventListener
    public void handleOrderStatusChange(OrderStatusChangeEvent event) {
        log.info("Order Event listener handleOrderStatusChange");

        List<String> templateParameters = null;
        Order order = event.getOrder();

        messageTemplate.convertAndSend("/topic/order-status", order);

        Customer customer = customerService.findById(order.getCustomerId());
        Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
        Delivery delivery = deliveryService.findByOrderId(order.getId());
        Partner partner = partnerService.findPartnersByRestaurantId(restaurant.getId(), PartnerType.THEATRE);

        switch (order.getStatus()) {
            case CREATED:
                if (partner != null) {
                    List<String> parameters = CommonUtils.buildStringList(
                            customer.getName(), restaurant.getRestaurantName(), order.getId());
                    notificationService.sendNotification(
                            customer.getMobile(), Constants.META_ORDER_CREATED_THEATRE_TEMPLATE, parameters);
                }
                break;

            case ACCEPTED:
                if (partner != null) {
                    List<String> parameters = CommonUtils.buildStringList(
                            customer.getName(),
                            restaurant.getRestaurantName(),
                            order.getId(),
                            order.getScreen(),
                            order.getSeat());
                    notificationService.sendNotification(
                            customer.getMobile(), Constants.META_ORDER_CONFIRMED_THEATRE_TEMPLATE, parameters);
                } else {
                    List<String> parameters = CommonUtils.buildStringList(
                            customer.getName(),
                            restaurant.getRestaurantName(),
                            restaurant.getCity(),
                            order.getId(),
                            restaurant.getSupportContact());
                    notificationService.sendNotification(
                            customer.getMobile(), Constants.META_ORDER_CONFIRMED_TEMPLATE, parameters);
                }
                break;

            case PAID:
                if (restaurant.getPosPartner().equalsIgnoreCase(PosPartner.SELF.name())) {
                    List<User> users = userService.findByRestaurantId(order.getRestaurantId());
                    if (users != null && !users.isEmpty()) {
                        Map<String, Object> customDataMap = new HashMap<>();
                        customDataMap.put("orderId", order.getId());
                        customDataMap.put("customerName", customer.getName());

                        List<Map<String, Object>> orderItems = order.getOrderItems().stream()
                                .map(orderItem -> {
                                    Map<String, Object> itemMap = new HashMap<>();
                                    itemMap.put("itemName", orderItem.getName());
                                    itemMap.put("quantity", orderItem.getQuantity());
                                    itemMap.put("price", orderItem.getPrice());
                                    itemMap.put("finalPrice", orderItem.getFinalPrice());
                                    itemMap.put("variationName", orderItem.getVariationName());
                                    if (orderItem.getOrderAddonItems() != null
                                            && !orderItem.getOrderAddonItems().isEmpty()) {
                                        List<Map<String, Object>> addons = orderItem.getOrderAddonItems().stream()
                                                .map(addon -> {
                                                    Map<String, Object> addonMap = new HashMap<>();
                                                    addonMap.put("addonName", addon.getAddonItemName());
                                                    addonMap.put("price", addon.getPrice());
                                                    addonMap.put("quantity", addon.getQuantity());
                                                    return addonMap;
                                                })
                                                .toList();
                                        itemMap.put("addons", addons);
                                    }
                                    return itemMap;
                                })
                                .toList();
                        customDataMap.put("items", orderItems);

                        // Send notification to all users of the restaurant
                        List<String> userIds = users.stream().map(User::getId).toList();
                        OneSignalNotificationRequest request = OneSignalNotificationRequest.builder()
                                .targetChannel("push")
                                .includeAliases(OneSignalNotificationAlias.builder()
                                        .externalId(userIds)
                                        .build())
                                .appId(partnerAppId)
                                .templateId(Constants.ONE_SIGNAL_ORDER_PLACED_TEMPLATE)
                                .customData(customDataMap)
                                .build();

                        try {
                            notificationService.sendOneSignalNotificationForPartner(request);
                        } catch (OneSignalException e) {
                            log.error("Error occurred in sending push notification {}", request);
                        }
                    }
                }

                templateParameters = CommonUtils.buildStringList(
                        customer.getName(),
                        restaurant.getRestaurantName(),
                        order.getId(),
                        order.getStatus(),
                        restaurant.getSupportContact());

                notificationService.sendNotification(
                        customer.getMobile(), Constants.META_ORDER_PAID_TEMPLATE, templateParameters);
                break;

            case PICKED_UP:
                templateParameters = CommonUtils.buildStringList(
                        customer.getName(),
                        order.getId(),
                        delivery.getFulfillment().getRider().getName(),
                        delivery.getFulfillment().getRider().getMobile(),
                        restaurant.getSupportContact());

                notificationService.sendNotification(
                        customer.getMobile(),
                        Constants.META_ORDER_PICKEDUP_TEMPLATE,
                        templateParameters,
                        order.getId());
                break;

            case DELIVERED:
                if (partner != null) {
                    templateParameters = CommonUtils.buildStringList(
                            customer.getName(), order.getId(), restaurant.getSupportContact());

                    notificationService.sendNotification(
                            customer.getMobile(), Constants.META_ORDER_DELIVERED_THEATRE_TEMPLATE, templateParameters);
                } else {
                    templateParameters = CommonUtils.buildStringList(
                            customer.getName(),
                            restaurant.getRestaurantName(),
                            order.getId(),
                            restaurant.getSupportContact(),
                            restaurant.getWebsiteUrl());

                    notificationService.sendNotification(
                            customer.getMobile(), Constants.META_ORDER_DELIVERED_TEMPLATE, templateParameters);
                }
                break;

            case CANCELLED:
                templateParameters =
                        CommonUtils.buildStringList(customer.getName(), order.getId(), restaurant.getRestaurantName());

                notificationService.sendNotification(
                        customer.getMobile(), Constants.META_ORDER_CANCELLED_TEMPLATE, templateParameters);
                break;

            case REFUND_INITIATED:
            case REFUND_COMPLETED:
                Payment payment = paymentService.findByOrderId(order.getId());

                if (payment != null && payment.getRefund() != null) {
                    String customerName = customer.getName();
                    String refundAmount = String.valueOf(
                            CommonUtils.parseISOAmount((int) payment.getRefund().getAmount()));
                    String orderId = order.getId();
                    String restaurantName = restaurant.getRestaurantName();
                    String refundReason = payment.getRefund().getReason();
                    String refundId = payment.getRefund().getId();
                    String supportContact = restaurant.getSupportContact();

                    templateParameters = CommonUtils.buildStringList(
                            customerName,
                            refundAmount,
                            orderId,
                            restaurantName,
                            refundReason,
                            refundId,
                            supportContact,
                            restaurantName);

                    notificationService.sendNotification(
                            customer.getMobile(), Constants.META_REFUND_TEMPLATE, templateParameters);
                } else {
                    log.warn("Refund details not found for order ID: {}", order.getId());
                }
                break;
            default:
                break;
        }
    }

    @Async
    @EventListener
    public void handlePaymentSuccessEvent(PaymentEvent event) {
        log.info("Order Event listener handlePaymentSuccessEvent");
        paymentService.processSuccessPayment(event.getOrder(), event.getPayment(), event.getPaymentStatus());
    }
}
