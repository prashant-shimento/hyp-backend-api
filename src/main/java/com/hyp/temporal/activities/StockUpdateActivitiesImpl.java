package com.hyp.temporal.activities;

import com.hyp.service.AddonItemService;
import com.hyp.service.ItemService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StockUpdateActivitiesImpl implements StockUpdateActivities {

    @Autowired
    AddonItemService addonItemService;

    @Autowired
    ItemService itemService;

    @Override
    public void updateStock(List<String> itemIds, String type, boolean inStock) {
        switch (type.toLowerCase()) {
            case "item":
                itemService.updateItemStock(itemIds, inStock);
                break;
            case "addon":
                addonItemService.updateAddonItemStock(itemIds, inStock);
                break;
            default:
                throw new IllegalArgumentException("Unsupported item type: " + type);
        }
        log.info("Updated stock type={} count={} inStock={}", type, itemIds.size(), inStock);
    }
}
