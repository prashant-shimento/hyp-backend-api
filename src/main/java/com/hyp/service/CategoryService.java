package com.hyp.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
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
		LookupOperation lookupOperation = createLookupOperation();

		Aggregation aggregation;
		if (categoryId != null && !categoryId.isEmpty()) {
			Criteria criteria = Criteria.where("_id").is(categoryId);
			aggregation = Aggregation.newAggregation(Aggregation.match(criteria), lookupOperation);
		} else {
			aggregation = Aggregation.newAggregation(lookupOperation);
		}

		return mongoTemplate.aggregate(aggregation, "categories", Category.class).getMappedResults();
	}

	public List<Category> getAllCategoryItems() {
		LookupOperation lookupOperation = createLookupOperation();
		Aggregation aggregation = Aggregation.newAggregation(lookupOperation);
		return mongoTemplate.aggregate(aggregation, "categories", Category.class).getMappedResults();
	}

	private LookupOperation createLookupOperation() {
		return LookupOperation.newLookup().from("items").localField("_id").foreignField("item_category_id").as("items");
	}
}
