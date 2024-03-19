package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.entity.Restaurant;
import com.hyp.request.PosStockRequest;
import com.hyp.response.Response;
import com.hyp.service.RestaurantService;
import com.hyp.service.StockService;
@RestController
@RequestMapping("/api")
public class PosStockController {

	@Autowired
	RestaurantService restaurantService;
	
	@Autowired
	StockService stockService;
	
	
}
