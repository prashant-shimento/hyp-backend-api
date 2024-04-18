package com.hyp.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.entity.Address;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.DeliveryOrderStatusType;
import com.hyp.enums.OrderStatusType;
import com.hyp.model.DeliveryOrderStatus;
import com.hyp.model.DeliveryOrderStatus.DeliveryFulfillment;
import com.hyp.model.DeliveryQuote;
import com.hyp.model.DeliveryRiderLocation;
import com.hyp.response.Response;
import com.hyp.service.AddressService;
import com.hyp.service.DeliveryService;
import com.hyp.service.OrderService;
import com.hyp.service.PosService;
import com.hyp.service.RestaurantService;
import com.hyp.translation.DeliveryRequestTranslation;

import io.swagger.v3.oas.annotations.Hidden;

@RestController
@RequestMapping("/delivery")
public class DeliveryController {

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	DeliveryService deliveryService;

	@Autowired
	AddressService addressService;

	@Autowired
	PosService posService;

	@Autowired
	OrderService orderService;

	@PostMapping("/callback")
	public ResponseEntity<Response> updateDeliveryOrderStatus(@RequestBody DeliveryOrderStatus deliveryOrderStatus) {
		Response response;
		try {
			Delivery delivery = deliveryService.findByDeliveryOrderId(deliveryOrderStatus.getId());
			if (delivery == null) {
				response = new Response(null, true, "Delivery Id not found " + deliveryOrderStatus.getId());
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			DeliveryFulfillment deliveryFulfill = deliveryOrderStatus.getFulfillment();
			DeliveryFulfillStatusType fullFillStatus = deliveryFulfill.getStatus();

			delivery.setStatus(DeliveryOrderStatusType.getDeliveryOrderStatus(deliveryOrderStatus.getStatus()));

			if (delivery.getStatus() == DeliveryOrderStatusType.FULFILLED && deliveryFulfill != null) {
				delivery.setFulfillment(deliveryFulfill);
			}
			deliveryService.save(delivery);

			Order order = orderService.findById(delivery.getOrderId());

			if (delivery.getStatus() == DeliveryOrderStatusType.FULFILLED && deliveryFulfill != null) {
				orderService.updateOrderStatus(order.getId(),
						OrderStatusType.getOrderStatusByDelvieryStatus(fullFillStatus));
			}

			if (delivery.getStatus() == DeliveryOrderStatusType.FULFILLED
					&& deliveryService.isPosUpdateRequired(fullFillStatus)) {
				deliveryService.updatePosRiderStatus(delivery,order);
			}

			response = new Response(null, false, "Success");
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/quote/{restaurantId}")
	public ResponseEntity<Response> getDeliveryQuote(@PathVariable("restaurantId") String restaurantId,
			@RequestParam("addressId") String addressId) {
		Response response;
		try {
			Restaurant restaurant = restaurantService.findById(restaurantId);
			if (restaurant == null) {
				response = new Response(null, true, "Restaurant not found " + restaurantId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			Address address = addressService.findById(addressId);
			if (address == null) {
				response = new Response(null, true, "Address not found " + addressId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			DeliveryQuote deliveryQuote = deliveryService
					.getDeliveryQuote(DeliveryRequestTranslation.getQuoteRequest(restaurant, address));

			Optional<DeliveryQuote.DeliveryNetworks> deliveryHighestQuote = deliveryQuote.getData().getItems().stream()
					.max(Comparator.comparingDouble(item -> item.getQuote().getPrice()));

			response = new Response(Collections.singletonList(deliveryHighestQuote), false, "Delivery Quotes Fetched");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/rider-location/{orderId}")
	public ResponseEntity<Response> getRiderLocation(@PathVariable String orderId) {
		Response response;
		try {
			Order order = orderService.findById(orderId);
			if (order == null) {
				response = new Response(null, true, "Order not found " + orderId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

			DeliveryRiderLocation deliveryRiderLocation = deliveryService.getRiderCurrentLocation(orderId);

			response = new Response(Collections.singletonList(deliveryRiderLocation), false, "Delivery Quotes Fetched");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

    @Hidden
	@PostMapping("/eligibility/{restaurantId}")
	public ResponseEntity<Response> checkDelivery(@PathVariable("restaurantId") String restaurantId,
			@RequestParam("latitude") double latitude, @RequestParam("longitude") double longitude) {
		Response response;
		try {
			Restaurant restaurant = restaurantService.findById(restaurantId);
			if (restaurantService == null) {
				response = new Response(null, true, "Restaurant not found " + restaurantId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			if (!deliveryService.isLocationDeliverable(latitude, longitude, restaurant.getLocation().getLatitude(),
					restaurant.getLocation().getLatitude())) {
				response = new Response(null, false, "Location Not Deliverable");
				return ResponseEntity.badRequest().body(response);
			}
			response = new Response(null, false, "Location Deliverable");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
