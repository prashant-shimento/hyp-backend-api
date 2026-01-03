package com.hyp.service;

import com.hyp.entity.Variation;
import com.hyp.repository.VariationRepository;
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
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationPipeline;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VariationService extends BaseServiceImpl<Variation, String> {

    @Autowired
    VariationRepository variationRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    public List<Variation> getVariationsWithAddonGroupsAndItemsById(String variationId) {
        Criteria criteria = Criteria.where("_id").is(variationId);

        Aggregation aggregation =
                Aggregation.newAggregation(getVariationsAddonsLookupOperation(), Aggregation.match(criteria));

        AggregationResults<Variation> results = mongoTemplate.aggregate(aggregation, "variations", Variation.class);
        return results.getMappedResults();
    }

    public List<Variation> getVariationsWithAddonGroupsAndItems() {
        try {

            Aggregation aggregation = Aggregation.newAggregation(getVariationsAddonsLookupOperation());

            AggregationResults<Variation> results = mongoTemplate.aggregate(aggregation, "variations", Variation.class);
            return results.getMappedResults();
        } catch (Exception e) {
            log.error("Failed to get variations with addon groups", e);
            return null;
        }
    }

    private LookupOperation getVariationsAddonsLookupOperation() {
        AggregationPipeline addonItemsLookupPipeline = Aggregation.newAggregation(LookupOperation.newLookup()
                        .from("addon_items")
                        .localField("addon_group_items")
                        .foreignField("_id")
                        .as("addon_items"))
                .getPipeline();

        return LookupOperation.newLookup()
                .from("addon_groups")
                .localField("addon_group_id")
                .foreignField("_id")
                .pipeline(addonItemsLookupPipeline)
                .as("addon_groups");
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
            if (inStock && "1".equals(variation.getActive())) {
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

            Document updateFields =
                    new Document().append("active", inStock ? "1" : "0").append("updated_at", LocalDateTime.now());

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
