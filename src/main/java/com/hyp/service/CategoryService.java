package com.hyp.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationPipeline;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import com.hyp.entity.Category;
import com.hyp.repository.CategoryRepository;

@Service
public class CategoryService extends BaseServiceImpl<Category, String> {

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	MongoTemplate mongoTemplate;

	public List<Category> getCategoryItemsById(String categoryId) {

		Criteria criteria = Criteria.where("_id").is(categoryId);
		Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria),
				getCategoryItemsLookupOperation());

		return mongoTemplate.aggregate(aggregation, "categories", Category.class).getMappedResults();
	}

	public List<Category> getAllCategoryItems(String restaurantId) {
		Criteria criteria = Criteria.where("restaurant_id").is(restaurantId);
		Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria),
				getCategoryItemsLookupOperation());
		return mongoTemplate.aggregate(aggregation, "categories", Category.class).getMappedResults();
	}
	
	private LookupOperation getCategoryItemsLookupOperation() {
		AggregationPipeline taxLookUpPipeline = Aggregation
				.newAggregation(
						LookupOperation.newLookup().from("taxes").localField("item_tax").foreignField("_id").as("taxes"))
				.getPipeline();

		return LookupOperation.newLookup().from("items").localField("_id").foreignField("item_category_id")
				.pipeline(taxLookUpPipeline).as("items");
	}
}
