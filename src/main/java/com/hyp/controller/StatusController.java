package com.hyp.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.entity.Restaurant;
import com.hyp.request.StatusRequest;
import com.hyp.response.Response;
import com.hyp.service.RestaurantService;

@RestController
@RequestMapping("/api/pos/status")
public class StatusController {
	
	@Autowired
	RestaurantService restaurantService;
	
	@PostMapping("/get")
	public ResponseEntity<Response> getStatus(@RequestBody StatusRequest getStatus) {
		Restaurant restaurant = restaurantService.findById(getStatus.getRestID());
		Response response = new Response.Builder().httpCode(restaurant != null ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value())
				.message(restaurant != null ? "Store Delivery Status fetched successfully" : "Restaurant Not Found")
				.status(restaurant != null ? "success" : "failed")
				.storeStatus(restaurant != null && restaurant.isActive() ? "1" : "0").build();
		return ResponseEntity.ok(response);
	}

	@PostMapping("/update")
	public ResponseEntity<Response> updateStatus(@RequestBody StatusRequest updateStatus) {
		Response response = null;
		Restaurant restaurant = restaurantService.findById(updateStatus.getRestID());
		if (restaurant == null) {
			response = new Response.Builder().httpCode(HttpStatus.NOT_FOUND.value()).message("Restaurant Not Found")
					.status("failed").build();
			return ResponseEntity.ok(response);
		}

		restaurant.setActive(updateStatus.getStore_status().equalsIgnoreCase("1") ? true : false);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime turnOnTime = LocalDateTime.parse(updateStatus.getTurn_on_time(), formatter);
		restaurant.setTurnOnTime(turnOnTime);
		restaurant.setStatusReason(updateStatus.getReason());
		restaurantService.update(restaurant);
		response = new Response.Builder().httpCode(HttpStatus.OK.value())
				.message("Store Status updated successfully for store restID").status("success")
				.build();
		updateStatus.setMessage("Store Status updated successfully for store restID");
		updateStatus.setStatus("success");
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}

}
