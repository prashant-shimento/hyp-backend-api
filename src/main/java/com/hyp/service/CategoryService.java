package com.hyp.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.hyp.entity.Category;
import com.hyp.repository.CategoryRepository;

import lombok.Data;

@Service
@Data
public class CategoryService extends BaseServiceImpl<Category, String> {

	@Autowired
	CategoryRepository categoryRepository;

	private final MongoTemplate mongoTemplate;

	public List<Category> getCategories(Map<String, Object> parameters, Integer page, Integer pageSize, String sortBy) {
		Query query = new Query();

		List<Criteria> criteriaList = new ArrayList<>();
		for (Map.Entry<String, Object> entry : parameters.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			switch (key) {
			case "categoryName":
				if (value instanceof List) {
					@SuppressWarnings("unchecked")
					List<String> categoryNames = (List<String>) value;
					criteriaList.add(Criteria.where("categoryName").in(categoryNames));
				} else if (value instanceof String) {
					String categoryName = (String) value;
					criteriaList.add(Criteria.where("categoryName").in(Arrays.asList(categoryName.split(","))));
				}
				break;
				
				// here we can add more no. of case as per requirement
				
			}
		}

		if (!criteriaList.isEmpty()) {
			Criteria[] criteriaArray = criteriaList.toArray(new Criteria[0]);
			query.addCriteria(new Criteria().andOperator(criteriaArray));
		}

		if (sortBy != null && !sortBy.isEmpty()) {
			String[] sortParams = sortBy.split(",");
			for (String sortParam : sortParams) {
				String[] parts = sortParam.split(":");
				String field = parts[0].trim();
				String order = parts.length > 1 ? parts[1].trim() : "asc";
				query.with(Sort.by(order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, field));
			}
		}

		Pageable pageable = null;
		if (page != null && pageSize != null) {
			pageable = PageRequest.of(page, pageSize);
			query.with(pageable);
		} else {
			pageable = PageRequest.of(0, Integer.MAX_VALUE);
		}

		return mongoTemplate.find(query, Category.class);
	}

}
