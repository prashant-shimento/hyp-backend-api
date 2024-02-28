package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.entity.Restaurant;
import com.hyp.request.StockRequest;
import com.hyp.response.Response;
import com.hyp.service.RestaurantService;
import com.hyp.service.StockService;
@RestController
@RequestMapping("/api/pos/stock")
public class StockController {

	@Autowired
	RestaurantService restaurantService;
	
	@Autowired
	StockService stockService;
	
	@PostMapping()
	public ResponseEntity<Response> updateStock(@RequestBody StockRequest stockRequest) {
		Response response = null;
		Restaurant restaurant = restaurantService.findById(stockRequest.getRestID());
		if (restaurant == null) {
			response = new Response.Builder().httpCode(HttpStatus.NOT_FOUND.value()).message("Restaurant Not Found")
					.status("failed").build();
			return ResponseEntity.ok(response);
		}
		boolean result = stockService.updateStock(stockRequest);
		response = new Response.Builder()
				.httpCode(result ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value())
				.message(result ? "Stock Updated Successfully" : "Something Went Wrong")
				.status(result ? "success" : "failed").build();
		return ResponseEntity.ok(response);
	}
}
