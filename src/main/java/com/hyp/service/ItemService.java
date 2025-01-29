package com.hyp.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bson.Document;
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

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
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

			UnwindOperation unwindVariations = Aggregation.unwind("item_variations", true);

			LookupOperation lookupAddonGroups = LookupOperation.newLookup().from("addon_groups")
					.localField("item_variations.addon_group_id").foreignField("_id")
					.as("item_variations.addon_groups");

			UnwindOperation unwindAddonGroups = Aggregation.unwind("item_variations.addon_groups", true);

			LookupOperation lookupAddonItems = LookupOperation.newLookup().from("addon_items")
					.localField("item_variations.addon_groups.addon_group_items").foreignField("_id")
					.as("item_variations.addon_groups.addon_items");

			GroupOperation groupVariations = Aggregation.group("item_variations._id").first("item_variations._id")
					.as("id").first("item_variations.name").as("name").first("item_variations.group_name")
					.as("group_name").first("item_variations.price").as("price").first("item_variations.status")
					.as("status").first("item_variations.active").as("active")
					.first("item_variations.item_packing_charges").as("item_packing_charges")
					.first("item_variations.variation_rank").as("variation_rank")
					.first("item_variations.variation_allow_addon").as("variation_allow_addon")
					.first("item_variations.variation_id").as("variation_id").first("item_variations.restaurant_id")
					.as("restaurant_id").push("item_variations.addon_groups").as("addon_groups");

			GroupOperation groupAll = Aggregation.group().push(new Document("id", "$id").append("name", "$name")
					.append("group_name", "$group_name").append("price", "$price").append("status", "$status")
					.append("active", "$active").append("item_packing_charges", "$item_packing_charges")
					.append("variation_rank", "$variation_rank")
					.append("variation_allow_addon", "$variation_allow_addon").append("variation_id", "$variation_id")
					.append("restaurant_id", "$restaurant_id").append("addon_groups", "$addon_groups"))
					.as("item_variations");

			Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupVariations, unwindVariations,
					lookupAddonGroups, unwindAddonGroups, lookupAddonItems, groupVariations, groupAll);

			AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "items", Document.class);
			return (List<Variation>) results.getMappedResults().stream()
					.map(doc -> doc.getList("item_variations", Document.class)).flatMap(List::stream)
					.collect(Collectors.collectingAndThen(Collectors.toList(), this::mapToVariations));
		} catch (Exception e) {
			log.error("Exception occurred in getVariationsByItemId for itemid {} cause :", itemId, e.getMessage());
			return Collections.emptyList();
		}
	}

	public List<AddonGroup> getAddonsByItemId(String itemId) {
		try {
			MatchOperation matchOperation = Aggregation
					.match(Criteria.where("_id").is(itemId).and("addon").exists(true).ne(Collections.emptyList()));

			LookupOperation lookupAddonGroups = LookupOperation.newLookup().from("addon_groups").localField("addon")
					.foreignField("_id").as("item_addons");

			UnwindOperation unwindAddon = Aggregation.unwind("item_addons", true);

			LookupOperation lookupAddonItems = LookupOperation.newLookup().from("addon_items")
					.localField("item_addons.addon_group_items").foreignField("_id").as("item_addons.addon_items");

			GroupOperation regroupAddons = Aggregation.group("_id").push("item_addons").as("item_addons");

			ProjectionOperation projectAddons = Aggregation.project("item_addons").andExclude("_id");

			Aggregation aggregation = Aggregation.newAggregation(matchOperation, lookupAddonGroups, unwindAddon,
					lookupAddonItems, regroupAddons, projectAddons);

			AggregationResults<Item> results = mongoTemplate.aggregate(aggregation, "items", Item.class);
			return results.getMappedResults().stream().map(Item::getItemAddons).filter(Objects::nonNull)
					.flatMap(List::stream).collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Exception occurred in getAddonsByItemId for itemid {} cause :", itemId, e.getMessage());
			return Collections.emptyList();
		}

	}

	private List<Variation> mapToVariations(List<Document> itemVariations) {
		return itemVariations.stream().map(variationDoc -> {
			Variation variation = new Variation();
			variation.setId(variationDoc.getString("id")); // Explicitly set the id field
			variation.setName(variationDoc.getString("name"));
			variation.setGroupName(variationDoc.getString("group_name"));
			variation.setPrice(variationDoc.getString("price"));
			variation.setStatus(variationDoc.getString("status"));
			variation.setActive(variationDoc.getString("active"));
			variation.setItemPackingCharges(variationDoc.getString("item_packing_charges"));
			variation.setVariationRank(variationDoc.getString("variation_rank"));
			variation.setVariationAllowAddon(variationDoc.getInteger("variation_allow_addon"));
			variation.setVariationId(variationDoc.getString("variation_id"));
			variation.setRestaurantId(variationDoc.getString("restaurant_id"));

			List<Document> addonGroups = variationDoc.getList("addon_groups", Document.class);
			if (addonGroups != null) {
				List<AddonGroup> mappedAddonGroups = addonGroups.stream().map(addonGroupDoc -> {
					AddonGroup addonGroup = new AddonGroup();
					return addonGroup;
				}).collect(Collectors.toList());
				variation.setAddonGroups(mappedAddonGroups);
			}

			return variation;
		}).collect(Collectors.toList());
	}

}
