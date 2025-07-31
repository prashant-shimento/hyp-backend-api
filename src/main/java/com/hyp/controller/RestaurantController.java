package com.hyp.controller;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.RestaurantDto;
import com.hyp.entity.Restaurant;
import com.hyp.response.Response;
import com.hyp.service.RestaurantService;
import com.hyp.translation.RestaurantTranslation;

@RestController
@RequestMapping("/restaurant")
public class RestaurantController extends BaseListController<RestaurantDto, Restaurant, String> {

	@Autowired
	public RestaurantTranslation restaurantTranslation;

	@Autowired
	private SimpMessagingTemplate messageTemplate;

	@Autowired
	RestaurantService restaurantService;

	@PatchMapping("/{restaurantId}")
	public ResponseEntity<Response> updateRestaurant(@PathVariable String restaurantId,
			@RequestBody RestaurantDto restaurantDto) {

		try {
			Restaurant existingRestaurant = restaurantService.findById(restaurantId);
			if (existingRestaurant == null) {
				String message = "Restaurant not found with ID: " + restaurantId;
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Response(null, true, message));
			}
			restaurantTranslation.updateEntityFromDto(restaurantDto, existingRestaurant);
			Restaurant restaurant = restaurantService.save(existingRestaurant);
			RestaurantDto restaurantData = restaurantTranslation.getDto(restaurant);
			messageTemplate.convertAndSend("/topic/restaurant-serviceable", restaurantData);
			Response response = new Response(Collections.singletonList(restaurantData), false,
					"Restaurant updated successfully");
			return ResponseEntity.ok(response);

		} catch (Exception ex) {
			Response response = new Response(null, true, ex.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}