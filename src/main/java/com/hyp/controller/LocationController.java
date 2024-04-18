package com.hyp.controller;

import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.hyp.model.PlaceData;
import com.hyp.model.PlacePredictionData;
import com.hyp.response.Response;
import com.hyp.service.LocationService;

@RestController
@RequestMapping("/location")
public class LocationController {

	@Autowired
	LocationService locationService;

	@GetMapping("/maps/predict")	
	public ResponseEntity<Response> getLocationPrediction(@RequestParam String search) {
		Response response;
		try {
			PlacePredictionData placePredictionData = locationService.getLocationPrediction(search);
			response = new Response(Collections.singletonList(placePredictionData), false,
					"Location Prediction Fetched");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/maps/place")
	public ResponseEntity<Response> getPlace(@RequestParam String placeId) {
		Response response;
		try {
			PlaceData placeData = locationService.getPlace(placeId);
			response = new Response(Collections.singletonList(placeData), false, "Location Place Data Fetched");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
