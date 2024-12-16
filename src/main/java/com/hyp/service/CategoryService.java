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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CategoryService extends BaseServiceImpl<Category, String> {

	@Autowired
	CategoryRepository categoryRepository;

	@Autowired
	MongoTemplate mongoTemplate;

	public List<Category> getCategoryItemsById(String categoryId) {

		Criteria criteria = Criteria.where("_id").is(categoryId).andOperator(Criteria.where("is_deleted").is(false));
		Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria),
				getCategoryItemsLookupOperation());

		return mongoTemplate.aggregate(aggregation, "categories", Category.class).getMappedResults();
	}

	public List<Category> getAllCategoryItems(String restaurantId) {
		Criteria criteria = Criteria.where("restaurant_id").is(restaurantId).andOperator(Criteria.where("is_deleted").is(false));
		long startTime = System.currentTimeMillis();
		Aggregation aggregation = Aggregation.newAggregation(Aggregation.match(criteria),
				getCategoryItemsLookupOperation());
		List<Category> category = mongoTemplate.aggregate(aggregation, "categories", Category.class).getMappedResults();
		long endTime = System.currentTimeMillis(); 
		long executionTime = endTime - startTime;
		log.info("Query Execution Time for getAllCategoryItems: " + executionTime + " ms");
		return category;
	}

	private LookupOperation getCategoryItemsLookupOperation() {
		AggregationPipeline taxLookUpPipeline = Aggregation.newAggregation(
				Aggregation.match(Criteria.where("is_deleted").is(false)),
				LookupOperation.newLookup().from("taxes").localField("item_tax").foreignField("_id").as("taxes"))
				.getPipeline();

		return LookupOperation.newLookup().from("items").localField("_id").foreignField("item_category_id")
				.pipeline(taxLookUpPipeline).as("items");
	}
}
