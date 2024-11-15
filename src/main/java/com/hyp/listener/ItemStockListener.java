package com.hyp.listener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

	@Autowired
	SimpMessagingTemplate messageTemplate;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String expiredKey = message.toString();

		if (expiredKey.endsWith(":stock")) {
			String[] keyParts = expiredKey.split(":");
			if (keyParts.length >= 2) {
				String entityType = keyParts[0]; 
				String entityId = keyParts[1]; 
				
				log.info("Received {} Expiry from Redis for {}", entityType, entityId);

				switch (entityType) {
				case "item":
					processItemStockChange(entityId);
					break;
				case "addon":
					processAddonStockChange(entityId);
					break;
				default:
					log.warn("Unknown entity type: {}", entityType);
				}
			} else {
				log.warn("Invalid key format: {}", expiredKey);
			}
		}
	}

	private void processItemStockChange(String itemId) {
		Item item = itemService.findById(itemId);
		if (item != null) {
			item.setActive("1");
			itemService.save(item);

			Map<String, Object> payload = new HashMap<>();
			payload.put("inStock", true);
			payload.put("itemID", Collections.singletonList(itemId));

			messageTemplate.convertAndSend("/topic/item-status", payload);
			log.info("Sent stock update message for item {}", itemId);
		} else {
			log.warn("Item not found for ID: {}", itemId);
		}
	}

	private void processAddonStockChange(String addonItemId) {
		AddonItem addonItem = addonItemService.findById(addonItemId);
		if (addonItem != null) {
			addonItem.setActive("1");
			addonItemService.save(addonItem);

			Map<String, Object> payload = new HashMap<>();
			payload.put("inStock", true);
			payload.put("itemID", Collections.singletonList(addonItemId));

			messageTemplate.convertAndSend("/topic/item-status", payload);
			log.info("Sent stock update message for addon item {}", addonItemId);
		} else {
			log.warn("Addon Item not found for ID: {}", addonItemId);
		}
	}

}
