package com.hyp.controller;

import java.util.Collections;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.RazorpayEventDto;
import com.hyp.dto.RazorpayVerifyDto;
import com.hyp.entity.Order;
import com.hyp.entity.Payment;
import com.hyp.enums.OrderStatusType;
import com.hyp.enums.OrderType;
import com.hyp.response.ResponseTemplate;
import com.hyp.service.OrderService;
import com.hyp.service.PaymentService;
import com.hyp.service.RazorpaySignatureVerifier;

@RestController
@RequestMapping("/api/payment")
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
	public ResponseEntity<ResponseTemplate> createPaymentOrder(@PathVariable("orderId") String orderId) {
		ResponseTemplate response;
		try {
			Order order = orderService.findById(orderId);
			if(order == null) {
				throw new Exception("Order not found " + orderId);
			}
			Payment payment = paymentService.createPaymentOrder(order.getId(), order.getTotalAmount());
			paymentService.save(payment);
			response = new ResponseTemplate(Collections.singletonList(payment), false,
					"Payment Order Created");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new ResponseTemplate(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
	
	@PostMapping("/verify/{paymentId}")
	public ResponseEntity<ResponseTemplate> verifyPayment(@PathVariable("paymentId") String paymentId, @RequestBody RazorpayVerifyDto razorPayDto) {
		ResponseTemplate response;
		try {
			Payment payment = paymentService.findById(paymentId);
			if(payment == null) {
				throw new Exception("Payment not found " + paymentId);
			}
			if(!razorPaySignatureVerifier.verifySignature(payment.getOrderId() + "|" + razorPayDto.getRazorpayPaymentId(), razorPayDto.getRazorpaySignature()
					, razorPaySecret)) {
				throw new Exception("Signature Verification failed ");
			}
			payment.setSignature(razorPayDto.getRazorpaySignature());
			payment.setPaymentId(razorPayDto.getRazorpayPaymentId());
			paymentService.save(payment);
			response = new ResponseTemplate(Collections.singletonList(payment), false,
					"Payment Verified Successfully");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new ResponseTemplate(null, true, e.getMessage());
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
	                handlePaymentCapturedEvent(razorPayEventDto);
	                break;
	            case "payment.failed":
	                handlePaymentFailedEvent(razorPayEventDto);
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

	private void handleOrderPaidEvent(RazorpayEventDto razorPayEventDto) {
	    String paymentOrderId = razorPayEventDto.getPayload().getOrder().getEntity().getId();
	    Payment payment = paymentService.findByPaymentOrderId(paymentOrderId);
	    Order order = orderService.findById(payment.getOrderId());
	    order.setStatus(OrderStatusType.PAID);
	    orderService.save(order);
	}

	private void handlePaymentCapturedEvent(RazorpayEventDto razorPayEventDto) {
	    String paymentOrderId = razorPayEventDto.getPayload().getPayment().getEntity().getOrder_id();
	    Payment payment = paymentService.findByPaymentOrderId(paymentOrderId);
	    payment.setStatus(razorPayEventDto.getPayload().getPayment().getEntity().getStatus());
	    paymentService.save(payment);
	}

	private void handlePaymentFailedEvent(RazorpayEventDto razorPayEventDto) {
	    String paymentOrderId = razorPayEventDto.getPayload().getPayment().getEntity().getOrder_id();
	    Payment payment = paymentService.findByPaymentOrderId(paymentOrderId);
	    Order order = orderService.findById(payment.getOrderId());
	    order.setStatus(OrderStatusType.PAYMENT_FAILED);
	    orderService.save(order);
	    payment.setStatus(razorPayEventDto.getPayload().getPayment().getEntity().getStatus());
	    paymentService.save(payment);
	}

	

}
