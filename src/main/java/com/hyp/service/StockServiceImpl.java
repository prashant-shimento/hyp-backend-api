package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.AddonItem;
import com.hyp.entity.Item;
import com.hyp.request.StockRequest;

@Service
public class StockServiceImpl implements StockService {

	@Autowired
	RestaurantService restaurantService;
	
	@Autowired
	ItemService itemService;
	
	@Autowired
	AddonItemService addOnitemService;
	
	@Override
	public boolean updateStock(StockRequest stockRequest) {
		try {
			for (String id : stockRequest.getItemID()) {
				if (stockRequest.getType().equalsIgnoreCase("item")) {
					Item item = itemService.findById(id);
					if(item != null) {
						item.setInStock(stockRequest.isInStock());
						itemService.update(item);
					}
				} else {
					AddonItem addOnItem = addOnitemService.findById(id);
					if(addOnItem != null) {
						addOnItem.setActive(stockRequest.isInStock() ? "1" : "0");
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
