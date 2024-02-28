package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.AddonGroup;
import com.hyp.repository.AddonGroupRepository;

@Service
public class AddonGroupService extends BaseServiceImpl<AddonGroup, String> {
	@Autowired
	AddonGroupRepository addonGroupRepository;
}
