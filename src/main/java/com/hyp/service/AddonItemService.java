package com.hyp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.AddonItem;
import com.hyp.repository.AddonItemRepository;

import java.util.List;

@Service
@Slf4j
public class AddonItemService extends BaseServiceImpl<AddonItem, String> {
	@Autowired
	AddonItemRepository addonItemRepository;

	public void updateAddonItemStock(List<String> addonItemIds, boolean inStock) {
		long findByIdsStart = System.currentTimeMillis();
		List<AddonItem> addonItems = findByIds(addonItemIds);
		log.info("Fetched addons in {} ms", System.currentTimeMillis() - findByIdsStart);

		addonItems.forEach(addonItem -> {
			addonItem.setActive(inStock ? "1" : "0");
		});
		long bulkWriteStart = System.currentTimeMillis();
		bulkUpdate(addonItems, AddonItem.class);
		log.info("Bulk write addons completed in {} ms", System.currentTimeMillis() - bulkWriteStart);
	}
}
