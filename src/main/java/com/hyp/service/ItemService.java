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

import com.hyp.entity.Item;
import com.hyp.repository.ItemRepository;

import lombok.Data;

@Service
@Data
public class ItemService extends BaseServiceImpl<Item, String> {
	@Autowired
	ItemRepository itemRepository;

	private final MongoTemplate mongoTemplate;

	public List<Item> getItems(Map<String, Object> parameters, Integer page, Integer pageSize, String sortBy) {
		Query query = new Query();

		List<Criteria> criteriaList = new ArrayList<>();
		for (Map.Entry<String, Object> entry : parameters.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			switch (key) {
			case "itemName":
				if (value instanceof List) {
					@SuppressWarnings("unchecked")
					List<String> itemNames = (List<String>) value;
					criteriaList.add(Criteria.where("itemName").in(itemNames));
				} else if (value instanceof String) {
					String itemName = (String) value;
					criteriaList.add(Criteria.where("itemName").in(Arrays.asList(itemName.split(","))));
				}
				break;
			case "itemRanks":
				if (value instanceof List) {
					@SuppressWarnings("unchecked")
					List<String> itemNames = (List<String>) value;
					criteriaList.add(Criteria.where("itemRank").in(itemNames));
				} else if (value instanceof String) {
					String itemName = (String) value;
					criteriaList.add(Criteria.where("itemRank").in(Arrays.asList(itemName.split(","))));
				}
				break;

			// this price is not giving output as expected, need to visit again....
			case "priceRange":
				if (value instanceof List && ((List<?>) value).size() == 2) {
					@SuppressWarnings("unchecked")
					List<Integer> priceRange = (List<Integer>) value;
					criteriaList.add(Criteria.where("price").gte(priceRange.get(0)).lte(priceRange.get(1)));
				}
				break;
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

		Pageable pageable;
		if (page != null && pageSize != null) {
			pageable = PageRequest.of(page, pageSize);
			query.with(pageable);
		} else {
			pageable = PageRequest.of(0, Integer.MAX_VALUE);
		}

		return mongoTemplate.find(query, Item.class);
	}

}
