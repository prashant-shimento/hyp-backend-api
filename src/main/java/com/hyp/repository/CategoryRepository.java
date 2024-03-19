package com.hyp.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.hyp.entity.Category;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {

	@Aggregation("{ $lookup: { from: 'items', localField: '_id', foreignField: 'item_category_id', as: 'items' } }")
	List<Category> getCategoryItems();
}
