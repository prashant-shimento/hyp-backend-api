package com.hyp.controller;

import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.maps.model.GeocodingResult;
import com.hyp.dto.AddressDto;
import com.hyp.entity.Restaurant;
import com.hyp.model.PlaceData;
import com.hyp.model.PlacePredictionData;
import com.hyp.response.Response;
import com.hyp.service.LocationService;
import com.hyp.service.RestaurantService;
import com.hyp.translation.MapDataTranslation;

@RestController
@RequestMapping("/location")
public class LocationController {

	@Autowired
	LocationService locationService;

	@Autowired
	RestaurantService restaurantService;

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

	@GetMapping("/maps/place/{restaurantId}")
	public ResponseEntity<Response> getPlace(@PathVariable String restaurantId, @RequestParam String placeId) {
		Response response;
		try {
			Restaurant restaurant = restaurantService.findById(restaurantId);
			if (restaurantService == null) {
				response = new Response(null, true, "Restaurant not found " + restaurantId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			String rawPlaceData = locationService.getPlaceDetails(placeId);
			if (rawPlaceData == null) {
				response = new Response(null, true, "Place not found " + placeId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			AddressDto addressPlaceData = MapDataTranslation.getPlaceDatatoAddress(rawPlaceData);

			if (!locationService.isLocationDeliverable(addressPlaceData.getLatitude(), addressPlaceData.getLongitude(),
					restaurant.getLocation().getLatitude(), restaurant.getLocation().getLatitude())) {
				response = new Response(null, true, "Location Not Deliverable");
				return ResponseEntity.badRequest().body(response);
			}

			response = new Response(Collections.singletonList(addressPlaceData), false, "Location Place Data Fetched");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/maps/geocode/{restaurantId}")
	public ResponseEntity<Response> getGeoCodePlace(@PathVariable String restaurantId, @RequestParam double latitude,
			@RequestParam double longitude) {
		Response response;
		try {
			Restaurant restaurant = restaurantService.findById(restaurantId);
			if (restaurantService == null) {
				response = new Response(null, true, "Restaurant not found " + restaurantId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

			GeocodingResult[] geocodingResults = locationService.getPlaceByGeocodebyClient(latitude, longitude);
			if (geocodingResults == null) {
				response = new Response(null, true, "Place not found ");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			AddressDto addressPlaceData = MapDataTranslation.getGeocodeDatatoAddress(geocodingResults);

			if (!locationService.isLocationDeliverable(addressPlaceData.getLatitude(), addressPlaceData.getLongitude(),
					restaurant.getLocation().getLatitude(), restaurant.getLocation().getLatitude())) {
				response = new Response(null, true, "Location Not Deliverable");
				return ResponseEntity.badRequest().body(response);
			}

			response = new Response(Collections.singletonList(addressPlaceData), false, "Location Place Data Fetched");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
