package com.hyp.service;

import java.util.List;

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

}
