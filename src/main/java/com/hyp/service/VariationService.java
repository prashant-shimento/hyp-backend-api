package com.hyp.service;

import com.hyp.entity.AddonGroup;
import com.hyp.entity.Variation;
import com.hyp.repository.VariationRepository;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
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
public class VariationService extends BaseServiceImpl<Variation, String> {

    private static final String VARIATION_CACHE = "variationItems";

    @Autowired
    VariationRepository variationRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    AddonGroupService addonGroupService;

    public List<Variation> getVariationsWithAddonGroupsAndItemsById(String variationId) {
        List<Variation> variations =
                mongoTemplate.find(Query.query(Criteria.where("_id").is(variationId)), Variation.class, "variations");
        populateAddonGroups(variations);
        return variations;
    }

    @SuppressWarnings("unchecked")
    public List<Variation> getVariationsWithAddonGroupsAndItems() {
        try {
            return (List<Variation>) cacheService()
                    .getOrLoad(VARIATION_CACHE, "all", List.class, this::loadAllVariationsWithAddonGroupsAndItems);
        } catch (Exception e) {
            log.error("Failed to get variations with addon groups", e);
            return null;
        }
    }

    private List<Variation> loadAllVariationsWithAddonGroupsAndItems() {
        List<Variation> variations =
                mongoTemplate.find(Query.query(Criteria.where("is_deleted").ne(true)), Variation.class, "variations");
        populateAddonGroups(variations);
        return variations;
    }

    /** Populates addon_groups and their addon_items for the given variations using batch queries. */
    void populateAddonGroups(List<Variation> variations) {
        // Collect all addon group IDs from all variations
        Set<String> allAddonGroupIds = new HashSet<>();
        for (Variation variation : variations) {
            if (variation.getAddonGroupId() != null) {
                allAddonGroupIds.addAll(variation.getAddonGroupId());
            }
        }
        if (allAddonGroupIds.isEmpty()) return;

        // Single batch query for all addon groups
        List<AddonGroup> addonGroups = mongoTemplate.find(
                Query.query(Criteria.where("_id").in(allAddonGroupIds)), AddonGroup.class, "addon_groups");

        // Delegate item population to AddonGroupService — single source of truth for this logic
        addonGroupService.populateAddonItems(addonGroups);

        // Distribute populated addon groups to their variations
        Map<String, AddonGroup> addonGroupMap =
                addonGroups.stream().collect(Collectors.toMap(AddonGroup::getId, g -> g));

        for (Variation variation : variations) {
            if (variation.getAddonGroupId() != null) {
                variation.setAddonGroups(variation.getAddonGroupId().stream()
                        .map(addonGroupMap::get)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
        }
    }

    public void updateVariationStock(List<String> variationIds, boolean inStock) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        long findByVariationsStart = System.currentTimeMillis();
        List<Variation> variations = findByIds(variationIds);
        log.info("Fetched variations in {} ms", System.currentTimeMillis() - findByVariationsStart);

        if (variations.isEmpty()) {
            log.warn("No items or variations found for given IDs: {}", variationIds);
            return;
        }
        List<WriteModel<Document>> writeModels = new ArrayList<>();

        for (Variation variation : variations) {
            Bson filter = Filters.eq("_id", variation.getId());
            if (inStock && "1".equals(variation.getActive()) && "1".equals(variation.getStatus())) {
                log.info("Skipping workflow turn-on for variation {} as it is already active", variation.getId());
                continue;
            }

            if (inStock
                    && variation.getAutoTurnOnTime() != null
                    && variation.getAutoTurnOnTime().isAfter(now)) {
                log.info(
                        "Skipping workflow turn-on for item {} as a newer OFF request exists with autoTurnOnTime: {}",
                        variation.getId(),
                        variation.getAutoTurnOnTime());
                continue;
            }

            Document updateFields = new Document()
                    .append("active", inStock ? "1" : "0")
                    .append("status", inStock ? "1" : "0")
                    .append("updated_at", LocalDateTime.now());

            Bson update = new Document("$set", updateFields);
            writeModels.add(new UpdateOneModel<>(filter, update));
        }

        if (!writeModels.isEmpty()) {
            long bulkWriteStart = System.currentTimeMillis();
            mongoTemplate.getCollection("variations").bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
            log.info("Bulk write items completed in {} ms", System.currentTimeMillis() - bulkWriteStart);
        }
    }
}
