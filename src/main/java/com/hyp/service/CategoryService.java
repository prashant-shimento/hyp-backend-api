package com.hyp.service;

import com.hyp.entity.Category;
import com.hyp.entity.Item;
import com.hyp.repository.CategoryRepository;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationPipeline;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CategoryService extends BaseServiceImpl<Category, String> {

    private static final String CACHE_NAME = "categories";
    private static final String MENU_CACHE = "categoryMenu";

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    protected String cacheName() {
        return CACHE_NAME;
    }

    @Override
    protected Class<Category> entityType() {
        return Category.class;
    }

    public List<Category> getCategoryItemsById(String categoryId) {

        Criteria criteria = Criteria.where("_id")
                .is(categoryId)
                .andOperator(Criteria.where("is_deleted").is(false));
        Aggregation aggregation =
                Aggregation.newAggregation(Aggregation.match(criteria), getCategoryItemsLookupOperation());

        return mongoTemplate
                .aggregate(aggregation, "categories", Category.class)
                .getMappedResults();
    }

    @SuppressWarnings("unchecked")
    public List<Category> getAllCategoryItems(String restaurantId) {
        return (List<Category>) cacheService()
                .getOrLoad(
                        MENU_CACHE, "restaurant:" + restaurantId, List.class, () -> loadAllCategoryItems(restaurantId));
    }

    private List<Category> loadAllCategoryItems(String restaurantId) {
        Criteria criteria = Criteria.where("restaurant_id")
                .is(restaurantId)
                .andOperator(Criteria.where("is_deleted").is(false));
        long startTime = System.currentTimeMillis();
        Aggregation aggregation =
                Aggregation.newAggregation(Aggregation.match(criteria), getCategoryItemsLookupOperation());
        List<Category> category = mongoTemplate
                .aggregate(aggregation, "categories", Category.class)
                .getMappedResults();
        log.info("Query Execution Time for getAllCategoryItems: {} ms", System.currentTimeMillis() - startTime);
        return category;
    }

    private LookupOperation getCategoryItemsLookupOperation() {
        AggregationPipeline taxLookUpPipeline = Aggregation.newAggregation(
                        Aggregation.match(Criteria.where("is_deleted").is(false)),
                        LookupOperation.newLookup()
                                .from("taxes")
                                .localField("item_tax")
                                .foreignField("_id")
                                .as("taxes"))
                .getPipeline();

        return LookupOperation.newLookup()
                .from("items")
                .localField("_id")
                .foreignField("item_category_id")
                .pipeline(taxLookUpPipeline)
                .as("items");
    }

    public int updateItems(List<Item> items, Map<String, Object> fieldsToUpdate) {
        if (items.isEmpty()) {
            return 0;
        }

        List<String> itemIds = items.stream().map(Item::getId).collect(Collectors.toList());

        Query query = new Query(Criteria.where("_id").in(itemIds));

        Update update = new Update();
        fieldsToUpdate.forEach(update::set);

        UpdateResult result = mongoTemplate.updateMulti(query, update, "items");

        // Evict menu cache for affected restaurants
        items.stream().map(Item::getRestaurantId).distinct().forEach(rId -> cacheService()
                .evict(MENU_CACHE, "restaurant:" + rId));

        return (int) result.getModifiedCount();
    }
}
