package com.hyp.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.maps.model.GeocodingResult;
import com.hyp.constants.ErrorConstants;
import com.hyp.dto.AddressDto;
import com.hyp.entity.Partner;
import com.hyp.entity.Restaurant;
import com.hyp.model.PlaceData;
import com.hyp.model.PlacePredictionData;
import com.hyp.request.LocationRequest;
import com.hyp.response.Response;
import com.hyp.service.LocationService;
import com.hyp.service.PartnerService;
import com.hyp.service.RestaurantService;
import com.hyp.translation.MapDataTranslation;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/location")
public class LocationController {

	@Autowired
	LocationService locationService;

	@Autowired
	PartnerService partnerService;

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
			log.error("Exception occurred in getLocationPrediction " + e.getMessage());
			response = new Response(null, true, ErrorConstants.INTERNAL_SERVER_ERROR);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/maps/place/{entityId}")
	public ResponseEntity<Response> getPlace(@PathVariable String entityId,
			@RequestBody LocationRequest locationRequest) {
		Response response;
		AddressDto addressPlaceData;
		try {
			Partner partner = partnerService.findByIdWithReference(entityId, Partner.class);
			Restaurant restaurant = null;
			if (partner == null) {
				restaurant = restaurantService.findById(entityId);
				if (restaurant == null) {
					response = new Response(null, true, "Entity not found for id: " + entityId);
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
				}
			}
			if (locationRequest.getPlaceId() != null) {
				String rawPlaceData = locationService.getPlaceDetails(locationRequest.getPlaceId());
				if (rawPlaceData == null) {
					response = new Response(null, true, "Place not found " + locationRequest.getPlaceId());
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
				}
				addressPlaceData = MapDataTranslation.getPlaceDataToAddress(rawPlaceData);
			} else if (locationRequest.getLatitude() != null && locationRequest.getLongitude() != null) {
				GeocodingResult[] geocodingResults = locationService
						.getPlaceByGeocodebyClient(locationRequest.getLatitude(), locationRequest.getLongitude());
				if (geocodingResults == null) {
					response = new Response(null, true, "Place not found for given co-ordinates");
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
				}
				addressPlaceData = MapDataTranslation.getGeocodeDataToAddress(geocodingResults);
			} else {
				response = new Response(null, true, "Please provide PlaceId or Co-ordinates");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}
			List<String> restaurantList;
			if (restaurant != null) {
				restaurantList = locationService.getServicableRestaurants(addressPlaceData,
						Collections.singletonList(restaurant));
			} else {
				restaurantList = locationService.getServicableRestaurants(addressPlaceData,
						partner.getRestaurantDetails());
			}

			if (restaurantList.size() == 0) {
				response = new Response(null, true, ErrorConstants.LOCATION_NOT_DELIVERBLE);
				return ResponseEntity.badRequest().body(response);
			}

			response = new Response(
					Collections.singletonList(
							PlaceData.builder().address(addressPlaceData).restaurants(restaurantList).build()),
					false, "Location Place Data Fetched");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Exception occurred in getPlace " + e.getMessage());
			response = new Response(null, true, ErrorConstants.INTERNAL_SERVER_ERROR);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

}
