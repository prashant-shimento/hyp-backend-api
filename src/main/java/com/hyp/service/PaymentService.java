package com.hyp.service;

import java.util.Date;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.hyp.constants.Constants;
import com.hyp.dto.RazorpayVerifyDto;
import com.hyp.entity.Payment;
import com.hyp.enums.OrderStatusType;
import com.hyp.repository.PaymentRepository;
import com.hyp.util.CommonUtils;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import com.razorpay.Utils;

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

	public Payment createPaymentOrder(String orderId, double amount) {
		try {
			RazorpayClient razorpayClient = new RazorpayClient(razorPayKey, razorPaySecret);
			JSONObject orderRequest = new JSONObject();
			orderRequest.put("amount", CommonUtils.getISOAmount(amount));
			orderRequest.put("currency", "INR");
			orderRequest.put("receipt", CommonUtils.genId());
			Order order = razorpayClient.orders.create(orderRequest);

			Payment payment = new Payment();
			payment.setId(CommonUtils.genId());
			payment.setPaymentOrderId(order.get("id"));
			payment.setAmount(Double.parseDouble(order.get("amount").toString()));
			payment.setReceipt(order.get("receipt"));
			payment.setStatus(order.get("status"));
			payment.setCurrency(order.get("currency"));
			payment.setProvider(Constants.RAZOR_PAY);
			payment.setOrderId(orderId);
			orderService.updateOrderStatus(orderId, OrderStatusType.PAYMENT_PENDING);
			return payment;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error creating payment order: " + e.getMessage(), e);
		}
	}

	public boolean verifySignature(RazorpayVerifyDto razorPayVerifyDto) {
		try {
			JSONObject verifyRequest = new JSONObject();
			verifyRequest.put("razorpay_order_id", razorPayVerifyDto.getRazorpayOrderId());
			verifyRequest.put("razorpay_payment_id", razorPayVerifyDto.getRazorpayPaymentId());
			verifyRequest.put("razorpay_signature", razorPayVerifyDto.getRazorpaySignature());
			return Utils.verifyPaymentSignature(verifyRequest, razorPaySecret);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error in verifySignature: " + e.getMessage(), e);
		}
	}

	public String fetchOrderStatus(String orderId) {
		try {
			RazorpayClient razorpayClient = new RazorpayClient(razorPayKey, razorPaySecret);
			Order order = razorpayClient.orders.fetch(orderId);
			return order.get("status");
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error fetchOrderStatus: " + e.getMessage(), e);
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

	public Payment createRefund(String orderId, double amount, boolean instantRefund) {
		try {
			Payment payment = paymentRepository.findByOrderId(orderId);

			RazorpayClient razorpayClient = new RazorpayClient(razorPayKey, razorPaySecret);
			JSONObject refundRequest = new JSONObject();
			refundRequest.put("amount", CommonUtils.getISOAmount(amount));
			if (instantRefund) {
				refundRequest.put("speeed", "optimum");
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
			payment.setRefund(paymentRefund);

			this.save(payment);
			orderService.updateOrderStatus(orderId, OrderStatusType.REFUND_INITIATED);
			return payment;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error creating payment order: " + e.getMessage(), e);
		}
	}

}
