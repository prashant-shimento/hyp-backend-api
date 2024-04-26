package com.hyp.controller;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.RazorpayEventDto;
import com.hyp.dto.RazorpayVerifyDto;
import com.hyp.dto.RefundDto;
import com.hyp.entity.Order;
import com.hyp.entity.Payment;
import com.hyp.enums.OrderStatusType;
import com.hyp.response.Response;
import com.hyp.service.OrderService;
import com.hyp.service.PaymentService;
import com.hyp.service.RazorpaySignatureVerifier;

@RestController
@RequestMapping("/payment")
public class PaymentController {

	@Autowired
	PaymentService paymentService;

	@Autowired
	OrderService orderService;

	@Autowired
	RazorpaySignatureVerifier razorPaySignatureVerifier;

	@Value("${razorpay.secret}")
	private String razorPaySecret;

	@PostMapping("{orderId}")
	public ResponseEntity<Response> createPaymentOrder(@PathVariable("orderId") String orderId) {
		Response response;
		try {
			Order order = orderService.findById(orderId);
			if (order == null) {
				throw new Exception("Order not found " + orderId);
			}
			Payment payment = paymentService.createPaymentOrder(order.getId(), order.getGrandTotalAmount());
			paymentService.save(payment);
			response = new Response(Collections.singletonList(payment), false, "Payment Order Created");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@PostMapping("/verify/{paymentId}")
	public ResponseEntity<Response> verifyPayment(@PathVariable("paymentId") String paymentId,
			@RequestBody RazorpayVerifyDto razorPayDto) {
		Response response;
		try {
			Payment payment = paymentService.findById(paymentId);
			if (payment == null) {
				throw new Exception("Payment not found " + paymentId);
			}
			if (!razorPaySignatureVerifier.verifySignature(
					payment.getOrderId() + "|" + razorPayDto.getRazorpayPaymentId(), razorPayDto.getRazorpaySignature(),
					razorPaySecret)) {
				throw new Exception("Signature Verification failed ");
			}
			payment.setSignature(razorPayDto.getRazorpaySignature());
			payment.setPaymentId(razorPayDto.getRazorpayPaymentId());
			paymentService.save(payment);
			response = new Response(Collections.singletonList(payment), false, "Payment Verified Successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@PostMapping("/refund")
	public ResponseEntity<Response> initiateRefund(@RequestBody RefundDto refundDto) {
		Response response;
		try {
			Order order = orderService.findById(refundDto.getOrderId());
			if (order == null) {
				throw new Exception("Order not found " + refundDto.getOrderId());
			}
			Payment payment = paymentService.findByOrderId(refundDto.getOrderId());
			if (payment == null) {
				throw new Exception("Payment not found " + refundDto.getOrderId());
			}

			payment = paymentService.createRefund(refundDto.getOrderId(), refundDto.getAmount(), true);
			paymentService.save(payment);
			response = new Response(Collections.singletonList(payment), false, "Refund Intiated");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@PostMapping("/webhook/razorpay")
	public ResponseEntity<String> razorPayWebHook(@RequestBody RazorpayEventDto razorPayEventDto) {
		try {
			switch (razorPayEventDto.getEvent()) {
			case "order.paid":
				handleOrderPaidEvent(razorPayEventDto);
				break;
			case "payment.captured":
				handlePaymentEvent(razorPayEventDto, OrderStatusType.PROCESSING);
				break;
			case "payment.failed":
				handlePaymentEvent(razorPayEventDto, OrderStatusType.PAYMENT_FAILED);
				break;
			case "refund.processed":
				handleRefundEvent(razorPayEventDto, OrderStatusType.REFUND_COMPLETED);
				break;
			case "refund.failed":
				handleRefundEvent(razorPayEventDto, OrderStatusType.REFUND_FAILED);
				break;
			default:
				return ResponseEntity.ok("Success");
			}
			return ResponseEntity.ok("Success");
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed");
		}
	}

	private void handleOrderPaidEvent(RazorpayEventDto razorPayEventDto) throws Exception {
		System.out.println("handleOrderPaidEvent");

		String paymentOrderId = razorPayEventDto.getPayload().getOrder().getEntity().getId();
		Payment payment = paymentService.findByPaymentOrderId(paymentOrderId);
		Order order = orderService.findById(payment.getOrderId());
		orderService.updateOrderStatus(order.getId(), OrderStatusType.PAID);
		orderService.processOrder(order);

	}

	private void handlePaymentEvent(RazorpayEventDto razorPayEventDto, OrderStatusType orderStatus) {
		System.out.println("handlePaymentEvent");

		String paymentOrderId = razorPayEventDto.getPayload().getPayment().getEntity().getOrder_id();
		String paymentId = razorPayEventDto.getPayload().getPayment().getEntity().getId();
		Payment payment = paymentService.findByPaymentOrderId(paymentOrderId);
		orderService.updateOrderStatus(payment.getOrderId(), orderStatus);
		payment.setStatus(razorPayEventDto.getPayload().getPayment().getEntity().getStatus());
		payment.setPaymentId(paymentId);
		paymentService.save(payment);
	}

	private void handleRefundEvent(RazorpayEventDto razorPayEventDto, OrderStatusType orderStatus) {
		System.out.println("handleRefundCalled");
		String paymentId = razorPayEventDto.getPayload().getRefund().getEntity().getPayment_id();
		Payment payment = paymentService.findByPaymentId(paymentId);
		orderService.updateOrderStatus(payment.getOrderId(), orderStatus);
		payment.getRefund().setStatus(razorPayEventDto.getPayload().getRefund().getEntity().getStatus());
		paymentService.save(payment);
	}

}
