package com.hyp.controller;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;

import com.hyp.enums.*;
import com.hyp.exception.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hyp.constants.Constants;
import com.hyp.constants.ErrorConstants;
import com.hyp.dto.OrderDto;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest.RiderDetails;
import com.hyp.response.Response;
import com.hyp.service.DeliveryService;
import com.hyp.service.OrderService;
import com.hyp.service.PaymentService;
import com.hyp.service.PosService;
import com.hyp.service.RedisService;
import com.hyp.service.RestaurantService;
import com.hyp.translation.OrderTranslation;
import com.hyp.translation.PosOrderRequestTranslation;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController extends BaseListController<OrderDto, Order, String> {

	@Autowired
	OrderService orderService;

	@Autowired
	OrderTranslation orderTranslation;

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	PosService posService;

	@Autowired
	DeliveryService deliveryService;

	@Autowired
	PaymentService paymentService;
	
	@Autowired
	RedisService redisService;

	@Autowired
	PosOrderRequestTranslation posOrderRequestTranslation;

	@PostMapping()
	public ResponseEntity<Response> create(@RequestBody @Valid OrderDto orderDto) {
		Response response;
		try {
			Order createdOrder = orderService.create(orderDto);
			OrderDto createdOrderDto = orderTranslation.getDto(createdOrder);
			response = new Response(Collections.singletonList(createdOrderDto), false, "Order Created");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Exception occurred in create order {}",e.getMessage());
			response = new Response(null, true, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@PatchMapping("/{orderId}")
	public ResponseEntity<Response> update(@PathVariable String orderId, @RequestBody OrderDto orderDto) {
		Response response;
		try {
			Order order = orderService.findById(orderId);
			if (order == null) {
				response = new Response(null, true, "Order not found " + orderId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			OrderStatusType orderStatus = order.getStatus();
			orderTranslation.updateEntityFromDto(orderDto, order);
			Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
			if(OrderStatusType.ACCEPTED.name().equalsIgnoreCase(orderDto.getStatus())
					&& restaurant.getPosPartner().equalsIgnoreCase(PosPartner.SELF.name())) {
				String fulFill = redisService.getRedisData(Constants.REDIS_KEY_FULFILL).orElse("smart");
				Delivery delivery = deliveryService.findByOrderId(order.getId());
				if (delivery != null && delivery.getStatus().equals(DeliveryOrderStatusType.PENDING)) {
					if (fulFill.equalsIgnoreCase("smart")) {
						deliveryService.processDeliverySmartFulfill(delivery, Constants.PET_POOJA);
					} else {
						deliveryService.processDeliveryStandardFulfill(delivery, Constants.PET_POOJA);
					}
				}
			}
			if (OrderStatusType.DELIVERED.name().equalsIgnoreCase(orderDto.getStatus())) {
				orderService.updateOrderStatus(orderId, OrderStatusType.DELIVERED);
				posService.updatePosRiderStatus(deliveryService.findByOrderId(orderId), order);
			}
			if (OrderStatusType.CANCELLED.name().equalsIgnoreCase(orderDto.getStatus())) {
				if (Constants.CANCELABLE_STATUSES.contains(orderStatus)) {
					if(!restaurant.getPosPartner().equalsIgnoreCase(PosPartner.SELF.name())){
						PosOrderUpdateRequest posOrderUpdateRequest = posOrderRequestTranslation
								.getPosOrderUpdateRequest(restaurant, order, "Cancellation");
						posService.updatePosOrder(posOrderUpdateRequest);
					}
					if (OrderType.fromCode(order.getOrderType()) == OrderType.H) {
						Delivery delivery = deliveryService.findByOrderId(orderId);
						deliveryService.cancelDeliveryOrder(delivery.getDeliveryOrderId());
					}
					paymentService.createRefund(order.getId(), order.getGrandTotalAmount(), restaurant.isInstantRefund(), "Cancellation");
				}
				orderService.updateOrderStatus(orderId, OrderStatusType.CANCELLED);
			}
			order = orderService.save(order);
			orderService.updateOrderStatus(orderId, order.getStatus());
			response = new Response(Collections.singletonList(orderTranslation.getDto(order)), false, "Order Updated");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Exception occurred in update {}", e.getMessage());
			response = new Response(null, true, ErrorConstants.INTERNAL_SERVER_ERROR);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@Hidden
	@PostMapping("/rider/{orderId}")
	public ResponseEntity<Response> orderRiderUpdate(@PathVariable String orderId) {
		Response response;
		try {
			Order order = orderService.findById(orderId);
			Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
			PosRiderUpdateRequest posRiderUpdateRequest = posOrderRequestTranslation.getPosRiderStatusUpdateRequest(
					restaurant, order, new RiderDetails("rider", "9964552656"), RiderStatusType.rider_assigned);
			String posResponse = posService.updatePosRiderStatus(posRiderUpdateRequest);
			response = new Response(Collections.singletonList(posResponse), false, "Rider Status Updated");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Exception occurred in orderRiderUpdate {}", e.getMessage());
			response = new Response(null, true, ErrorConstants.INTERNAL_SERVER_ERROR);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/track/{orderId}")
	public ResponseEntity<Void> orderTracking(@PathVariable String orderId) throws EntityNotFoundException {
		Order order = Optional.ofNullable(orderService.findById(orderId))
				.orElseThrow(() -> new EntityNotFoundException("Order", orderId));

		Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
				.orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));

		String trackingUrl = "https://t.pidge.in?t="+delivery.getFulfillment().getTrackCode();
		if (trackingUrl.isBlank()) {
			throw new EntityNotFoundException("Order Tracking Link", orderId);
		}
		return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(trackingUrl)).build();
	}
}
