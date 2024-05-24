package com.hyp.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.hyp.constants.Constants;

public class QueryUtils {

	public static final Map<String, List<String>> ALLOWED_API_PARAMS = new HashMap<>();

	static {
		ALLOWED_API_PARAMS.put("Order", Constants.ORDER_API_PARAMS);
		ALLOWED_API_PARAMS.put("Customer", Constants.CUSTOMER_API_PARAMS);
		ALLOWED_API_PARAMS.put("Item", Constants.ITEM_API_PARAMS);
		ALLOWED_API_PARAMS.put("Address", Constants.ADDRESS_API_PARAMS);
		ALLOWED_API_PARAMS.put("Delivery", Constants.DELIVERY_API_PARAMS);
		ALLOWED_API_PARAMS.put("Content", Constants.CONTENT_API_PARAMS);
		ALLOWED_API_PARAMS.put("Variation", Constants.VARIATIONS_API_PARAMS);

	}

	public static Query getFilterQuery(Map<String, String> requestParam, List<String> allowedParams) {
		Integer limit = null;
		Integer offset = null;

		for (String key : requestParam.keySet()) {
			if (!key.equalsIgnoreCase("limit") && !key.equalsIgnoreCase("offset")) {
				String[] parts = key.split("_");
				if (parts.length != 2 || !allowedParams.contains(parts[0]) || !isValidQueryParamOperator(parts[1])) {
					return null;
				}
			}
		}

		Query query = new Query();

		for (Map.Entry<String, String> entry : requestParam.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();

			if (key.equalsIgnoreCase("limit")) {
				limit = Integer.parseInt(value);
			}
			if (key.equalsIgnoreCase("offset")) {
				offset = Integer.parseInt(value);
			}

			

			String[] parts = key.split("_");
			String fieldName = parts[0];
			String operator = parts[1];

			switch (operator) {
			case "eq":
				query.addCriteria(Criteria.where(fieldName).is(value));
				break;
			case "in":
				query.addCriteria(Criteria.where(fieldName).in((Object[]) value.split(",")));
				break;
			case "nin":
				query.addCriteria(Criteria.where(fieldName).nin((Object[]) value.split(",")));
				break;
			case "neq":
				query.addCriteria(Criteria.where(fieldName).ne(value));
				break;
			case "gt":
				query.addCriteria(Criteria.where(fieldName).gt(value));
				break;
			case "lt":
				query.addCriteria(Criteria.where(fieldName).lt(value));
				break;
			case "gte":
				query.addCriteria(Criteria.where(fieldName).gte(value));
				break;
			case "lte":
				query.addCriteria(Criteria.where(fieldName).lte(value));
				break;
			case "like":
				query.addCriteria(Criteria.where(fieldName).regex(".*" + value + ".*"));
				break;
			case "nlike":
				query.addCriteria(Criteria.where(fieldName).not().regex(".*" + value + ".*"));
				break;
			default:
				return null;
			}
		}

		query.limit(limit != null ? limit : 10);
		query.skip(offset != null ? offset * limit : 0);
		
		return query;

	}

	public static boolean isValidQueryParamOperator(String operator) {
		return Arrays.asList("eq", "neq", "in", "nin", "gte", "gt", "lte", "lt", "like", "nlike").contains(operator);
	}

	public static List<String> getAllowedParameters(String className) {
		return ALLOWED_API_PARAMS.getOrDefault(className, Arrays.asList("id"));
	}
}
