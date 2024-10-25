package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.entity.Restaurant;
import com.hyp.request.PosCallbackRequest;
import com.hyp.request.PosDataRequest;
import com.hyp.request.PosStatusRequest;
import com.hyp.request.PosStockRequest;
import com.hyp.response.PosResponse;
import com.hyp.service.OrderService;
import com.hyp.service.PosService;
import com.hyp.service.RestaurantService;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RestController
@RequestMapping("/pos")
public class PosController {

	@Autowired
	PosService posDataService;

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	OrderService orderService;

	@Autowired
	SimpMessagingTemplate messageTemplate;

	@PostMapping("/menu")
	public ResponseEntity<PosResponse> saveMenuData(@RequestBody PosDataRequest posDataRequest) {
		boolean result = posDataService.savePosData(posDataRequest);
		return ResponseEntity.ok(PosResponse.builder().success(result ? "1" : "0")
				.message(result ? "Menu items are successfully listed." : "Something Went Wrong").build());
	}

	@PostMapping("/status/get")
	public ResponseEntity<PosResponse> getStatus(@RequestBody PosStatusRequest getStatus) {
		Restaurant restaurant = restaurantService.findByMenuSharingCode(getStatus.getRestaurantId());
		PosResponse response = PosResponse.builder()
				.httpCode(restaurant != null ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value())
				.status(restaurant != null ? "success" : "failed")
				.storeStatus(restaurant != null && restaurant.isActive() ? "1" : "0")
				.message(restaurant != null ? "Store Delivery Status fetched successfully" : "Restaurant Not Found")
				.build();
		return ResponseEntity.ok(response);
	}

	@PostMapping("/status/update")
	public ResponseEntity<PosResponse> updateStatus(@RequestBody PosStatusRequest updateStatus) {
		PosResponse response = null;
		Restaurant restaurant = restaurantService.findByMenuSharingCode(updateStatus.getRestaurantId());
		if (restaurant == null) {
			response = PosResponse.builder().httpCode(HttpStatus.NOT_FOUND.value()).message("Restaurant Not Found")
					.status("failed").build();
			return ResponseEntity.ok(response);
		}
		boolean result = posDataService.updateRestaurant(updateStatus);
		messageTemplate.convertAndSend("/topic/restaurant-status", updateStatus);
		response = PosResponse.builder().code(result ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value())
				.message(result ? "Restaurant Updated Successfully" : "Something Went Wrong")
				.status(result ? "success" : "failed").build();
		return ResponseEntity.ok(response);
	}

	@PostMapping("/stock")
	public ResponseEntity<PosResponse> updateStock(@RequestBody PosStockRequest stockRequest) {
		PosResponse response = null;
		Restaurant restaurant = restaurantService.findByMenuSharingCode(stockRequest.getRestaurantId());
		if (restaurant == null) {
			response = PosResponse.builder().code(HttpStatus.NOT_FOUND.value()).message("Restaurant Not Found")
					.status("failed").build();
			return ResponseEntity.ok(response);
		}
		boolean result = posDataService.updateStock(stockRequest);
		messageTemplate.convertAndSend("/topic/item-status", stockRequest);
		response = PosResponse.builder().code(result ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value())
				.message(result ? "Stock Updated Successfully" : "Something Went Wrong")
				.status(result ? "success" : "failed").build();
		return ResponseEntity.ok(response);
	}

	@PostMapping("/order/callback")
	public ResponseEntity<PosResponse> orderCallBack(@RequestBody PosCallbackRequest posCallbackRequest) {
		PosResponse response = null;
		try {
			orderService.processOrderCallback(posCallbackRequest);
			response = PosResponse.builder().httpCode(HttpStatus.OK.value()).message("Order Updated Successfully")
					.build();
			return new ResponseEntity<PosResponse>(response, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			response = PosResponse.builder().httpCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).message("Error Occured")
					.error(e.getMessage()).build();
			return new ResponseEntity<PosResponse>(response, HttpStatus.OK);
		}
	}
}
