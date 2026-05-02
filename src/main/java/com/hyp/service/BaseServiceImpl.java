package com.hyp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.entity.*;
import com.hyp.security.principal.RestaurantContext;
import com.hyp.security.principal.SecurityContextHolder;
import com.hyp.security.principal.UserPrincipal;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;
import io.lettuce.core.dynamic.support.GenericTypeResolver;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        if (entity instanceof Identifiable<?> identifiable) {
            Object id = identifiable.getId();
            return id != null ? id.toString() : null;
        }
        return null;
    }

    private void refreshEntity(T entity) {
        if (!isCacheEnabled() || entity == null) return;
        String id = entityId(entity);
        if (id != null) {
            cacheService.put(cacheName(), id, entity);
        }
        for (String key : additionalEvictionKeys(entity)) {
            cacheService.put(cacheName(), key, entity);
        }
    }

    /**
     * Enable strict mode - throws exception if restaurant context missing
     * Default: false (backward compatible)
     */
    protected boolean strictRestaurantScoping = false;

    /**
     * Enable auto-scoping - automatically applies restaurant filter when context exists
     * Default: false (backward compatible)
     */
    protected boolean autoRestaurantScoping = false;

    /**
     * Get current restaurant context from ThreadLocal
     */
    protected String getCurrentRestaurantId() {
        return RestaurantContext.getRestaurantId();
    }

    /**
     * Check if entity is restaurant-scoped
     * Handles inheritance properly
     */
    protected boolean isRestaurantScoped(Class<?> entityClass) {
        // Strategy 1: Check if implements RestaurantScoped interface (single restaurantId)
        if (RestaurantScoped.class.isAssignableFrom(entityClass)) {
            log.debug("Entity {} is restaurant-scoped (implements RestaurantScoped)", entityClass.getSimpleName());
            return true;
        }

        // Strategy 2: Check if implements RestaurantSetScoped interface (Set<String> restaurants)
        if (RestaurantSetScoped.class.isAssignableFrom(entityClass)) {
            log.debug("Entity {} is restaurant-scoped (implements RestaurantSetScoped)", entityClass.getSimpleName());
            return true;
        }

        // Strategy 3: Check for restaurantId field in class hierarchy
        boolean hasField = hasRestaurantIdField(entityClass);
        log.debug("Entity {} restaurant-scoped field check: {}", entityClass.getSimpleName(), hasField);
        return hasField;
    }

    /**
     * Check if class or any superclass has restaurantId field
     */
    private boolean hasRestaurantIdField(Class<?> entityClass) {
        Class<?> currentClass = entityClass;
        while (currentClass != null && currentClass != Object.class) {
            try {
                Field field = currentClass.getDeclaredField("restaurantId");
                log.debug("Found restaurantId field in {}", currentClass.getSimpleName());
                return true;
            } catch (NoSuchFieldException e) {
                // Continue to superclass
                currentClass = currentClass.getSuperclass();
            }
        }
        return false;
    }

    /**
     * Get restaurantId from entity using reflection (handles inheritance)
     */
    protected String getRestaurantIdFromEntity(T entity) {
        if (entity instanceof RestaurantScoped) {
            return ((RestaurantScoped) entity).getRestaurantId();
        }

        // Fallback: use reflection
        try {
            Class<?> currentClass = entity.getClass();
            while (currentClass != null && currentClass != Object.class) {
                try {
                    Field field = currentClass.getDeclaredField("restaurantId");
                    field.setAccessible(true);
                    return (String) field.get(entity);
                } catch (NoSuchFieldException e) {
                    currentClass = currentClass.getSuperclass();
                }
            }
        } catch (IllegalAccessException e) {
            log.error("Error accessing restaurantId field", e);
        }
        return null;
    }

    /**
     * Set restaurantId on entity using reflection (handles inheritance)
     */
    protected void setRestaurantIdOnEntity(T entity, String restaurantId) {
        if (entity instanceof RestaurantScoped) {
            ((RestaurantScoped) entity).setRestaurantId(restaurantId);
            return;
        }

        // Fallback: use reflection
        try {
            Class<?> currentClass = entity.getClass();
            while (currentClass != null && currentClass != Object.class) {
                try {
                    Field field = currentClass.getDeclaredField("restaurantId");
                    field.setAccessible(true);
                    field.set(entity, restaurantId);
                    return;
                } catch (NoSuchFieldException e) {
                    currentClass = currentClass.getSuperclass();
                }
            }
        } catch (IllegalAccessException e) {
            log.error("Error setting restaurantId field", e);
        }
    }

    protected void validateRestaurantContext(Class<?> entityClass) {
        if (isRestaurantScoped(entityClass)) {
            String restaurantId = getCurrentRestaurantId();
            if (restaurantId == null || restaurantId.isBlank()) {
                if (strictRestaurantScoping) {
                    throw new IllegalStateException("Restaurant context required for " + entityClass.getSimpleName());
                } else {
                    log.warn("Restaurant context not set for scoped entity: {}", entityClass.getSimpleName());
                }
            }
        }
    }

    protected Query applyRestaurantFilter(Query query, Class<?> entityClass) {
        String restaurantId = getCurrentRestaurantId();
        if (restaurantId == null || restaurantId.isBlank()) return query;

        if (RestaurantSetScoped.class.isAssignableFrom(entityClass)) {
            // Set<String> restaurants field — match if restaurantId is an element
            query.addCriteria(Criteria.where("restaurants").is(restaurantId));
            log.debug("Applied RestaurantSetScoped filter: restaurants contains {}", restaurantId);
        } else if (isRestaurantScoped(entityClass)) {
            query.addCriteria(Criteria.where("restaurant_id").is(restaurantId));
            log.debug("Applied restaurant filter: restaurant_id={}", restaurantId);
        }
        return query;
    }

    /**
     * Get current customer ID from security context.
     * Returns null if not a CUSTOMER user (admin/partner/superadmin bypass).
     */
    protected String getCurrentCustomerId() {
        UserPrincipal principal = SecurityContextHolder.getPrincipal();
        if (principal != null && "CUSTOMER".equals(principal.getUserType())) {
            return principal.getUserId();
        }
        return null;
    }

    /**
     * Apply customer owner filter to a query (for list operations).
     * - CustomerScoped entities: filters by customer_id
     * - SelfScoped entities: filters by _id
     * Only applies when the current user is a CUSTOMER.
     */
    protected Query applyOwnerFilter(Query query) {
        String customerId = getCurrentCustomerId();
        if (customerId == null) return query; // non-CUSTOMER users bypass

        Class<T> entityClass = getEntityClass();
        if (CustomerScoped.class.isAssignableFrom(entityClass)) {
            query.addCriteria(Criteria.where("customer_id").is(customerId));
            log.debug("Applied CustomerScoped filter: customer_id={}", customerId);
        } else if (SelfScoped.class.isAssignableFrom(entityClass)) {
            query.addCriteria(Criteria.where("_id").is(customerId));
            log.debug("Applied SelfScoped filter: _id={}", customerId);
        }

        return query;
    }

    /**
     * Check if the current user owns the entity (for single-entity operations).
     * - CustomerScoped: entity.getCustomerId() == principal.userId
     * - SelfScoped + Identifiable: entity.getId() == principal.userId
     * Returns true if: entity is not scoped, user is not a CUSTOMER, or ownership matches.
     */
    @Override
    public boolean isEntityOwner(T entity) {
        String customerId = getCurrentCustomerId();
        if (customerId == null) return true; // non-CUSTOMER users bypass

        if (entity == null) return false;

        if (entity instanceof CustomerScoped scoped) {
            return customerId.equals(scoped.getCustomerId());
        }
        if (entity instanceof SelfScoped && entity instanceof Identifiable<?> identifiable) {
            Object id = identifiable.getId();
            return customerId.equals(id != null ? id.toString() : null);
        }

        return true; // entity is not customer-scoped
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
        if (autoRestaurantScoping && isRestaurantScoped(entity.getClass())) {
            String currentRestaurantId = getRestaurantIdFromEntity(entity);
            String contextRestaurantId = getCurrentRestaurantId();

            if (currentRestaurantId == null && contextRestaurantId != null) {
                setRestaurantIdOnEntity(entity, contextRestaurantId);
                log.debug(
                        "Auto-set restaurantId={} for entity {}",
                        contextRestaurantId,
                        entity.getClass().getSimpleName());
            }
        }

        // Auto-set customerId from principal for CUSTOMER users (e.g., Address, Order)
        if (entity instanceof CustomerScoped scoped) {
            String customerId = getCurrentCustomerId();
            if (customerId != null) {
                scoped.setCustomerId(customerId);
                log.debug(
                        "Auto-set customerId={} for entity {}",
                        customerId,
                        entity.getClass().getSimpleName());
            }
        }

        T saved = repository.save(entity);
        refreshEntity(saved);
        return saved;
    }

    @Override
    public List<T> saveAll(List<T> entities) {
        if (autoRestaurantScoping) {
            String restaurantId = getCurrentRestaurantId();
            if (restaurantId != null) {
                for (T entity : entities) {
                    if (isRestaurantScoped(entity.getClass())) {
                        String currentRestaurantId = getRestaurantIdFromEntity(entity);
                        if (currentRestaurantId == null) {
                            setRestaurantIdOnEntity(entity, restaurantId);
                        }
                    }
                }
            }
        }
        return repository.saveAll(entities);
    }

    @Override
    public List<T> saveAll(List<T> entities, String rId) {
        for (T entity : entities) {
            if (entity instanceof RestaurantScoped) {
                ((RestaurantScoped) entity).setRestaurantId(rId);
            } else {
                setRestaurantIdOnEntity(entity, rId);
            }
        }
        return repository.saveAll(entities);
    }

    @Override
    public T update(T entity) {
        T updated = repository.save(entity);
        refreshEntity(updated);
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
        Query query = new Query(Criteria.where("restaurant_id").is(value));
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
        populateReferencesBatch(entities);
        return entities;
    }

    private void populateReferencesBatch(List<T> entities) {
        // Batch Item taxes: collect all tax IDs → 1 query → distribute
        Set<String> allTaxIds = new HashSet<>();
        for (T entity : entities) {
            if (entity instanceof Item item && item.getItemTax() != null) {
                allTaxIds.addAll(item.getItemTax());
            }
        }
        if (!allTaxIds.isEmpty()) {
            Map<String, Tax> taxMap =
                    mongoTemplate.find(Query.query(Criteria.where("_id").in(allTaxIds)), Tax.class, "taxes").stream()
                            .collect(Collectors.toMap(Tax::getId, t -> t));
            for (T entity : entities) {
                if (entity instanceof Item item && item.getItemTax() != null) {
                    item.setTaxes(item.getItemTax().stream()
                            .map(taxMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()));
                }
            }
        }

        // Batch Partner restaurants: collect all restaurant IDs → 1 query → distribute
        Set<String> allRestaurantIds = new HashSet<>();
        for (T entity : entities) {
            if (entity instanceof Partner partner && partner.getRestaurants() != null) {
                allRestaurantIds.addAll(partner.getRestaurants());
            }
        }
        if (!allRestaurantIds.isEmpty()) {
            Map<String, Restaurant> restaurantMap = mongoTemplate
                    .find(Query.query(Criteria.where("_id").in(allRestaurantIds)), Restaurant.class, "restaurants")
                    .stream()
                    .collect(Collectors.toMap(Restaurant::getId, r -> r));
            for (T entity : entities) {
                if (entity instanceof Partner partner && partner.getRestaurants() != null) {
                    partner.setRestaurantDetails(partner.getRestaurants().stream()
                            .map(restaurantMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList()));
                }
            }
        }
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

    @Override
    public T findByIdScoped(ID id) {
        validateRestaurantContext(getEntityClass());

        if (!isRestaurantScoped(getEntityClass())) {
            return findById(id);
        }

        String restaurantId = getCurrentRestaurantId();
        if (restaurantId == null || restaurantId.isBlank()) {
            return findById(id); // Fallback to unscoped if no context
        }

        Query query = new Query(Criteria.where("_id").is(id).and("restaurantId").is(restaurantId));

        return mongoTemplate.findOne(query, getEntityClass());
    }

    @Override
    public List<T> findByIdsScoped(List<ID> ids) {
        validateRestaurantContext(getEntityClass());

        if (!isRestaurantScoped(getEntityClass())) {
            return findByIds(ids);
        }

        String restaurantId = getCurrentRestaurantId();
        if (restaurantId == null || restaurantId.isBlank()) {
            return findByIds(ids); // Fallback
        }

        Query query =
                new Query(Criteria.where("_id").in(ids).and("restaurantId").is(restaurantId));

        return mongoTemplate.find(query, getEntityClass());
    }

    @Override
    public List<T> findAllScoped() {
        validateRestaurantContext(getEntityClass());

        if (!isRestaurantScoped(getEntityClass())) {
            return findAll();
        }

        String restaurantId = getCurrentRestaurantId();
        if (restaurantId == null || restaurantId.isBlank()) {
            return findAll(); // Fallback
        }

        Query query = new Query(Criteria.where("restaurantId").is(restaurantId));
        return mongoTemplate.find(query, getEntityClass());
    }

    @Override
    public T findByFieldScoped(Class<T> entityClass, String fieldName, Object value) {
        Query query = new Query(Criteria.where(fieldName).is(value));
        query = applyRestaurantFilter(query, entityClass);
        return mongoTemplate.findOne(query, entityClass);
    }

    @Override
    public List<T> findByQueryScoped(Class<T> entityClass, Query query) {
        validateRestaurantContext(entityClass);
        query = applyRestaurantFilter(query, entityClass);
        return mongoTemplate.find(query, entityClass);
    }

    /**
     * Smart query method:
     * - If restaurant context exists → apply scoping
     * - If no context → use unscoped
     * Perfect for gradual migration!
     */
    @Override
    public List<T> findByQuerySmart(Class<T> entityClass, Query query) {
        String restaurantId = getCurrentRestaurantId();

        // If context exists and entity is scoped, apply filter
        if (restaurantId != null && !restaurantId.isBlank() && isRestaurantScoped(entityClass)) {
            log.debug("Smart query: applying restaurant scope for restaurantId={}", restaurantId);
            query = applyRestaurantFilter(query, entityClass);
        } else {
            log.debug("Smart query: no restaurant scope applied");
        }

        // Apply owner filter (only affects CUSTOMER users when ownerField is set)
        query = applyOwnerFilter(query);

        return mongoTemplate.find(query, entityClass);
    }

    @SuppressWarnings("unchecked")
    protected Class<T> getEntityClass() {
        Class<?>[] typeArgs = GenericTypeResolver.resolveTypeArguments(getClass(), BaseServiceImpl.class);
        if (typeArgs != null && typeArgs.length >= 1) {
            return (Class<T>) typeArgs[0];
        }
        throw new IllegalStateException("Cannot resolve entity class");
    }
}
