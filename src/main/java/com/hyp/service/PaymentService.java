package com.hyp.service;

import com.hyp.constants.Constants;
import com.hyp.dto.RazorpayVerifyDto;
import com.hyp.dto.RefundDto;
import com.hyp.entity.Partner;
import com.hyp.entity.Payment;
import com.hyp.entity.Restaurant;
import com.hyp.enums.FeeType;
import com.hyp.enums.OrderStatusType;
import com.hyp.enums.RefundType;
import com.hyp.event.OrderEventPublisher;
import com.hyp.exception.PaymentException;
import com.hyp.exception.ValidationException;
import com.hyp.model.PaymentConfig;
import com.hyp.model.PaymentRoute;
import com.hyp.observability.ApplicationMetrics;
import com.hyp.observability.MetricTag;
import com.hyp.observability.MetricsEvent;
import com.hyp.repository.PaymentRepository;
import com.hyp.temporal.service.OrderTrackWorkflowService;
import com.hyp.temporal.service.OrderWorkflowService;
import com.hyp.util.CommonUtils;
import com.hyp.util.EncryptionUtils;
import com.razorpay.Account;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.razorpay.Utils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentService extends BaseServiceImpl<Payment, String> {

    @Value("${razorpay.key}")
    private String razorPayKey;

    @Value("${razorpay.secret}")
    private String razorPaySecret;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    OrderService orderService;

    @Autowired
    RestaurantService restaurantService;

    @Autowired
    PartnerService partnerService;

    @Autowired
    RedisService redisService;

    @Autowired
    CacheService cacheService;

    private static final String PAYMENT_CONFIG_CACHE = "paymentConfig";

    @Autowired
    OrderWorkflowService orderWorkflowService;

    @Autowired
    OrderTrackWorkflowService orderTrackWorkflowService;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private ApplicationMetrics metrics;

    public String getRazorpayKey() throws Exception {
        return EncryptionUtils.decrypt(razorPayKey);
    }

    public Payment createPaymentOrder(com.hyp.entity.Order order) throws PaymentException {
        long startTime = System.currentTimeMillis();
        try {
            if (order == null || !OrderStatusType.CREATED.equals(order.getStatus())) {
                throw new ValidationException(
                        "Order is in Invalid Status: " + (order != null ? order.getStatus() : "null"));
            }

            String orderId = order.getId();
            double amount = order.getGrandTotalAmount();
            Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
            RazorpayClient razorpayClient = getRazorpayClient(orderId);

            Payment payment;
            if (restaurant.isPaymentRoutingEnabled()) {
                try {
                    payment = createRoutingOrder(razorpayClient, orderId, amount, restaurant);
                } catch (Exception routingEx) {
                    log.warn("Payment routing failed, falling back to standard reason={}", routingEx.getMessage());
                    payment = createStandardOrder(razorpayClient, orderId, amount, restaurant);
                }
            } else {
                payment = createStandardOrder(razorpayClient, orderId, amount, restaurant);
            }
            setPaymentCheck(orderId);
            log.info("Payment created amount={} routing={}", amount, restaurant.isPaymentRoutingEnabled());
            log.info("Payment creation time : {} ms", System.currentTimeMillis() - startTime);

            metrics.count(
                    MetricsEvent.PAYMENT,
                    MetricTag.PARTNER,
                    Constants.RAZOR_PAY,
                    MetricTag.ACTION,
                    "create",
                    MetricTag.STATUS,
                    payment.getStatus(),
                    MetricTag.RESULT,
                    "success");
            return save(payment);
        } catch (Exception e) {
            log.error("Payment creation failed", e);
            metrics.count(
                    MetricsEvent.PAYMENT,
                    MetricTag.PARTNER,
                    Constants.RAZOR_PAY,
                    MetricTag.ACTION,
                    "create",
                    MetricTag.RESULT,
                    "failed");
            throw new PaymentException("Error creating payment order: " + e.getMessage(), e);
        }
    }

    private Payment createRoutingOrder(
            RazorpayClient razorpayClient, String orderId, double amount, Restaurant restaurant)
            throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", CommonUtils.getISOAmount(amount));
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", CommonUtils.genId());
        JSONArray transfers = new JSONArray();
        double totalTransferAmount = 0.0;

        for (PaymentRoute route :
                Optional.ofNullable(restaurant.getPaymentRouteList()).orElse(Collections.emptyList())) {
            double transferAmount = calculateRoutingAmount(amount, restaurant);

            if (transferAmount <= 0 || totalTransferAmount + transferAmount > amount) {
                log.warn(
                        "Transfer amount {} for route {} is invalid or exceeds remaining order amount. Skipping...",
                        transferAmount,
                        route.getRecipientId());
                continue;
            }

            try {
                Account account = razorpayClient.account.fetch(route.getRecipientId());
                if (account == null) {
                    log.warn("Skipping route: Linked account not found for {}", route.getRecipientId());
                    continue;
                }

                JSONObject transfer = new JSONObject();
                transfer.put("account", route.getRecipientId());
                transfer.put("amount", CommonUtils.getISOAmount(transferAmount));
                transfer.put("currency", "INR");
                transfer.put("on_hold", 0);

                JSONObject notes = new JSONObject();
                notes.put("branch", restaurant.getId());
                notes.put("name", restaurant.getRestaurantName());

                transfer.put("notes", notes);

                transfers.put(transfer);
                totalTransferAmount += transferAmount;
            } catch (RazorpayException ex) {
                log.warn(
                        "Error fetching account for {}: {}. Skipping this route.",
                        route.getRecipientId(),
                        ex.getMessage());
            }
        }

        if (transfers.isEmpty()) {
            throw new RazorpayException("No valid payment routes found. Cannot create routing order.");
        }

        orderRequest.put("transfers", transfers);
        Order paymentOrder = razorpayClient.orders.create(orderRequest);
        return buildPaymentFromOrder(paymentOrder, orderId);
    }

    private Payment createStandardOrder(
            RazorpayClient razorpayClient, String orderId, double amount, Restaurant restaurant)
            throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", CommonUtils.getISOAmount(amount));
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", CommonUtils.genId());

        JSONObject notes = new JSONObject();
        notes.put("restaurant", restaurant.getId() + ":" + restaurant.getRestaurantName());
        orderRequest.put("notes", notes);
        Order paymentOrder = razorpayClient.orders.create(orderRequest);
        return buildPaymentFromOrder(paymentOrder, orderId);
    }

    private Payment buildPaymentFromOrder(Order razorpayOrder, String orderId) {
        Payment payment = new Payment();
        payment.setId(CommonUtils.genId());
        payment.setPaymentOrderId(razorpayOrder.get("id"));
        payment.setAmount(Double.parseDouble(razorpayOrder.get("amount").toString()));
        payment.setReceipt(razorpayOrder.get("receipt"));
        payment.setStatus(razorpayOrder.get("status"));
        payment.setCurrency(razorpayOrder.get("currency"));
        payment.setProvider(Constants.RAZOR_PAY);
        payment.setOrderId(orderId);

        orderService.updateOrderStatus(orderId, OrderStatusType.PAYMENT_PENDING);
        return payment;
    }

    public void startOrderPaymentWorkflow(String orderId) {
        orderWorkflowService.startOrderPaymentWorkflow(orderId);
    }

    public void startOrderTrackWorkflow(String orderId) {
        orderTrackWorkflowService.startOrderTrackWorkflow(orderId);
    }

    private void updateOrderStatusInRedis(String orderId) {
        String redisStateKey = "order:" + orderId + ":state";
        String redisPaymentKey = "order:" + orderId + ":payment";
        long stateTtl = Duration.ofMinutes(15).toSeconds();
        long paymentTtl = Duration.ofMinutes(4).toSeconds();

        redisService.setRedisData(redisStateKey, OrderStatusType.PAYMENT_PENDING, stateTtl);
        redisService.setRedisData(redisPaymentKey, OrderStatusType.PAYMENT_PENDING, paymentTtl);
    }

    private void setPaymentCheck(String orderId) {
        boolean isWorkflowEnabled = redisService
                .getRedisData(Constants.PAYMENT_WORKFLOW_ENABLED)
                .map(Boolean::parseBoolean)
                .orElse(false);

        if (isWorkflowEnabled) {
            startOrderPaymentWorkflow(orderId);
        } else {
            updateOrderStatusInRedis(orderId);
        }
    }

    private double calculateRoutingAmount(double orderAmount, Restaurant restaurant) {
        double platformFee = calculatePlatformFee(orderAmount, restaurant);
        double petPoojaApiFee = orderAmount / 100.0;
        double deliveryFee = (platformFee <= 0) ? restaurant.getDeliveryFee() : 0.0;
        double total = platformFee + petPoojaApiFee + deliveryFee;
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public double calculatePlatformFee(double orderAmount, Restaurant restaurant) {
        return Optional.ofNullable(restaurant.getPlatformFee()).orElse(Collections.emptyList()).stream()
                .filter(rule -> (rule.getMinOrderAmount() == null || orderAmount >= rule.getMinOrderAmount())
                        && (rule.getMaxOrderAmount() == null || orderAmount <= rule.getMaxOrderAmount()))
                .findFirst()
                .map(rule -> {
                    if (rule.getFeeType() == FeeType.FIXED) {
                        return rule.getFeeValue();
                    } else {
                        return (orderAmount * rule.getFeeValue()) / 100.0;
                    }
                })
                .orElse(0.0);
    }

    public boolean verifySignature(RazorpayVerifyDto razorPayVerifyDto, String orderId) throws PaymentException {
        try {
            log.info("Verifying payment signature");
            JSONObject verifyRequest = new JSONObject();
            verifyRequest.put("razorpay_order_id", razorPayVerifyDto.getRazorpayOrderId());
            verifyRequest.put("razorpay_payment_id", razorPayVerifyDto.getRazorpayPaymentId());
            verifyRequest.put("razorpay_signature", razorPayVerifyDto.getRazorpaySignature());
            return Utils.verifyPaymentSignature(
                    verifyRequest,
                    EncryptionUtils.decrypt(getRazorpayPaymentConfig(orderId).getSecret()));
        } catch (Exception e) {
            log.error("Payment signature verification failed", e);
            throw new PaymentException("Error in verifySignature: " + e.getMessage(), e);
        }
    }

    public String fetchPaymentOrderStatus(String orderId) throws PaymentException {
        try {
            RazorpayClient razorpayClient = getRazorpayClient(orderId);
            Payment payment = findByOrderId(orderId);
            Order order = razorpayClient.orders.fetch(payment.getPaymentOrderId());
            return order.get("status");
        } catch (Exception e) {
            log.error("Failed to fetch payment status", e);
            throw new PaymentException("Error fetchOrderStatus: " + e.getMessage(), e);
        }
    }

    public String fetchPaymentId(String orderId) {
        try {
            RazorpayClient razorpayClient = getRazorpayClient(orderId);
            Payment payment = findByOrderId(orderId);

            List<com.razorpay.Payment> payments = razorpayClient.orders.fetchPayments(payment.getPaymentOrderId());

            com.razorpay.Payment matchedPayment = payments.stream()
                    .filter(p -> {
                        String status = p.get("status");
                        return "captured".equalsIgnoreCase(status) || "authorized".equalsIgnoreCase(status);
                    })
                    .findFirst()
                    .orElse(null);

            if (matchedPayment != null) {
                String paymentId = matchedPayment.get("id");
                log.debug("Fetched razorpay paymentId={}", paymentId);
                return paymentId;
            }

            log.warn("No captured/authorized payment found");
        } catch (Exception e) {
            log.error("Failed to fetch razorpay payment ID", e);
        }
        return null;
    }

    public Payment findByPaymentOrderId(String paymentOrderId) {
        return paymentRepository.findByPaymentOrderId(paymentOrderId);
    }

    public Payment findByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    public Payment findByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }

    public Payment createRefund(String orderId, double amount, boolean instantRefund, String reason)
            throws PaymentException {
        log.info("Creating refund amount={} instant={}", amount, instantRefund);
        try {
            Payment payment = paymentRepository.findByOrderId(orderId);

            RazorpayClient razorpayClient = getRazorpayClient(orderId);
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", CommonUtils.getISOAmount(amount));
            if (instantRefund) {
                refundRequest.put("speed", RefundType.optimum.name());
            }
            Refund refund = razorpayClient.payments.refund(payment.getPaymentId(), refundRequest);

            Payment.Refund paymentRefund = new Payment.Refund();
            paymentRefund.setId(refund.get("id"));
            paymentRefund.setAmount(Double.parseDouble(refund.get("amount").toString()));
            paymentRefund.setCurrency(refund.get("INR"));
            paymentRefund.setStatus(refund.get("status"));
            paymentRefund.setSpeedProcessed(refund.get("speed_processed"));
            paymentRefund.setSpeedRequested(refund.get("speed_requested"));
            paymentRefund.setCreatedAt((Date) refund.get("created_at"));
            paymentRefund.setReason(Optional.ofNullable(reason).orElse(Constants.REFUND_REASON));
            payment.setRefund(paymentRefund);

            save(payment);
            orderService.updateOrderStatus(
                    orderId, OrderStatusType.getOrderStatusByRefundStatus(paymentRefund.getStatus()));
            metrics.count(
                    MetricsEvent.PAYMENT,
                    MetricTag.PARTNER,
                    Constants.RAZOR_PAY,
                    MetricTag.ACTION,
                    "refund",
                    MetricTag.STATUS,
                    paymentRefund.getStatus(),
                    MetricTag.RESULT,
                    "success");
            return payment;
        } catch (Exception e) {
            log.error("Refund creation failed", e);
            metrics.count(
                    MetricsEvent.PAYMENT,
                    MetricTag.PARTNER,
                    Constants.RAZOR_PAY,
                    MetricTag.ACTION,
                    "refund",
                    MetricTag.RESULT,
                    "failed");
            throw new PaymentException("Error creating refund order: " + e.getMessage(), e);
        }
    }

    public RefundDto fetchRefund(String orderId) throws PaymentException {
        try {
            Payment payment = paymentRepository.findByOrderId(orderId);

            RazorpayClient razorpayClient = getRazorpayClient(orderId);

            Refund refund = razorpayClient.payments.fetchRefund(
                    payment.getPaymentId(), payment.getRefund().getId());
            return RefundDto.builder()
                    .id(refund.get("id"))
                    .paymentId(refund.get("payment_id"))
                    .paymentOrderId(payment.getPaymentOrderId())
                    .orderId(orderId)
                    .currency(refund.get("currency"))
                    .amount(CommonUtils.parseISOAmount(refund.get("amount")))
                    .status(refund.get("status"))
                    .speedProcessed(refund.get("speed_processed"))
                    .speedRequested(refund.get("speed_requested"))
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch refund", e);
            throw new PaymentException("Error fetchRefund refund order: " + e.getMessage(), e);
        }
    }

    /**
     * Get PaymentConfig for a restaurant using L1/L2 cache.
     * L1 (Caffeine): 30s, L2 (Redis): 5min
     */
    private PaymentConfig getPaymentConfig(String restaurantId) {
        return cacheService.getOrLoad(
                PAYMENT_CONFIG_CACHE, restaurantId, PaymentConfig.class, () -> loadPaymentConfig(restaurantId));
    }

    private PaymentConfig loadPaymentConfig(String restaurantId) {
        Restaurant restaurant = restaurantService.findById(restaurantId);

        return Optional.ofNullable(restaurant.getPaymentPartner())
                .map(partnerService::findById)
                .map(Partner::getApiConfigs)
                .map(apiConfig -> PaymentConfig.builder()
                        .key(apiConfig.getKey())
                        .secret(apiConfig.getSecret())
                        .build())
                .orElseGet(() -> {
                    log.debug("Using default payment config for restaurant: {}", restaurantId);
                    return PaymentConfig.builder()
                            .key(razorPayKey)
                            .secret(razorPaySecret)
                            .build();
                });
    }

    /**
     * Get RazorpayClient for an order.
     * PaymentConfig is cached via L1/L2 cache.
     */
    private RazorpayClient getRazorpayClient(String orderId) throws PaymentException {
        try {
            String restaurantId = orderService.findById(orderId).getRestaurantId();
            PaymentConfig config = getPaymentConfig(restaurantId);
            return new RazorpayClient(
                    EncryptionUtils.decrypt(config.getKey()), EncryptionUtils.decrypt(config.getSecret()));
        } catch (Exception e) {
            throw new PaymentException("Failed to initialize RazorpayClient", e);
        }
    }

    /**
     * Evict payment config cache for a restaurant (call when partner config changes).
     */
    public void evictPaymentConfigCache(String restaurantId) {
        cacheService.evict(PAYMENT_CONFIG_CACHE, restaurantId);
        log.info("Evicted payment config cache for restaurant: {}", restaurantId);
    }

    private PaymentConfig getRazorpayPaymentConfig(String orderId) {
        String restaurantId = orderService.findById(orderId).getRestaurantId();
        return getPaymentConfig(restaurantId);
    }

    public void processPayment(com.hyp.entity.Order order, Payment payment, String paymentStatus)
            throws PaymentException {
        if ("paid".equalsIgnoreCase(paymentStatus)) {
            processSuccessPayment(order, payment, paymentStatus);
        }
    }

    public void updatePayment(com.hyp.entity.Order order, Payment payment, String paymentStatus) {
        OrderStatusType status = order.getStatus();
        if (status == OrderStatusType.PAYMENT_PENDING
                || status == OrderStatusType.PAYMENT_FAILED
                || status == OrderStatusType.ERROR
                || status == OrderStatusType.PROCESSING) {
            log.info("Processing payment status={}", paymentStatus);
            orderService.updateOrderStatus(order.getId(), OrderStatusType.getOrderStatusByPaymentStatus(paymentStatus));
            payment.setStatus(paymentStatus);
            if (payment.getPaymentId() == null) {
                payment.setPaymentId(fetchPaymentId(order.getId()));
            }
            save(payment);
        }
    }

    public void processSuccessPayment(com.hyp.entity.Order order, Payment payment, String paymentStatus) {
        OrderStatusType status = order.getStatus();
        if (status == OrderStatusType.PAYMENT_PENDING
                || status == OrderStatusType.PAYMENT_FAILED
                || status == OrderStatusType.ERROR
                || status == OrderStatusType.PROCESSING) {
            log.info("Payment successful, updating to PAID");
            boolean save = orderService.updateStatus(order.getId(), OrderStatusType.PAID);
            if (!save) {
                log.error("Failed to update order status to PAID");
                metrics.count(
                        MetricsEvent.REFUND,
                        MetricTag.PARTNER,
                        Constants.RAZOR_PAY,
                        MetricTag.ACTION,
                        "process_payment",
                        MetricTag.RESULT,
                        "failed");
                return;
            }
            payment.setStatus(paymentStatus);
            if (payment.getPaymentId() == null) {
                payment.setPaymentId(fetchPaymentId(order.getId()));
            }
            save(payment);
            metrics.count(
                    MetricsEvent.REFUND,
                    MetricTag.PARTNER,
                    Constants.RAZOR_PAY,
                    MetricTag.ACTION,
                    "process_payment",
                    MetricTag.STATUS,
                    paymentStatus,
                    MetricTag.RESULT,
                    "success");
            orderEventPublisher.publishProcessOrderEvent(order);
            if (!order.isPreOrder()) {
                startOrderTrackWorkflow(order.getId());
            }
        }
    }
}
