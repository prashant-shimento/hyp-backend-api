package com.hyp.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

import com.hyp.constants.Constants;
import com.hyp.entity.Address;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.DeliveryOrderStatusType;
import com.hyp.enums.OrderStatusType;
import com.hyp.model.DeliveryOrderStatus;
import com.hyp.model.DeliveryOrderStatus.DeliveryFulfillment;
import com.hyp.model.DeliveryOrderStatus.DeliveryOrderData;
import com.hyp.model.DeliveryQuote;
import com.hyp.model.DeliveryRiderLocation;
import com.hyp.response.Response;
import com.hyp.service.AddressService;
import com.hyp.service.DeliveryService;
import com.hyp.service.LocationService;
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

	@Autowired
	LocationService locationService;

	@PostMapping("/callback")
	public ResponseEntity<Response> updateDeliveryOrderStatus(@RequestBody DeliveryOrderStatus deliveryOrderStatus) {
		Response response;
		try {
			DeliveryOrderData deliveryOrderData = deliveryOrderStatus.getData();
			Delivery delivery = deliveryService.findByDeliveryOrderId(deliveryOrderData.getId());
			if (delivery == null) {
				response = new Response(null, true, "Delivery Id not found " + deliveryOrderData.getId());
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			delivery.setStatus(DeliveryOrderStatusType.getDeliveryOrderStatus(deliveryOrderData.getStatus()));
			if (delivery.getStatus() == DeliveryOrderStatusType.FULFILLED
					|| delivery.getStatus() == DeliveryOrderStatusType.COMPLETED) {
				DeliveryFulfillment deliveryFulfill = deliveryOrderData.getFulfillment();
				DeliveryFulfillStatusType fullFillStatus = deliveryFulfill.getStatus();
				delivery.setFulfillment(deliveryFulfill);
				Order order = orderService.findById(delivery.getOrderId());
				orderService.updateOrderStatus(order.getId(),
						OrderStatusType.getOrderStatusByDelvieryStatus(fullFillStatus));
				if (deliveryOrderData.getFulfillment().getTrackCode() != null
						&& order.getDeliveryTrackingLink() == null) {
					delivery.getFulfillment().setTrackCode(deliveryOrderData.getFulfillment().getTrackCode());
					order.setDeliveryTrackingLink("https://t.pidge.in?t=" + delivery.getFulfillment().getTrackCode());
					orderService.save(order);
				}
				if (deliveryService.isPosUpdateRequired(fullFillStatus)) {
					deliveryService.updatePosRiderStatus(delivery, order);
				}
				deliveryService.save(delivery);
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

			if (!locationService.isLocationDeliverable(address.getLocation().getLatitude(),
					address.getLocation().getLongitude(), restaurant.getLocation().getLatitude(),
					restaurant.getLocation().getLongitude(), restaurant.getDeliveryRadius())) {
				response = new Response(null, true, "Location not Deliverable");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}

			DeliveryQuote deliveryQuote = deliveryService
					.getDeliveryQuote(DeliveryRequestTranslation.getQuoteRequest(restaurant, address));

			List<DeliveryQuote.DeliveryNetworks> filteredQuotes = deliveryQuote.getData().getItems().stream()
					.filter(item -> item.isPickupNow())
					.sorted(Comparator.comparingDouble(item -> item.getQuote().getPrice()))
					.collect(Collectors.toList());

			if (filteredQuotes.size() == 1) {
				response = new Response(Collections.singletonList(filteredQuotes.get(0)), false,
						"Only one pickupNow quote available");
			} else {
				response = new Response(Collections.singletonList(filteredQuotes.get(1)), false,
						"Delivery Quotes Fetched");
			}

			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@Hidden
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

	@PostMapping("/fulfill/{orderId}")
	public ResponseEntity<Response> smartFulfill(@PathVariable String orderId,
			@RequestParam(defaultValue = "", required = false) String fulfillType) {
		Response response;
		try {
			Order order = orderService.findById(orderId);
			if (order == null) {
				response = new Response(null, true, "Order not found " + orderId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			Delivery delivery = deliveryService.findByOrderId(order.getId());

			if (order.getStatus().equals(OrderStatusType.ERROR)
					|| order.getStatus().equals(OrderStatusType.DELIVERY_ERROR)) {
				if (fulfillType.equalsIgnoreCase("smart")) {
					orderService.processDeliverySmartFulfill(delivery, Constants.SMART);
				} else {
					orderService.processDeliveryFulfill(delivery, Constants.API);
				}
				orderService.updateOrderStatus(orderId, OrderStatusType.READY_FOR_DELIVERY);
			}

			response = new Response(Collections.singletonList(delivery), false, "Delivery Fullfilled");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
