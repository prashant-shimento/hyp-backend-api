package com.hyp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.entity.*;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import java.util.*;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

@Service
public abstract class BaseServiceImpl<T, ID> implements BaseService<T, ID> {

    @Autowired
    private MongoRepository<T, ID> repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheService cacheService;

    /** Return a cache namespace (e.g. "restaurants") to enable caching. Default: null (disabled). */
    protected String cacheName() {
        return null;
    }

    /** Return the entity class for JSON deserialization. Required when cacheName() is non-null. */
    protected Class<T> entityType() {
        return null;
    }

    /** Return extra cache keys to evict on save/update (e.g. "mobile:9876543210"). */
    protected List<String> additionalEvictionKeys(T entity) {
        return Collections.emptyList();
    }

    /** Accessor for subclasses that need custom cache lookups (e.g. findByMobile). */
    protected CacheService cacheService() {
        return cacheService;
    }

    private boolean isCacheEnabled() {
        return cacheName() != null && entityType() != null;
    }

    private String entityId(T entity) {
        if (entity instanceof BaseEntity be) return be.getId();
        return null;
    }

    private void evictEntity(T entity) {
        if (!isCacheEnabled() || entity == null) return;
        String id = entityId(entity);
        if (id != null) {
            cacheService.evict(cacheName(), id);
        }
        for (String key : additionalEvictionKeys(entity)) {
            cacheService.evict(cacheName(), key);
        }
    }

    @Override
    public T findById(ID id) {
        if (id == null) return null;
        if (isCacheEnabled()) {
            return cacheService.getOrLoad(cacheName(), id.toString(), entityType(), () -> loadFromDb(id));
        }
        return loadFromDb(id);
    }

    private T loadFromDb(ID id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public List<T> findByIds(List<ID> ids) {
        return repository.findAllById(ids);
    }

    @Override
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    public T save(T entity) {
        T saved = repository.save(entity);
        evictEntity(saved);
        return saved;
    }

    @Override
    public List<T> saveAll(List<T> entities) {
        return repository.saveAll(entities);
    }

    @Override
    public List<T> saveAll(List<T> entities, String rId) {
        for (T entity : entities) {
            ((BaseEntity) entity).setRestaurantId(rId);
        }
        return repository.saveAll(entities);
    }

    @Override
    public T update(T entity) {
        T updated = repository.save(entity);
        evictEntity(updated);
        return updated;
    }

    @Override
    public void deleteById(ID id) {
        repository.deleteById(id);
        if (isCacheEnabled() && id != null) {
            cacheService.evict(cacheName(), id.toString());
        }
    }

    @Override
    public boolean isExistsById(ID id) {
        return repository.existsById(id);
    }

    @Override
    public T findByField(Class<T> entityClass, String fieldName, Object value) {
        Query query = new Query(Criteria.where(fieldName).is(value));
        return mongoTemplate.findOne(query, entityClass);
    }

    @Override
    public List<T> findByRestaurant(Class<T> entityClass, Object value) {
        Query query = new Query(Criteria.where("restaurantId").is(value));
        return mongoTemplate.find(query, entityClass);
    }

    @Override
    public List<T> findByQuery(Class<T> entityClass, Query query) {
        return mongoTemplate.find(query, entityClass);
    }

    public T populateReferences(T entity) {
        if (entity instanceof Item item) {
            item.setTaxes(lookupByIds(Tax.class, item.getItemTax(), "taxes"));
        }
        if (entity instanceof Partner partner) {
            partner.setRestaurantDetails(lookupByIds(Restaurant.class, partner.getRestaurants(), "restaurants"));
        }
        return entity;
    }

    private <E> List<E> lookupByIds(Class<E> entityClass, List<String> ids, String collectionName) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        Query query = Query.query(Criteria.where("_id").in(ids));
        return mongoTemplate.find(query, entityClass, collectionName);
    }

    public List<T> findByQueryWithReferences(Class<T> entityClass, Query query) {
        List<T> entities = mongoTemplate.find(query, entityClass);
        return entities.stream().map(this::populateReferences).collect(Collectors.toList());
    }

    @Override
    public T findByIdWithReference(ID id, Class<T> entityClass) {
        T entity = mongoTemplate.findById(id, entityClass);
        return populateReferences(entity);
    }

    @Override
    public void softDeleteById(ID id) {
        T entity = findById(id);
        if (entity instanceof BaseEntity) {
            ((BaseEntity) entity).setDeleted(true);
            save(entity);
        }
    }

    @Override
    public void softDeleteByRestaurant(Class<T> entityClass, ID restaurantId) {
        Query query = Query.query(Criteria.where("restaurant_id").is(restaurantId));
        Update update = new Update().set("is_deleted", true);
        mongoTemplate.updateMulti(query, update, entityClass);
    }

    @Override
    public void deleteAll(Class<T> entityClass) {
        Query query = new Query();
        mongoTemplate.remove(query, entityClass);
    }

    @Override
    public void softDeleteAll(List<T> entities) {
        for (T entity : entities) {
            if (entity instanceof BaseEntity) {
                ((BaseEntity) entity).setDeleted(true);
            }
        }
        repository.saveAll(entities);
    }

    private String getCollectionName(Class<T> entityClass) {
        return mongoTemplate.getCollectionName(entityClass);
    }

    public BulkWriteResult bulkInsert(List<T> entities, Class<T> entityClass) {
        List<WriteModel<Document>> writeModels = new ArrayList<>();

        for (T entity : entities) {
            writeModels.add(new InsertOneModel<>((Document) entity));
        }
        return mongoTemplate
                .getCollection(getCollectionName(entityClass))
                .bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
    }

    public void bulkUpdate(List<T> entities, Class<T> entityClass) {
        List<WriteModel<Document>> writeModels = new ArrayList<>();

        for (T entity : entities) {
            String id = ((BaseEntity) entity).getId();
            Bson filter = Filters.eq("_id", id);

            Document doc = new Document();
            mongoTemplate.getConverter().write(entity, doc);
            doc.remove("_id");

            Bson update = new Document("$set", doc);
            writeModels.add(new UpdateOneModel<>(filter, update));
        }

        mongoTemplate
                .getCollection(getCollectionName(entityClass))
                .bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
    }

    @Override
    public List<T> findAllByIdIn(List<ID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return repository.findAllById(ids);
    }
}
