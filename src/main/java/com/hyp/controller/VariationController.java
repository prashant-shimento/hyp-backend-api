package com.hyp.controller;

import java.util.List;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.entity.Variation;
import com.hyp.response.ResponseTemplate;
import com.hyp.service.VariationService;

@RestController
@RequestMapping("/variations")
public class VariationController {

	@Autowired
	VariationService variationService;

	@GetMapping("/addons")
	public ResponseEntity<ResponseTemplate> getVariationsDetails() {
		ResponseTemplate response = new ResponseTemplate();
		try {
			List<Variation> variations = variationService.getVariationsWithAddonGroupsAndItems();
			response.setData(variations);
			response.setMessage("Variations retrieved successfully.");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.setError(true);
			response.setMessage("Error occurred while fetching variations: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/{variationId}/addons")
	public ResponseEntity<ResponseTemplate> getVariationsDetailsById(@PathVariable String variationId) {
		ResponseTemplate response = new ResponseTemplate();
		try {
			List<Variation> variations = variationService.getVariationsWithAddonGroupsAndItemsById(variationId);
			response.setData(variations);
			response.setMessage("Variations retrieved successfully.");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.setError(true);
			response.setMessage("Error occurred while fetching variations: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
