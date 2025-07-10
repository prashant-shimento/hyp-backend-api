package com.hyp.service;

import java.time.LocalDateTime;
import java.util.List;

import com.hyp.request.PosStockRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationPipeline;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.hyp.entity.Variation;
import com.hyp.repository.VariationRepository;

@Service
@Slf4j
public class VariationService extends BaseServiceImpl<Variation, String> {

	@Autowired
	VariationRepository variationRepository;
	
	@Autowired
	MongoTemplate mongoTemplate;

	public List<Variation> getVariationsWithAddonGroupsAndItemsById(String variationId) {
		Criteria criteria = Criteria.where("_id").is(variationId);

		Aggregation aggregation = Aggregation.newAggregation(getVariationsAddonsLookupOperation(),
				Aggregation.match(criteria));

		AggregationResults<Variation> results = mongoTemplate.aggregate(aggregation, "variations", Variation.class);
		return results.getMappedResults();

	}

	public List<Variation> getVariationsWithAddonGroupsAndItems() {
		try {

			Aggregation aggregation = Aggregation.newAggregation(getVariationsAddonsLookupOperation());

			AggregationResults<Variation> results = mongoTemplate.aggregate(aggregation, "variations", Variation.class);
			return results.getMappedResults();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private LookupOperation getVariationsAddonsLookupOperation() {
		AggregationPipeline addonItemsLookupPipeline = Aggregation.newAggregation(LookupOperation.newLookup()
				.from("addon_items").localField("addon_group_items").foreignField("_id").as("addon_items"))
				.getPipeline();

		return LookupOperation.newLookup().from("addon_groups").localField("addon_group_id").foreignField("_id")
				.pipeline(addonItemsLookupPipeline).as("addon_groups");
	}

	public void updateVariationStock(List<String> variationIds, boolean inStock) {
		long findByVariationsStart = System.currentTimeMillis();
		List<Variation> variations = findByIds(variationIds);
		log.info("Fetched variations in {} ms", System.currentTimeMillis() - findByVariationsStart);

		if (variations.isEmpty()) {
			log.warn("No items or variations found for given IDs: {}", variationIds);
			return;
		}

		variations.forEach(variation -> {
			variation.setActive(inStock ? "1" : "0");
		});

		long bulkWriteStart = System.currentTimeMillis();
		bulkUpdate(variations, Variation.class);
		log.info("Bulk write for variations completed in {} ms", System.currentTimeMillis() - bulkWriteStart);
	}

}
