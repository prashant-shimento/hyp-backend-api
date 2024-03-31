package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.entity.Restaurant;
import com.hyp.response.ResponseTemplate;
import com.hyp.service.DeliveryService;
import com.hyp.service.RestaurantService;

@RestController
@RequestMapping("/delivery")
public class DeliveryController {

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	DeliveryService deliveryService;

	@PostMapping("/check/{restaurantId}")
	public ResponseEntity<ResponseTemplate> checkDelivery(@PathVariable("restaurantId") String restaurantId,
			@RequestParam("latitude") double latitude, @RequestParam("longitude") double longitude) {
		ResponseTemplate response;
		try {
			Restaurant restaurant = restaurantService.findById(restaurantId);
			if (restaurantService == null) {
				response = new ResponseTemplate(null, true, "Restaurant not found " + restaurantId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			if (!deliveryService.isLocationDeliverable(latitude, longitude, restaurant.getLocation().getLatitude(),
					restaurant.getLocation().getLatitude())) {
				response = new ResponseTemplate(null, false, "Location Not Deliverable");
				return ResponseEntity.badRequest().body(response);
			}
			response = new ResponseTemplate(null, false, "Location Deliverable");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new ResponseTemplate(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
