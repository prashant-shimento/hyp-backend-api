package com.hyp.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.hyp.entity.AddonGroup;
import com.hyp.entity.Item;
import com.hyp.entity.Variation;
import com.hyp.repository.ItemRepository;

@Service
public class ItemService extends BaseServiceImpl<Item, String> {
	@Autowired
	ItemRepository itemRepository;

	@Autowired
	MongoTemplate mongoTemplate;

	public List<Variation> getVariationsByItemId(String itemId) {
		try {
			MatchOperation matchOperation = Aggregation.match(Criteria.where("_id").is(itemId));

			LookupOperation lookupVariations = LookupOperation.newLookup().from("variations").localField("variation")
					.foreignField("_id").as("item_variations");

			ProjectionOperation projectVariations = Aggregation.project("item_variations").andExclude("_id");

			Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupVariations, projectVariations);

			AggregationResults<Item> results = mongoTemplate.aggregate(aggregation, "items", Item.class);
			return results.getMappedResults().stream().map(Item::getItemVariations).filter(Objects::nonNull)
					.flatMap(List::stream).collect(Collectors.toList());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<AddonGroup> getAddonsByItemId(String itemId) {
		try {
			MatchOperation matchOperation = Aggregation.match(Criteria.where("_id").is(itemId));

			LookupOperation lookupAddonGroups = LookupOperation.newLookup().from("addon_groups").localField("addon")
					.foreignField("_id").as("item_addons");

			UnwindOperation unwindAddon = Aggregation.unwind("item_addons", true);

			LookupOperation lookupAddonItems = LookupOperation.newLookup().from("addon_items")
					.localField("item_addons.addon_group_items").foreignField("_id").as("item_addons.addon_items");

			GroupOperation regroupAddons = Aggregation.group("_id")
	                .push("item_addons").as("item_addons"); 

			ProjectionOperation projectAddons = Aggregation.project("item_addons").andExclude("_id");

			Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupAddonGroups, unwindAddon,
					lookupAddonItems, regroupAddons, projectAddons);

			AggregationResults<Item> results = mongoTemplate.aggregate(aggregation, "items", Item.class);
			return results.getMappedResults().stream().map(Item::getItemAddons).filter(Objects::nonNull).flatMap(List::stream)
					.collect(Collectors.toList());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}

}
