package com.hyp.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.AddonGroupDto;
import com.hyp.entity.AddonGroup;
import com.hyp.mapper.DataMapper;
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

	@GetMapping("/items")
	public ResponseEntity<List<AddonGroup>> getAllAddonGroupsAndItems() {
		List<AddonGroup> addonGroups = addonGroupService.getAllAddonGroupsAndItems();
		return ResponseEntity.ok().body(addonGroups);
	}

	@GetMapping("/items/{addonGroupId}")
	public ResponseEntity<List<AddonGroup>> getAddonGroupsAndItemsById(@PathVariable String addonGroupId) {
		List<AddonGroup> addonGroups = addonGroupService.getAddonGroupsAndItemsById(addonGroupId);
		return ResponseEntity.ok().body(addonGroups);
	}
}
