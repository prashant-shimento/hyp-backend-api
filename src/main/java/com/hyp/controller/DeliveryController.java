package com.hyp.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.constants.Constants;
import com.hyp.constants.ErrorConstants;
import com.hyp.dto.DeliveryDto;
import com.hyp.entity.Address;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.event.OrderEventPublisher;
import com.hyp.exception.BadRequestException;
import com.hyp.exception.DeliveryException;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.model.DeliveryOrderStatus;
import com.hyp.model.DeliveryOrderStatus.DeliveryOrderData;
import com.hyp.model.DeliveryQuote;
import com.hyp.model.DeliveryRiderLocation;
import com.hyp.model.RiderLocation;
import com.hyp.response.Response;
import com.hyp.service.AddressService;
import com.hyp.service.DeliveryService;
import com.hyp.service.LocationService;
import com.hyp.service.OrderService;
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
	OrderService orderService;

	@Autowired
	LocationService locationService;

	@Autowired
	private OrderEventPublisher orderEventPublisher;

	@PostMapping("/callback")
	public ResponseEntity<Response> updateDeliveryOrderStatus(@RequestBody DeliveryOrderData deliveryOrderData)
			throws EntityNotFoundException, DeliveryException {
		Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(deliveryOrderData.getReferenceId()))
				.orElseThrow(() -> new EntityNotFoundException("Delivery", deliveryOrderData.getId()));
		deliveryService.processDeliveryCallback(delivery, deliveryOrderData);
		return ResponseEntity.ok(new Response(null, false, "Success"));
	}

	@GetMapping("/quote/{restaurantId}")
	public ResponseEntity<Response> getDeliveryQuote(@PathVariable String restaurantId, String addressId)
			throws EntityNotFoundException, BadRequestException, DeliveryException {

		Restaurant restaurant = Optional.ofNullable(restaurantService.findById(restaurantId))
				.orElseThrow(() -> new EntityNotFoundException(Restaurant.class.getSimpleName(), restaurantId));

		if (restaurant.getPincode() == null || !restaurant.getPincode().matches("\\d{6}")) {
			throw new BadRequestException("Pin code", "Invalid pin code " + restaurant.getPincode());
		}

		Address address = Optional.ofNullable(addressService.findById(addressId))
				.orElseThrow(() -> new EntityNotFoundException(Address.class.getSimpleName(), addressId));

		if (address.getPincode() == null || !address.getPincode().matches("\\d{6}")) {
			throw new BadRequestException("Pin code", "Invalid pin code " + address.getPincode());
		}

		if (!locationService.isLocationDeliverable(address.getLocation().getLatitude(),
				address.getLocation().getLongitude(), restaurant.getLocation().getLatitude(),
				restaurant.getLocation().getLongitude(), restaurant.getDeliveryRadius())) {
			throw new BadRequestException("Location", "The location is not deliverable.");
		}

		log.info("Requesting Delivery quote for addressId {} and restaurantId {}", addressId, restaurantId);
		DeliveryQuote deliveryQuote = deliveryService
				.getDeliveryQuote(DeliveryRequestTranslation.getQuoteRequest(restaurant, address));

		if (deliveryQuote.getData().getItems().isEmpty()) {
			throw new EntityNotFoundException("Delivery", ErrorConstants.DELIVERY_OPTION_NOT_FOUND);
		}

		Optional<DeliveryQuote.DeliveryNetworks> filteredQuotes = deliveryQuote.getData().getItems().stream()
                .filter(DeliveryQuote.DeliveryNetworks::isPickupNow).filter(items -> !items.getService().equalsIgnoreCase("loadshare")).min(Comparator.comparingDouble(item -> item.getQuote().getPrice()));
		if (filteredQuotes.isEmpty()) {
			throw new EntityNotFoundException("Delivery", ErrorConstants.DELIVERY_OPTION_NOT_FOUND);
		}
		return ResponseEntity
				.ok(new Response(Collections.singletonList(filteredQuotes.get()), false, "Delivery Quotes Fetched"));
	}

	@Hidden
	@GetMapping("/rider-detail/{orderId}")
	public ResponseEntity<Response> getRiderDetails(@PathVariable String orderId)
			throws EntityNotFoundException, DeliveryException {
		Order order = Optional.ofNullable(orderService.findById(orderId))
				.orElseThrow(() -> new EntityNotFoundException("Order", orderId));
		Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
				.orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));
		DeliveryRiderLocation deliveryRiderLocation = deliveryService
				.getDeliveryRiderLocation(delivery.getDeliveryOrderId());

		Response response = new Response(Collections.singletonList(deliveryRiderLocation), false,
				"Rider Details Fetched");
		return ResponseEntity.ok(response);

	}
	
	@GetMapping("/rider-location/{orderId}")
	public ResponseEntity<Response> getRiderLocation(@PathVariable String orderId)
			throws EntityNotFoundException, DeliveryException {
		RiderLocation riderLocation = null;
		Order order = Optional.ofNullable(orderService.findById(orderId))
				.orElseThrow(() -> new EntityNotFoundException("Order", orderId));
		Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
				.orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));
		if(delivery.getFulfillment().getChannel().getName().equalsIgnoreCase("porter")
				|| delivery.getService().equalsIgnoreCase("porter")){
			log.info("Getting Porter Rider location of the order {}", orderId);
			riderLocation = deliveryService.getPorterRiderLocation(delivery.getDeliveryOrderId());
		} else {
			log.info("Getting Rider location of the order {}", orderId);
			riderLocation = deliveryService.getRiderLocation(delivery.getDeliveryOrderId());
		}
		Response response = new Response(Collections.singletonList(riderLocation), false,
				"Rider Location Fetched");
		return ResponseEntity.ok(response);

	}

	@PostMapping("/create/{orderId}")
	public ResponseEntity<Response> createDeliveryOrder(@PathVariable String orderId)
			throws EntityNotFoundException, DeliveryException {
		Order order = Optional.ofNullable(orderService.findById(orderId))
				.orElseThrow(() -> new EntityNotFoundException("Order", orderId));
		if(Optional.ofNullable(deliveryService.findByOrderId(orderId)).isPresent()) {
			throw new DeliveryException("Delivery order already exists for Order ID: " + orderId); 
		}
		orderEventPublisher.publishDeliveryOrderEvent(order);
		Response response = new Response(null, false, "Delivery Order Created");
		return ResponseEntity.ok(response);
	}

	@PostMapping("/fulfill/{orderId}")
	public ResponseEntity<Response> fulfillOrder(@PathVariable String orderId,
			@RequestParam(defaultValue = "", required = false) String fulfillType)
			throws EntityNotFoundException, DeliveryException {
		Order order = Optional.ofNullable(orderService.findById(orderId))
				.orElseThrow(() -> new EntityNotFoundException("Order", orderId));

		Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
				.orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));
		deliveryService.processDeliveryOrderFulfill(delivery, Constants.API, fulfillType);

		Response response = new Response(Collections.singletonList(deliveryTranslation.getDto(delivery)), false,
				"Delivery Fullfilled");
		return ResponseEntity.ok(response);

	}

	@PostMapping("/consume/{orderId}")
	public ResponseEntity<Response> consumeDeliveryCallback(@PathVariable String orderId)
			throws EntityNotFoundException, DeliveryException {
		Order order = Optional.ofNullable(orderService.findById(orderId))
				.orElseThrow(() -> new EntityNotFoundException("Order", orderId));

		Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
				.orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));

		DeliveryOrderStatus deliverOrderStatus = deliveryService.getDeliveryOrderStatus(delivery.getDeliveryOrderId());
		deliveryService.processDeliveryCallback(delivery, deliverOrderStatus.getData());
		Response response = new Response(Collections.singletonList(deliveryTranslation.getDto(delivery)), false,
				"Delivery Processed Consumed");
		return ResponseEntity.ok(response);

	}

	@PostMapping("/unallocate/{orderId}")
	public ResponseEntity<Response> unallocate(@PathVariable String orderId)
			throws DeliveryException, EntityNotFoundException {
		Order order = Optional.ofNullable(orderService.findById(orderId))
				.orElseThrow(() -> new EntityNotFoundException("Order", orderId));

		Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
				.orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));

		deliveryService.unallocateDeliveryOrder(delivery.getDeliveryOrderId());
		Response response = new Response(null, false, "Order Unallocated Successfully");
		return ResponseEntity.ok(response);
	}

	@PostMapping("/cancel/{orderId}")
	public ResponseEntity<Response> cancelDeliveryOrder(@PathVariable String orderId)
			throws DeliveryException, EntityNotFoundException {
		Response response;
		Order order = Optional.ofNullable(orderService.findById(orderId))
				.orElseThrow(() -> new EntityNotFoundException("Order", orderId));

		Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
				.orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));

		deliveryService.cancelDeliveryOrder(delivery.getDeliveryOrderId());
		response = new Response(null, false, "Delivery Order Cancelled");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/status/{orderId}")
	public ResponseEntity<Response> getDeliveryOrderStatus(@PathVariable String orderId)
			throws DeliveryException, EntityNotFoundException {
		Order order = Optional.ofNullable(orderService.findById(orderId))
				.orElseThrow(() -> new EntityNotFoundException("Order", orderId));

		Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
				.orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));

		DeliveryOrderStatus deliveryOrderStatus = deliveryService.getDeliveryOrderStatus(delivery.getDeliveryOrderId());
		Response response = new Response(Collections.singletonList(deliveryOrderStatus.getData()), false,
				"Delivery Order Status Fetched");
		return ResponseEntity.ok(response);
	}


}
