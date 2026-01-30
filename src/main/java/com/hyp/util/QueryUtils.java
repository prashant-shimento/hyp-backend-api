package com.hyp.util;

import com.hyp.constants.Constants;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@Slf4j
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
        ALLOWED_API_PARAMS.put("Category", Constants.CATEGORIES_API_PARAMS);
        ALLOWED_API_PARAMS.put("Partner", Constants.PARTNER_API_PARAMS);
        ALLOWED_API_PARAMS.put("Restaurant", Constants.RESTAURANT_API_PARAMS);
        ALLOWED_API_PARAMS.put("Payment", Constants.PAYMENT_API_PARAMS);
        ALLOWED_API_PARAMS.put("OrderType", Constants.ORDER_TYPE_API_PARAMS);
        ALLOWED_API_PARAMS.put("CustomerTestimonial", Constants.CUSTOMER_TESTIMONIAL_PARAMS);
        ALLOWED_API_PARAMS.put("Fee", Constants.FEE_API_PARAMS);
        ALLOWED_API_PARAMS.put("Settlement", Constants.SETTLEMENT_API_PARAMS);
        ALLOWED_API_PARAMS.put("ReferralCode", Constants.REFERRAL_CODE_API_PARAMS);
        ALLOWED_API_PARAMS.put("ReferralToken", Constants.REFERRAL_TOKEN_API_PARAMS);
        ALLOWED_API_PARAMS.put("Tax", Constants.TAX_API_PARAMS);
        ALLOWED_API_PARAMS.put("User", Constants.USERS_PARAM);
    }

    public static Query getFilterQuery(Map<String, String> requestParam, List<String> allowedParams) {
        int limit = 10;
        int offset = 0;
        Sort sort = null;
        String sortField = requestParam.get("sortField");

        if (sortField != null && allowedParams.contains(sortField)) {
            sort = Sort.by(Sort.Direction.DESC, sortField);
        }

        for (String key : requestParam.keySet()) {
            if (!key.equalsIgnoreCase("limit")
                    && !key.equalsIgnoreCase("offset")
                    && !key.equalsIgnoreCase("sortField")) {
                String[] parts = key.split("_");
                if (parts.length != 2 || !allowedParams.contains(parts[0]) || !isValidQueryParamOperator(parts[1])) {
                    return null;
                }
            }
        }

        Query query = new Query();

        if (sort != null) {
            query.with(sort);
        }

        for (Map.Entry<String, String> entry : requestParam.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.equalsIgnoreCase("limit")) {
                limit = Integer.parseInt(value);
            }
            if (key.equalsIgnoreCase("offset")) {
                offset = Integer.parseInt(value);
            }

            if (!key.equalsIgnoreCase("limit")
                    && !key.equalsIgnoreCase("offset")
                    && !key.equalsIgnoreCase("sortField")) {

                String[] parts = key.split("_");

                String fieldName = parts[0];
                String operator = parts[1];

                Object parsedValue = parseValue(fieldName, value, operator);
                if (parsedValue == null) {
                    return query;
                }

                switch (operator) {
                    case "eq":
                        query.addCriteria(Criteria.where(fieldName).is(parsedValue));
                        break;
                    case "in":
                        query.addCriteria(Criteria.where(fieldName).in((Object[]) parsedValue));
                        break;
                    case "nin":
                        query.addCriteria(Criteria.where(fieldName).nin((Object[]) parsedValue));
                        break;
                    case "neq":
                        query.addCriteria(Criteria.where(fieldName).ne(parsedValue));
                        break;
                    case "gt":
                        query.addCriteria(Criteria.where(fieldName).gt(parsedValue));
                        break;
                    case "lt":
                        query.addCriteria(Criteria.where(fieldName).lt(parsedValue));
                        break;
                    case "gte":
                        query.addCriteria(Criteria.where(fieldName).gte(parsedValue));
                        break;
                    case "lte":
                        query.addCriteria(Criteria.where(fieldName).lte(parsedValue));
                        break;
                    case "range":
                        Date[] dateRange = (Date[]) parsedValue;
                        query.addCriteria(
                                Criteria.where(fieldName).gte(dateRange[0]).lte(dateRange[1]));
                        query.with(Sort.by(Sort.Direction.ASC, fieldName));
                        break;
                    case "like":
                        String patternValue = String.valueOf(parsedValue);
                        query.addCriteria(Criteria.where(fieldName)
                                .regex(Pattern.compile(
                                        ".*" + Pattern.quote(patternValue) + ".*", Pattern.CASE_INSENSITIVE)));
                        break;
                    case "nlike":
                        String patternValues = String.valueOf(parsedValue);
                        query.addCriteria(Criteria.where(fieldName)
                                .not()
                                .regex(Pattern.compile(
                                        ".*" + Pattern.quote(patternValues) + ".*", Pattern.CASE_INSENSITIVE)));
                        break;
                    default:
                        return query;
                }
            }
        }
        query.limit(limit);
        query.skip(Math.multiplyExact(offset, limit));

        return query;
    }

    public static boolean isValidQueryParamOperator(String operator) {
        return Arrays.asList("eq", "neq", "in", "nin", "gte", "gt", "lte", "range", "lt", "like", "nlike")
                .contains(operator);
    }

    public static List<String> getAllowedParameters(String className) {
        return ALLOWED_API_PARAMS.getOrDefault(className, List.of("id"));
    }

    private static Object parseValue(String fieldName, String value, String operator) {
        if (Constants.DATE_API_PARAMS.contains(fieldName)) {
            if ("range".equals(operator)) {
                String[] dateRange = value.split(",");
                if (dateRange.length == 2) {
                    Date startDate = CommonUtils.getISODate(dateRange[0], Constants.SUPPORTED_DATE_FORMATS);
                    Date endDate = CommonUtils.getISODate(dateRange[1], Constants.SUPPORTED_DATE_FORMATS);
                    return new Date[] {startDate, endDate};
                }
            }
            return CommonUtils.getISODate(value, Constants.SUPPORTED_DATE_FORMATS);
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        switch (operator) {
            case "eq":
            case "neq":
            case "like":
            case "nlike":
                return value;
            case "in":
            case "nin":
                return value.split(",");
            case "gt":
            case "lt":
            case "gte":
            case "lte":
            case "range":
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    log.error("Exception occurred on parseValue {}", e.getMessage());
                    return null;
                }
            default:
                return null;
        }
    }
}
