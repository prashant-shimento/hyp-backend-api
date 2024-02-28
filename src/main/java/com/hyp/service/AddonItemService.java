package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.AddonItem;
import com.hyp.repository.AddonItemRepository;

@Service
public class AddonItemService extends BaseServiceImpl<AddonItem, String> {
	@Autowired
	AddonItemRepository addonItemRepository;
}
