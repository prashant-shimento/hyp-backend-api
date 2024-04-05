package com.hyp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.AddonGroupDto;
import com.hyp.entity.AddonGroup;
import com.hyp.mapper.DataMapper;
import com.hyp.response.ResponseTemplate;
import com.hyp.service.AddonGroupService;
import com.hyp.translation.AddonGroupsTranslation;

@RestController
@RequestMapping("/addon-groups")
public class AddonGroupController extends BaseListController<AddonGroupDto, AddonGroup, String> {

	@Autowired
	public AddonGroupsTranslation addonGroupTranslation;

	@Autowired
	public AddonGroupService addonGroupService;

	@Autowired
	DataMapper dataMapper;

	@GetMapping("/{addonGroupId}/items")
	public ResponseEntity<ResponseTemplate> getAddonGroupsAndItemsById(
			@PathVariable("addonGroupId") String addonGroupId) {
		ResponseTemplate response = new ResponseTemplate();
		try {
			if (!addonGroupService.isExistsById(addonGroupId)) {
				response = new ResponseTemplate(null, true, "AddonGroup not found " + addonGroupId);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			List<AddonGroup> addonGroups = addonGroupService.getAddonGroupsAndItemsById(addonGroupId);
			response.setData(dataMapper.toAddonGroupDtoList(addonGroups));
			response.setError(false);
			response.setMessage("Addon groups and items retrieved successfully.");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.setError(true);
			response.setMessage("Error occurred while fetching addon groups and items: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/items")
	public ResponseEntity<ResponseTemplate> getAddonGroupsAndItems() {
		ResponseTemplate response = new ResponseTemplate();
		try {
			List<AddonGroup> addonGroups = addonGroupService.getAddonGroupsAndItems();
			response.setData(dataMapper.toAddonGroupDtoList(addonGroups));
			response.setError(false);
			response.setMessage("Addon groups and items retrieved successfully.");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.setError(true);
			response.setMessage("Error occurred while fetching addon groups and items: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

}
