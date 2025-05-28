package com.hyp.controller;

import java.util.Collections;
import java.util.List;

import com.hyp.exception.PaymentException;
import com.razorpay.Refund;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hyp.constants.ErrorConstants;
import com.hyp.dto.PaymentDto;
import com.hyp.dto.RazorpayEventDto;
import com.hyp.dto.RazorpayVerifyDto;
import com.hyp.dto.RefundDto;
import com.hyp.entity.Order;
import com.hyp.entity.Payment;
import com.hyp.entity.Restaurant;
import com.hyp.enums.OrderStatusType;
import com.hyp.event.OrderEventPublisher;
import com.hyp.response.Response;
import com.hyp.service.OrderService;
import com.hyp.service.PaymentService;
import com.hyp.service.RestaurantService;
import com.hyp.translation.PaymentTranslation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/payment")
public class PaymentController extends BaseListController<PaymentDto, Payment, String> {

	@Autowired
	PaymentService paymentService;

	@Autowired
	PaymentTranslation paymentTranslation;

	@Autowired
	OrderService orderService;
	
	@Autowired
	RestaurantService restaurantService;

	@Autowired
	private OrderEventPublisher orderEventPublisher;

	@PostMapping("/{orderId}")
	public ResponseEntity<Response> createPaymentOrder(@PathVariable String orderId) throws Exception {
		Order order = orderService.findById(orderId);
		if (order == null) {
			throw new Exception("Order not found " + orderId);
		}
		Payment payment = paymentService.createPaymentOrder(order.getId(), order.getGrandTotalAmount());
		return ResponseEntity.ok(Response.builder().data(Collections.singletonList(payment)).error(false).message("Payment Order Created").build());
	}

	@PostMapping("/consume/{orderId}")
	public ResponseEntity<Response> consumePayment(@PathVariable String orderId) throws PaymentException {
		Payment payment = paymentService.findByOrderId(orderId);
		if (payment == null) {
			throw new PaymentException("Payment not found " + orderId);
		}
		Order order = orderService.findById(orderId);
		processPaymentStatus(order, payment);
		return ResponseEntity.ok(Response.builder().data(Collections.singletonList(order)).error(false).message("Order Payment Consumed Successfully").build());
	}

	@PostMapping("/verify/{orderId}")
	public ResponseEntity<Response> verifyPayment(@PathVariable String orderId,
			@RequestBody RazorpayVerifyDto razorPayDto) throws PaymentException {
		Payment payment = paymentService.findByOrderId(orderId);
		if (payment == null) {
			throw new PaymentException("Payment not found " + orderId);
		}
		Order order = orderService.findById(orderId);
		if (paymentService.verifySignature(razorPayDto, orderId)) {
			payment.setPaymentId(razorPayDto.getRazorpayPaymentId());
			payment.setSignature(razorPayDto.getRazorpaySignature());
			processPaymentStatus(order, payment);
		}
		return ResponseEntity.ok(Response.builder().data(Collections.singletonList(order)).error(false).message("Payment Verified Successfully").build());

	}

	@PostMapping("/refund")
	public ResponseEntity<Response> initiateRefund(@RequestBody RefundDto refundDto) throws PaymentException {
		Order order = orderService.findById(refundDto.getOrderId());
		if (order == null) {
			throw new PaymentException("Order not found " + refundDto.getOrderId());
		}
		Payment payment = paymentService.findByOrderId(refundDto.getOrderId());
		if (payment == null) {
			throw new PaymentException("Payment not found " + refundDto.getOrderId());
		}
		if (order.getStatus().equals(OrderStatusType.REFUND_COMPLETED)
				|| order.getStatus().equals(OrderStatusType.REFUND_INITIATED)) {
			throw new PaymentException("Refund Already " + order.getStatus());
		}
		Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
		payment = paymentService.createRefund(refundDto.getOrderId(), refundDto.getAmount(), restaurant.isInstantRefund());
		paymentService.save(payment);
		return ResponseEntity.ok(Response.builder().data(Collections.singletonList(payment)).error(false).message("Refund Initiated").build());
	}

	@GetMapping("/refund/{orderId}")
	public ResponseEntity<Response> getRefundDetails(@PathVariable String orderId) throws PaymentException {
		Order order = orderService.findById(orderId);
		if (order == null) {
			throw new PaymentException("Order not found " + orderId);
		}
		Payment payment = paymentService.findByOrderId(orderId);
		if (payment == null) {
			throw new PaymentException("Payment not found " + orderId);
		}
		if(payment.getRefund() == null){
			throw new PaymentException("Refund not found " + orderId);
		}
		return ResponseEntity.ok(Response.builder().data(Collections.singletonList(paymentService.fetchRefund(orderId))).error(false).message("Refund Fetched").build());
	}

	@PostMapping("/callback")
	public ResponseEntity<Response> paymentCallback(@RequestBody RazorpayEventDto razorPayEventDto) {
		Response response;


		try {
			switch (razorPayEventDto.getEvent()) {
			case "order.paid":
				handleOrderEvent(razorPayEventDto);
				break;
                case "payment.failed" , "payment.captured":
				handlePaymentEvent(razorPayEventDto);
				break;
			case "refund.processed":
				handleRefundEvent(razorPayEventDto, OrderStatusType.REFUND_COMPLETED);
				break;
			case "refund.failed":
				handleRefundEvent(razorPayEventDto, OrderStatusType.REFUND_FAILED);
				break;
			default:
				return ResponseEntity.ok(new Response(null, false, "Success"));
			}
			return ResponseEntity.ok(new Response(null, false, "Success"));
		} catch (Exception e) {
			log.error("Exception occurred in razorPayWebHook {}",e.getMessage());
			response = new Response(null, true, ErrorConstants.INTERNAL_SERVER_ERROR);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	private void handleOrderEvent(RazorpayEventDto razorPayEventDto) {
		String paymentOrderId = razorPayEventDto.getPayload().getOrder().getEntity().getId();
		Payment payment = paymentService.findByPaymentOrderId(paymentOrderId);
		Order order = orderService.findById(payment.getOrderId());
		log.info("payment paid event processing for order {}",order.getId());
		if (order.getStatus().equals(OrderStatusType.PAYMENT_PENDING)
				|| order.getStatus().equals(OrderStatusType.PROCESSING)) {
			orderService.updateOrderStatus(order.getId(), OrderStatusType.PAID);
			payment.setStatus("paid");
			paymentService.save(payment);
			orderEventPublisher.publishProcessOrderEvent(order);
		}

	}

	private void handlePaymentEvent(RazorpayEventDto razorPayEventDto) {
		String paymentOrderId = razorPayEventDto.getPayload().getPayment().getEntity().getOrder_id();
		String paymentId = razorPayEventDto.getPayload().getPayment().getEntity().getId();
		Payment payment = paymentService.findByPaymentOrderId(paymentOrderId);
		log.info("payment capture event processing for order {}",payment.getOrderId());
		payment.setStatus(razorPayEventDto.getPayload().getPayment().getEntity().getStatus());
		payment.setPaymentId(paymentId);
		paymentService.save(payment);
	}

	private void handleRefundEvent(RazorpayEventDto razorPayEventDto, OrderStatusType orderStatus) {
		String paymentId = razorPayEventDto.getPayload().getRefund().getEntity().getPayment_id();
		Payment payment = paymentService.findByPaymentId(paymentId);
		log.info("payment refund event processing for order {}",payment.getOrderId());
		orderService.updateOrderStatus(payment.getOrderId(), orderStatus);
		payment.getRefund().setStatus(razorPayEventDto.getPayload().getRefund().getEntity().getStatus());
		paymentService.save(payment);
	}

	private void processPaymentStatus(Order order, Payment payment) throws PaymentException {
		String paymentStatus = paymentService.fetchOrderStatus(order.getId());
		if (paymentStatus.equalsIgnoreCase("captured") || paymentStatus.equalsIgnoreCase("paid")) {
			if (order.getStatus().equals(OrderStatusType.PAYMENT_PENDING)
					|| order.getStatus().equals(OrderStatusType.PAYMENT_FAILED)
					|| order.getStatus().equals(OrderStatusType.ERROR)
					|| order.getStatus().equals(OrderStatusType.PROCESSING)) {
				orderService.updateOrderStatus(order.getId(), OrderStatusType.PAID);
				payment.setStatus(paymentStatus);
				paymentService.save(payment);
				orderEventPublisher.publishProcessOrderEvent(order);
			}
		}
	}

}
