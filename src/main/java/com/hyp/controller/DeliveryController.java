package com.hyp.controller;

import java.util.Collections;

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
import com.hyp.entity.Restaurant;
import com.hyp.model.DeliveryQuote;
import com.hyp.request.DeliveryOrderStatus;
import com.hyp.response.Response;
import com.hyp.service.AddressService;
import com.hyp.service.DeliveryService;
import com.hyp.service.RestaurantService;
import com.hyp.translation.DeliveryRequestTranslation;

@RestController
@RequestMapping("/delivery")
public class DeliveryController {

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	DeliveryService deliveryService;

	@Autowired
	AddressService addressService;

	@PostMapping("/callback")
	public ResponseEntity<Response> updateDeliveryOrderStatus(@RequestBody DeliveryOrderStatus deliveryOrderStatus) {
		Response response;
		try {
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
			response = new Response(Collections.singletonList(deliveryQuote.getData()), false, "Delivery Quotes Fetched");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

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
