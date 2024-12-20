package com.hyp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.constants.ErrorConstants;
import com.hyp.dto.ItemDto;
import com.hyp.entity.AddonGroup;
import com.hyp.entity.Item;
import com.hyp.entity.Variation;
import com.hyp.response.Response;
import com.hyp.service.ItemService;
import com.hyp.translation.ItemTranslation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/item")
public class ItemController extends BaseListController<ItemDto, Item, String> {

	@Autowired
	public ItemTranslation itemTranslation;
	
	@Autowired
	private ItemService itemService;
	
	@GetMapping("/{itemId}/variations")
	public ResponseEntity<Response> getVariationsDetails(@PathVariable String itemId) {
		Response response = new Response();
		try {
			List<Variation> variations = itemService.getVariationsByItemId(itemId);
			if(variations == null || variations.isEmpty()) {
				response = new Response(null, true, "Variations not found for Item : " + itemId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			response.setData(variations);
			response.setMessage("Variations retrieved successfully.");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Exception occurred in getVariationsDetails " + e.getMessage());
			response = new Response(null, true, ErrorConstants.INTERNAL_SERVER_ERROR);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
	
	@GetMapping("/{itemId}/addons")
	public ResponseEntity<Response> getAddons(@PathVariable String itemId) {
		Response response = new Response();
		try {
			List<AddonGroup> addonGroups = itemService.getAddonsByItemId(itemId);
			if(addonGroups == null || addonGroups.isEmpty()) {
				response = new Response(null, true, "Addons not found for Item : " + itemId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			response.setData(addonGroups);
			response.setMessage("Addons retrieved successfully.");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			log.error("Exception occurred in getAddons " + e.getMessage());
			response = new Response(null, true, ErrorConstants.INTERNAL_SERVER_ERROR);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
