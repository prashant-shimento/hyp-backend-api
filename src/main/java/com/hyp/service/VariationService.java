package com.hyp.service;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.hyp.entity.Variation;
import com.hyp.repository.VariationRepository;

@Service
public class VariationService extends BaseServiceImpl<Variation, String> {

	@Autowired
	VariationRepository variationRepository;
	@Autowired
	MongoTemplate mongoTemplate;

	public List<Document> getVariationsWithAddonGroupsAndItems() {
		List<Document> pipeline = buildPipelineWithoutId();
		return executePipeline(pipeline);
	}

	public List<Document> getVariationsWithAddonGroupsAndItemsById(String variationId) {
		List<Document> pipeline = buildPipelineWithoutId();
		Document variationMatchStage = new Document("$match", new Document("_id", variationId));
		pipeline.add(variationMatchStage);
		return executePipeline(pipeline);
	}

	private List<Document> executePipeline(List<Document> pipeline) {
		return mongoTemplate.getCollection("variations").aggregate(pipeline, Document.class).into(new ArrayList<>());
	}

	private List<Document> buildPipelineWithoutId() {
		Document matchStage = new Document("$match", new Document("addon_group_id", new Document("$size", 1)));

		Document lookupStage = new Document("$lookup", new Document("from", "addon_groups")
				.append("localField", "addon_group_id").append("foreignField", "_id").append("as", "addon_groups"));

		Document unwindStage = new Document("$unwind",
				new Document("path", "$addon_groups").append("preserveNullAndEmptyArrays", true));

		Document lookupAddonItemsStage = new Document("$lookup",
				new Document("from", "addon_items").append("localField", "addon_groups.addon_group_items")
						.append("foreignField", "_id").append("as", "addon_groups.addon_items"));

		List<Document> pipeline = new ArrayList<>();
		pipeline.add(matchStage);
		pipeline.add(lookupStage);
		pipeline.add(unwindStage);
		pipeline.add(lookupAddonItemsStage);
		return pipeline;
	}

}
