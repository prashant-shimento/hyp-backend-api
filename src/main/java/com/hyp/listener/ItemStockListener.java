package com.hyp.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.hyp.entity.AddonItem;
import com.hyp.entity.Item;
import com.hyp.service.AddonItemService;
import com.hyp.service.ItemService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ItemStockListener implements MessageListener {

	@Autowired
	ItemService itemService;

	@Autowired
	AddonItemService addonItemService;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String expiredKey = message.toString();
		if (expiredKey.startsWith("item:") && expiredKey.endsWith(":stock")) {
			String itemId = expiredKey.split(":")[1];
			log.info("Received Item Expiry from Redis for {}", itemId);
			Item item = itemService.findById(itemId);
			if (item != null) {
				item.setActive("1");
				itemService.save(item);
			}
		} else if (expiredKey.startsWith("addon:") && expiredKey.endsWith(":stock")) {
			String addonItemId = expiredKey.split(":")[1];
			log.info("Received AddonItem Expiry from Redis for {}", addonItemId);
			AddonItem addonItem = addonItemService.findById(addonItemId);
			if (addonItem != null) {
				addonItem.setActive("1");
				addonItemService.save(addonItem);
			}
		}
	}

}
