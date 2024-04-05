package com.hyp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.AddonGroupDTO;
import com.hyp.entity.AddonGroup;
import com.hyp.response.ResponseTemplate;
import com.hyp.service.AddonGroupService;
import com.hyp.translation.AddonGroupsTranslation;

@RestController
@RequestMapping("/addon-groups")
public class AddonGroupController extends BaseListController<AddonGroupDTO, AddonGroup, String> {

	@Autowired
	public AddonGroupsTranslation addonGroupTranslation;

	@Autowired
	public AddonGroupService addonGroupService;

	@GetMapping("/getAddonItems")
	public ResponseEntity<ResponseTemplate> getAddonGroupsAndItems() {
		ResponseTemplate response = new ResponseTemplate();
		try {
			List<AddonGroup> addonGroups = addonGroupService.getCategoryItems();
			response.setData(addonGroups);
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
