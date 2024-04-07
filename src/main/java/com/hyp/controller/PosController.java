package com.hyp.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

	@PostMapping("/menu")
	public ResponseEntity<PosResponse> saveMenuData(@RequestBody PosDataRequest posDataRequest) {
		JSONObject jb = new JSONObject(posDataRequest);
		System.out.println(jb.toString());
		boolean result = posDataService.savePosData(posDataRequest);
		PosResponse response = new PosResponse.Builder()
				.httpCode(result ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value())
				.message(result ? "Menu Updated Successfully" : "Something Went Wrong")
				.status(result ? "success" : "failed").build();
		return ResponseEntity.ok(response);
	}

	@PostMapping("/status/get")
	public ResponseEntity<PosResponse> getStatus(@RequestBody PosStatusRequest getStatus) {
		JSONObject jb = new JSONObject(getStatus);
		System.out.println(jb.toString());
		Restaurant restaurant = restaurantService.findByMenuSharingCode(getStatus.getRestID());
		PosResponse response = new PosResponse.Builder()
				.httpCode(restaurant != null ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value())
				.message(restaurant != null ? "Store Delivery Status fetched successfully" : "Restaurant Not Found")
				.status(restaurant != null ? "success" : "failed")
				.storeStatus(restaurant != null && restaurant.isActive() ? "1" : "0").build();
		return ResponseEntity.ok(response);
	}

	@PostMapping("/status/update")
	public ResponseEntity<PosResponse> updateStatus(@RequestBody PosStatusRequest updateStatus) {
		JSONObject jb = new JSONObject(updateStatus);
		System.out.println(jb.toString());
		PosResponse response = null;
		Restaurant restaurant = restaurantService.findByMenuSharingCode(updateStatus.getRestID());
		if (restaurant == null) {
			response = new PosResponse.Builder().httpCode(HttpStatus.NOT_FOUND.value()).message("Restaurant Not Found")
					.status("failed").build();
			return ResponseEntity.ok(response);
		}

		restaurant.setActive(updateStatus.getStore_status().equalsIgnoreCase("1") ? true : false);
		if (!restaurant.isActive()) {
			restaurant.setTurnOnTime(LocalDateTime.parse(updateStatus.getTurn_on_time(),
					DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		}
		restaurant.setStatusReason(updateStatus.getReason());
		restaurantService.update(restaurant);
		response = new PosResponse.Builder().httpCode(HttpStatus.OK.value())
				.message("Store Status updated successfully for store restID").status("success").build();
		updateStatus.setMessage("Store Status updated successfully for store restID");
		updateStatus.setStatus("success");
		return new ResponseEntity<PosResponse>(response, HttpStatus.OK);
	}

	@PostMapping("/stock")
	public ResponseEntity<PosResponse> updateStock(@RequestBody PosStockRequest stockRequest) {
		JSONObject jb = new JSONObject(stockRequest);
		System.out.println(jb.toString());
		PosResponse response = null;
		Restaurant restaurant = restaurantService.findByMenuSharingCode(stockRequest.getRestID());
		if (restaurant == null) {
			response = new PosResponse.Builder().code(HttpStatus.NOT_FOUND.value()).message("Restaurant Not Found")
					.status("failed").build();
			return ResponseEntity.ok(response);
		}
		boolean result = posDataService.updateStock(stockRequest);
		response = new PosResponse.Builder()
				.code(result ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value())
				.message(result ? "Stock Updated Successfully" : "Something Went Wrong")
				.status(result ? "success" : "failed").build();
		System.out.println(response.toString());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/order/callback")
	public ResponseEntity<PosResponse> orderCallBack(@RequestBody PosCallbackRequest posCallbackRequest) {
		JSONObject jb = new JSONObject(posCallbackRequest);
		System.out.println(jb.toString());
		PosResponse response = null;
		try {
			orderService.processCallback(posCallbackRequest);
			response = new PosResponse.Builder().httpCode(HttpStatus.OK.value()).message("Order Updated Successfully")
					.error(null).build();
			return new ResponseEntity<PosResponse>(response, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			response = new PosResponse.Builder().httpCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.message("Error Occured").error(e.getMessage()).build();
			return new ResponseEntity<PosResponse>(response, HttpStatus.OK);
		}
	}
}
