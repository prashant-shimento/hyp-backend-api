package com.hyp.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;

import com.hyp.entity.AddonGroup;
import com.hyp.repository.AddonGroupRepository;

@Service
public class AddonGroupService extends BaseServiceImpl<AddonGroup, String> {
	@Autowired
	AddonGroupRepository addonGroupRepository;

	@Autowired
	MongoTemplate mongoTemplate;

	public List<AddonGroup> getAddonGroupsDetails(String addonGroupId) {

		LookupOperation lookupOperation = LookupOperation.newLookup().from("addon_items")
				.localField("addon_group_items").foreignField("_id").as("addon_group_items");

		UnwindOperation unwindOperation = Aggregation.unwind("addon_group_items");

		// Match stage to filter by id if provided
		Criteria criteria = Criteria.where("_id").is(addonGroupId);
		MatchOperation matchOperation = Aggregation.match(criteria);

		GroupOperation groupOperation = Aggregation.group("_id").first("addon_group_name").as("addon_group_name")
				.first("active").as("active").first("addon_group_rank").as("addon_group_rank").push("addon_group_items")
				.as("addon_items").first("addon_item_selection_max").as("addon_item_selection_max")
				.first("addon_item_selection_min").as("addon_item_selection_min").first("updated_at").as("updated_at")
				.first("restaurant_id").as("restaurant_id").first("_class").as("_class");

		ProjectionOperation projectOperation = Aggregation.project("_id", "addon_group_name", "active",
				"addon_group_rank", "addon_items", "addon_item_selection_max", "addon_item_selection_min", "updated_at",
				"restaurant_id", "_class");

		Aggregation aggregation = null;
		if (addonGroupId != null && !addonGroupId.isEmpty()) {
			aggregation = Aggregation.newAggregation(lookupOperation, unwindOperation, matchOperation, groupOperation,
					projectOperation);
		} else {
			aggregation = Aggregation.newAggregation(lookupOperation, unwindOperation, groupOperation,
					projectOperation);
		}

		AggregationResults<AddonGroup> results = mongoTemplate.aggregate(aggregation, "addon_groups", AddonGroup.class);
		return results.getMappedResults();
	}

	public List<AddonGroup> getAddonGroupsAndItemsById(String addonGroupId) {
		return getAddonGroupsDetails(addonGroupId);
	}

	public List<AddonGroup> getAddonGroupsAndItems() {
		return getAddonGroupsDetails(null);
	}

}
