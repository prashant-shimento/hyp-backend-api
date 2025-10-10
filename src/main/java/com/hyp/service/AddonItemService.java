package com.hyp.service;

import com.hyp.entity.AddonItem;
import com.hyp.repository.AddonItemRepository;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AddonItemService extends BaseServiceImpl<AddonItem, String> {
    @Autowired
    AddonItemRepository addonItemRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    public void updateAddonItemStock(List<String> addonItemIds, boolean inStock) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        long findByIdsStart = System.currentTimeMillis();
        List<AddonItem> addonItems = findByIds(addonItemIds);
        log.info("Fetched addons in {} ms", System.currentTimeMillis() - findByIdsStart);

        List<WriteModel<Document>> writeModels = new ArrayList<>();

        for (AddonItem addonItem : addonItems) {
            Bson filter = Filters.eq("_id", addonItem.getId());
            if (inStock && "1".equals(addonItem.getActive())) {
                log.info("Skipping workflow turn-on for add-on {} as it is already active", addonItem.getId());
                continue;
            }

            if (inStock
                    && addonItem.getAutoTurnOnTime() != null
                    && addonItem.getAutoTurnOnTime().isAfter(now)) {
                log.info(
                        "Skipping workflow turn-on for add-on {} as a newer OFF request exists with autoTurnOnTime: {}",
                        addonItem.getId(),
                        addonItem.getAutoTurnOnTime());
                continue;
            }

            Document updateFields =
                    new Document().append("active", inStock ? "1" : "0").append("updated_at", LocalDateTime.now());

            Bson update = new Document("$set", updateFields);
            writeModels.add(new UpdateOneModel<>(filter, update));
        }

        if (!writeModels.isEmpty()) {
            long bulkWriteStart = System.currentTimeMillis();
            mongoTemplate.getCollection("addon_items").bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
            log.info("Bulk write items completed in {} ms", System.currentTimeMillis() - bulkWriteStart);
        }
    }
}
