package com.hyp.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.AddonItem;
import com.hyp.entity.Item;
import com.hyp.request.PosStockRequest;

@Service
public class StockServiceImpl implements StockService {

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	ItemService itemService;

	@Autowired
	AddonItemService addOnitemService;

	@Override
	public boolean updateStock(PosStockRequest stockRequest) {
		try {
			for (String id : stockRequest.getItemID()) {
				if (stockRequest.getType().equalsIgnoreCase("item")) {
					Item item = itemService.findById(id);
					if (item != null) {
						item.setInStock(stockRequest.isInStock());
						if (!stockRequest.isInStock() && stockRequest.getAutoTurnOnTime().equalsIgnoreCase("custom")) {
							item.setAutoTurnOnTime(LocalDateTime.parse(stockRequest.getCustomTurnOnTime(),
									DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
						} else if (!stockRequest.isInStock()) {
							item.setAutoTurnOnTime(LocalDateTime.parse(stockRequest.getAutoTurnOnTime(),
									DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
						}
						itemService.update(item);
					}
				} else {
					AddonItem addOnItem = addOnitemService.findById(id);
					if (addOnItem != null) {
						addOnItem.setActive(stockRequest.isInStock() ? "1" : "0");
						if (!stockRequest.isInStock() && stockRequest.getAutoTurnOnTime().equalsIgnoreCase("custom")) {
							addOnItem.setAutoTurnOnTime(LocalDateTime.parse(stockRequest.getCustomTurnOnTime(),
									DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
						} else if (!stockRequest.isInStock()) {
							addOnItem.setAutoTurnOnTime(LocalDateTime.parse(stockRequest.getAutoTurnOnTime(),
									DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
						}
						addOnitemService.update(addOnItem);
					}
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
