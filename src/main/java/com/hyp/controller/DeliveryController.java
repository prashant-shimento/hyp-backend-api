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

import com.hyp.constants.Constants;
import com.hyp.dto.DeliveryDto;
import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.OrderStatusType;
import com.hyp.model.DeliveryOrderStatus;
import com.hyp.model.DeliveryOrderStatusResponse;
import com.hyp.model.DeliveryQuote;
import com.hyp.model.DeliveryRiderLocation;
import com.hyp.request.DeliveryOrderRequest;
import com.hyp.response.Response;
import com.hyp.service.AddressService;
import com.hyp.service.CustomerService;
import com.hyp.service.DeliveryService;
import com.hyp.service.LocationService;
import com.hyp.service.OrderService;
import com.hyp.service.PosService;
import com.hyp.service.RestaurantService;
import com.hyp.translation.DeliveryRequestTranslation;
import com.hyp.translation.DeliveryTranslation;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/delivery")
public class DeliveryController extends BaseController<DeliveryDto, Delivery, String> {

	@Autowired
	DeliveryTranslation deliveryTranslation;

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

	@Autowired
	CustomerService customerService;

	@PostMapping("/callback")
	public ResponseEntity<Response> updateDeliveryOrderStatus(@RequestBody DeliveryOrderStatus deliveryOrderData) {
		Delivery delivery = deliveryService.findByDeliveryOrderId(deliveryOrderData.getId());
		Response response;
		try {
			if (delivery == null) {
				response = new Response(null, true, "Delivery Id not found " + deliveryOrderData.getId());
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			deliveryService.processDeliveryCallback(delivery, deliveryOrderData);
			response = new Response(null, false, "Success");
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			log.error("Exception occurred in updateDeliveryOrderStatus " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/quote/{restaurantId}")
	public ResponseEntity<Response> getDeliveryQuote(@PathVariable("restaurantId") String restaurantId,
			@RequestParam("addressId") String addressId) {
		Response response = null;
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

			Optional<DeliveryQuote.DeliveryNetworks> filteredQuotes = deliveryQuote.getData().getItems().stream()
					.filter(item -> item.isPickupNow())
					.filter(items -> !items.getService().equalsIgnoreCase("loadshare"))
					.sorted(Comparator.comparingDouble(item -> item.getQuote().getPrice())).findFirst();
			return filteredQuotes.isPresent()
					? ResponseEntity.ok(new Response(Collections.singletonList(filteredQuotes.get()), false,
							"Delivery Quotes Fetched"))
					: ResponseEntity.notFound().build();

		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			log.error("Exception occurred in getDeliveryQuote " + e.getMessage());
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
			log.error("Exception occurred in getRiderLocation " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@PostMapping("/create/{orderId}")
	public ResponseEntity<Response> createDeliveryOrder(@PathVariable String orderId) {
		Response response;
		try {
			Order order = orderService.findById(orderId);
			if (order == null) {
				response = new Response(null, true, "Order not found " + orderId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			Address address = addressService.findById(order.getDeliveryDetails().getAddressId());
			Customer customer = customerService.findById(order.getCustomerId());
			Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
			DeliveryOrderRequest deliveryOrderRequest = DeliveryRequestTranslation.getDeliveryOrderRequest(restaurant,
					address, customer, order);
			deliveryService.createOrder(deliveryOrderRequest, order);

			response = new Response(null, false, "Delivery Order Created");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			log.error("Exception occurred in createDeliveryOrder " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@PostMapping("/fulfill/{orderId}")
	public ResponseEntity<Response> fulfillOrder(@PathVariable String orderId,
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
					deliveryService.processDeliverySmartFulfill(delivery, Constants.SMART);
				} else {
					deliveryService.processDeliveryFulfill(delivery, Constants.API);
				}
				orderService.updateOrderStatus(orderId, OrderStatusType.READY_FOR_DELIVERY);
			}

			response = new Response(Collections.singletonList(delivery), false, "Delivery Fullfilled");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			log.error("Exception occurred in smartFulfill " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@PostMapping("/consume/{orderId}")
	public ResponseEntity<Response> consumeDeliveryCallback(@PathVariable String orderId) {
		Response response;
		try {
			Order order = orderService.findById(orderId);
			if (order == null) {
				response = new Response(null, true, "Order not found " + orderId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			Delivery delivery = deliveryService.findByOrderId(order.getId());
			if (delivery == null) {
				response = new Response(null, true, "Delivery Id not found ");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			DeliveryOrderStatusResponse deliverOrderStatusResponse = deliveryService
					.getDeliveryOrderStatus(delivery.getDeliveryOrderId());
			deliveryService.processDeliveryCallback(delivery, deliverOrderStatusResponse.getData());
			response = new Response(Collections.singletonList(delivery), false, "Delivery Processed Consumed");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			log.error("Exception occurred in consumeDeliveryCallback " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

}
