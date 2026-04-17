package com.hyp.service;

import com.hyp.entity.AddonGroup;
import com.hyp.entity.AddonItem;
import com.hyp.entity.Item;
import com.hyp.entity.Variation;
import com.hyp.repository.ItemRepository;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ItemService extends BaseServiceImpl<Item, String> {

    @Override
    protected String cacheName() {
        return "items";
    }

    @Override
    protected Class<Item> entityType() {
        return Item.class;
    }

    @Autowired
    ItemRepository itemRepository;

    @Autowired
    VariationService variationService;

    @Autowired
    MongoTemplate mongoTemplate;

    public List<Variation> getVariationsByItemId(String itemId) {
        try {
            Item item = mongoTemplate.findById(itemId, Item.class, "items");
            if (item == null
                    || item.getVariation() == null
                    || item.getVariation().isEmpty()) {
                return Collections.emptyList();
            }

            List<Variation> variations = mongoTemplate.find(
                    Query.query(Criteria.where("_id").in(item.getVariation())), Variation.class, "variations");

            variationService.populateAddonGroups(variations);
            return variations;
        } catch (Exception e) {
            log.error("Exception occurred in getVariationsByItemId for item id {} cause : {}", itemId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<AddonGroup> getAddonsByItemId(String itemId) {
        try {
            Item item = mongoTemplate.findById(itemId, Item.class, "items");
            if (item == null || item.getAddon() == null || item.getAddon().isEmpty()) {
                return Collections.emptyList();
            }

            List<AddonGroup> addonGroups = mongoTemplate.find(
                    Query.query(Criteria.where("_id").in(item.getAddon())), AddonGroup.class, "addon_groups");

            populateAddonItems(addonGroups);
            return addonGroups;
        } catch (Exception e) {
            log.error("Exception occurred in getAddonsByItemId for item id {} cause : {}", itemId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private void populateAddonItems(List<AddonGroup> groups) {
        Set<String> allItemIds = new HashSet<>();
        for (AddonGroup group : groups) {
            if (group.getAddonGroupItems() != null) {
                allItemIds.addAll(group.getAddonGroupItems());
            }
        }
        if (allItemIds.isEmpty()) return;

        Map<String, AddonItem> itemMap =
                mongoTemplate
                        .find(Query.query(Criteria.where("_id").in(allItemIds)), AddonItem.class, "addon_items")
                        .stream()
                        .collect(Collectors.toMap(AddonItem::getId, Function.identity()));

        for (AddonGroup group : groups) {
            if (group.getAddonGroupItems() != null) {
                group.setAddonItems(group.getAddonGroupItems().stream()
                        .map(itemMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
        }
    }

    public void updateItemStock(List<String> itemList, boolean inStock) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        long findByIdsStart = System.currentTimeMillis();
        List<Item> items = findByIds(itemList);
        log.info("Fetched items in {} ms", System.currentTimeMillis() - findByIdsStart);

        if (items.isEmpty()) {
            log.info("No items found. Checking variations...");
            variationService.updateVariationStock(itemList, inStock);
            return;
        }

        List<WriteModel<Document>> writeModels = new ArrayList<>();

        for (Item item : items) {
            Bson filter = Filters.eq("_id", item.getId());
            if (inStock && "1".equals(item.getActive())) {
                log.info("Skipping workflow turn-on for item {} as it is already active", item.getId());
                continue;
            }

            if (inStock
                    && item.getAutoTurnOnTime() != null
                    && item.getAutoTurnOnTime().isAfter(now)) {
                log.info(
                        "Skipping workflow turn-on for item {} as a newer OFF request exists with autoTurnOnTime: {}",
                        item.getId(),
                        item.getAutoTurnOnTime());
                continue;
            }

            Document updateFields = new Document()
                    .append("in_stock", inStock)
                    .append("active", inStock ? "1" : "0")
                    .append("updated_at", LocalDateTime.now());

            Bson update = new Document("$set", updateFields);
            writeModels.add(new UpdateOneModel<>(filter, update));
        }

        if (!writeModels.isEmpty()) {
            long bulkWriteStart = System.currentTimeMillis();
            mongoTemplate.getCollection("items").bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
            log.info("Bulk write items completed in {} ms", System.currentTimeMillis() - bulkWriteStart);
        }
    }
}
