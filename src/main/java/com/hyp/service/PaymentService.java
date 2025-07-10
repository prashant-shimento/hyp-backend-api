package com.hyp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import com.hyp.dto.RefundDto;
import com.hyp.enums.FeeType;
import com.hyp.event.OrderEventPublisher;
import com.hyp.exception.PaymentException;
import com.hyp.model.PaymentRoute;
import com.hyp.temporal.service.OrderWorkflowService;
import com.razorpay.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.hyp.constants.Constants;
import com.hyp.dto.RazorpayVerifyDto;
import com.hyp.entity.Partner;
import com.hyp.entity.Payment;
import com.hyp.entity.Restaurant;
import com.hyp.enums.OrderStatusType;
import com.hyp.model.PaymentConfig;
import com.hyp.enums.RefundType;
import com.hyp.repository.PaymentRepository;
import com.hyp.util.CommonUtils;
import com.hyp.util.EncryptionUtils;

import lombok.extern.slf4j.Slf4j;

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
	OrderWorkflowService orderWorkflowService;

	@Autowired
	private OrderEventPublisher orderEventPublisher;

	public Payment createPaymentOrder(String orderId, double amount) throws PaymentException {
		try {
			com.hyp.entity.Order order = orderService.findById(orderId);
			if (order == null || !OrderStatusType.CREATED.equals(order.getStatus())) {
				throw new IllegalStateException("Order is in Invalid Status: " + (order != null ? order.getStatus() : "null"));
			}

			Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
			RazorpayClient razorpayClient = getRazorpayClient(orderId);

			Payment payment;
			if (restaurant.isPaymentRoutingEnabled()) {
				try {
					payment = createRoutingOrder(razorpayClient, orderId, amount, restaurant);
				} catch (Exception routingEx) {
					log.warn("Routing failed for order {}: {}. Falling back to standard order.", orderId, routingEx.getMessage());
					payment = createStandardOrder(razorpayClient, orderId, amount, restaurant);
				}
			} else {
				payment = createStandardOrder(razorpayClient, orderId, amount, restaurant);
			}
			setPaymentCheck(orderId);
			return save(payment);
		} catch (Exception e) {
			log.error("Error creating payment order {}", e.getMessage());
			throw new PaymentException("Error creating payment order: " + e.getMessage(), e);
		}
	}

	private Payment createRoutingOrder(RazorpayClient razorpayClient, String orderId, double amount, Restaurant restaurant) throws RazorpayException {
		JSONObject orderRequest = new JSONObject();
		orderRequest.put("amount", CommonUtils.getISOAmount(amount));
		orderRequest.put("currency", "INR");
		orderRequest.put("receipt", CommonUtils.genId());
		JSONArray transfers = new JSONArray();
		double totalTransferAmount = 0.0;

		for (PaymentRoute route : Optional.ofNullable(restaurant.getPaymentRouteList()).orElse(Collections.emptyList())) {
			double transferAmount = calculateRoutingAmount(amount, restaurant);

			if (transferAmount <= 0 || totalTransferAmount + transferAmount > amount) {
				log.warn("Transfer amount {} for route {} is invalid or exceeds remaining order amount. Skipping...",
						transferAmount, route.getRecipientId());
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
				log.warn("Error fetching account for {}: {}. Skipping this route.", route.getRecipientId(), ex.getMessage());
			}
		}

		if (transfers.isEmpty()) {
			throw new RazorpayException("No valid payment routes found. Cannot create routing order.");
		}

		orderRequest.put("transfers", transfers);
		Order paymentOrder = razorpayClient.orders.create(orderRequest);
		return buildPaymentFromOrder(paymentOrder, orderId);
	}

	private Payment createStandardOrder(RazorpayClient razorpayClient, String orderId, double amount, Restaurant restaurant) throws RazorpayException {
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

	private void updateOrderStatusInRedis(String orderId) {
		String redisStateKey = "order:" + orderId + ":state";
		String redisPaymentKey = "order:" + orderId + ":payment";
		long stateTtl = Duration.ofMinutes(15).toSeconds();
		long paymentTtl = Duration.ofMinutes(4).toSeconds();

		redisService.setRedisData(redisStateKey, OrderStatusType.PAYMENT_PENDING, stateTtl);
		redisService.setRedisData(redisPaymentKey, OrderStatusType.PAYMENT_PENDING, paymentTtl);
	}

	private void setPaymentCheck(String orderId) {
		boolean isWorkflowEnabled = redisService.getRedisData(Constants.PAYMENT_WORKFLOW_ENABLED)
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
		return 	BigDecimal.valueOf(total)
				.setScale(2, RoundingMode.HALF_UP)
				.doubleValue();
	}

	public double calculatePlatformFee(double orderAmount, Restaurant restaurant) {
		return Optional.ofNullable(restaurant.getPlatformFee())
				.orElse(Collections.emptyList())
				.stream()
				.filter(rule ->
						(rule.getMinOrderAmount() == null || orderAmount >= rule.getMinOrderAmount()) &&
								(rule.getMaxOrderAmount() == null || orderAmount <= rule.getMaxOrderAmount())
				)
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
			JSONObject verifyRequest = new JSONObject();
			verifyRequest.put("razorpay_order_id", razorPayVerifyDto.getRazorpayOrderId());
			verifyRequest.put("razorpay_payment_id", razorPayVerifyDto.getRazorpayPaymentId());
			verifyRequest.put("razorpay_signature", razorPayVerifyDto.getRazorpaySignature());
			return Utils.verifyPaymentSignature(verifyRequest, EncryptionUtils.decrypt(getRazorpayPaymentConfig(orderId).getSecret()));
		} catch (Exception e) {
			log.error("Error in verifySignature {}", e.getMessage());
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
			log.error("Error in fetchOrderStatus {}", e.getMessage());
			throw new PaymentException("Error fetchOrderStatus: " + e.getMessage(), e);
		}
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

	public Payment createRefund(String orderId, double amount, boolean instantRefund, String reason) throws PaymentException {
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
			orderService.updateOrderStatus(orderId,
					OrderStatusType.getOrderStatusByRefundStatus(paymentRefund.getStatus()));
			return payment;
		} catch (Exception e) {
			log.error("Error in creating refund order {}", e.getMessage());
			throw new PaymentException("Error creating refund order: " + e.getMessage(), e);
		}
	}

	public RefundDto fetchRefund(String orderId) throws PaymentException {
		try {
			Payment payment = paymentRepository.findByOrderId(orderId);

			RazorpayClient razorpayClient = getRazorpayClient(orderId);

			Refund refund = razorpayClient.payments.fetchRefund(payment.getPaymentId(), payment.getRefund().getId());
			return RefundDto.builder().id(refund.get("id"))
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
			log.error("Error in fetchRefund order {}", e.getMessage());
			throw new PaymentException("Error fetchRefund refund order: " + e.getMessage(), e);
		}
	}

	@Cacheable(value = "paymentConfigCache", key = "#restaurant.id")
	private PaymentConfig getPaymentConfig(Restaurant restaurant) {
		return Optional.ofNullable(restaurant.getPaymentPartner())
				.map(partnerService::findById)
				.map(Partner::getApiConfigs)
				.map(apiConfig -> PaymentConfig.builder()
						.key(apiConfig.getKey())
						.secret(apiConfig.getSecret())
						.build())
				.orElseGet(() -> {
					log.warn("Falling back to default payment config for restaurant: {}", restaurant.getId());
					return PaymentConfig.builder()
							.key(razorPayKey)
							.secret(razorPaySecret)
							.build();
				});
	}

	private RazorpayClient getRazorpayClient(String orderId) throws PaymentException {
		try {
			Restaurant restaurant = restaurantService.findById(orderService.findById(orderId).getRestaurantId());
			PaymentConfig paymentConfig = getPaymentConfig(restaurant);
			return new RazorpayClient(
					EncryptionUtils.decrypt(paymentConfig.getKey()),
					EncryptionUtils.decrypt(paymentConfig.getSecret())
			);
		} catch (Exception e) {
			throw new PaymentException("Failed to initialize RazorpayClient", e);
		}
	}

	private PaymentConfig getRazorpayPaymentConfig(String orderId) {
		Restaurant restaurant = restaurantService.findById(orderService.findById(orderId).getRestaurantId());
		return getPaymentConfig(restaurant);
	}

	public void verifyPayment(com.hyp.entity.Order order, Payment payment, String paymentStatus) throws PaymentException {
		if ("paid".equalsIgnoreCase(paymentStatus)) {
			processSuccessPayment(order, payment, paymentStatus);
		}
	}

	public void processPayment(com.hyp.entity.Order order, Payment payment, String paymentStatus) {
		OrderStatusType status = order.getStatus();
		if (status == OrderStatusType.PAYMENT_PENDING ||
				status == OrderStatusType.PAYMENT_FAILED ||
				status == OrderStatusType.ERROR ||
				status == OrderStatusType.PROCESSING) {
			log.info("Processing order {}", order.getId());
			orderService.updateOrderStatus(order.getId(), OrderStatusType.getOrderStatusByPaymentStatus(paymentStatus));
			payment.setStatus(paymentStatus);
			save(payment);
		}
	}

	public void processSuccessPayment(com.hyp.entity.Order order, Payment payment, String paymentStatus) {
		OrderStatusType status = order.getStatus();
		if (status == OrderStatusType.PAYMENT_PENDING ||
				status == OrderStatusType.PAYMENT_FAILED ||
				status == OrderStatusType.ERROR ||
				status == OrderStatusType.PROCESSING) {
			log.info("Updating order {} to PAID", order.getId());
			boolean save = orderService.updateStatus(order.getId(), OrderStatusType.PAID);
			if(!save){
				log.error("Unable to update the status of order for ID {}", order.getId());
			}
			payment.setStatus(paymentStatus);
			save(payment);
			orderEventPublisher.publishProcessOrderEvent(order);
		}
	}
}
