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
import com.hyp.request.PosDataRequest;
import com.hyp.request.PosStatusRequest;
import com.hyp.request.PosStockRequest;
import com.hyp.response.Response;
import com.hyp.service.PosService;
import com.hyp.service.RestaurantService;
import com.hyp.service.StockService;
import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RestController
@RequestMapping("/api/pos")
public class PosController {

	@Autowired
	PosService posDataService;

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	StockService stockService;

	@PostMapping("/menu")
	public ResponseEntity<Response> save(@RequestBody PosDataRequest posDataRequest) {
		JSONObject jb = new JSONObject(posDataRequest);
		System.out.println(jb.toString());
		boolean result = posDataService.savePosData(posDataRequest);
		Response response = new Response.Builder()
				.httpCode(result ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value())
				.message(result ? "Menu Updated Successfully" : "Something Went Wrong")
				.status(result ? "success" : "failed").build();
		return ResponseEntity.ok(response);
	}

	@PostMapping("/status/get")
	public ResponseEntity<Response> getStatus(@RequestBody PosStatusRequest getStatus) {
		JSONObject jb = new JSONObject(getStatus);
		System.out.println(jb.toString());
		Restaurant restaurant = restaurantService.findByMenuSharingCode(getStatus.getRestID());
		Response response = new Response.Builder()
				.httpCode(restaurant != null ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value())
				.message(restaurant != null ? "Store Delivery Status fetched successfully" : "Restaurant Not Found")
				.status(restaurant != null ? "success" : "failed")
				.storeStatus(restaurant != null && restaurant.isActive() ? "1" : "0").build();
		return ResponseEntity.ok(response);
	}

	@PostMapping("/status/update")
	public ResponseEntity<Response> updateStatus(@RequestBody PosStatusRequest updateStatus) {
		JSONObject jb = new JSONObject(updateStatus);
		System.out.println(jb.toString());
		Response response = null;
		Restaurant restaurant = restaurantService.findByMenuSharingCode(updateStatus.getRestID());
		if (restaurant == null) {
			response = new Response.Builder().httpCode(HttpStatus.NOT_FOUND.value()).message("Restaurant Not Found")
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
		response = new Response.Builder().httpCode(HttpStatus.OK.value())
				.message("Store Status updated successfully for store restID").status("success").build();
		updateStatus.setMessage("Store Status updated successfully for store restID");
		updateStatus.setStatus("success");
		return new ResponseEntity<Response>(response, HttpStatus.OK);
	}

	@PostMapping("/stock")
	public ResponseEntity<Response> updateStock(@RequestBody PosStockRequest stockRequest) {
		JSONObject jb = new JSONObject(stockRequest);
		System.out.println(jb.toString());
		Response response = null;
		Restaurant restaurant = restaurantService.findByMenuSharingCode(stockRequest.getRestID());
		if (restaurant == null) {
			response = new Response.Builder().code(HttpStatus.NOT_FOUND.value()).message("Restaurant Not Found")
					.status("failed").build();
			return ResponseEntity.ok(response);
		}
		boolean result = stockService.updateStock(stockRequest);
		response = new Response.Builder()
				.code(result ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value())
				.message(result ? "Stock Updated Successfully" : "Something Went Wrong")
				.status(result ? "success" : "failed").build();
		System.out.println(response.toString());
		return ResponseEntity.ok(response);
	}
}
