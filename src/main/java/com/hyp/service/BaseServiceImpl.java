package com.hyp.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.entity.BaseEntity;
import com.hyp.entity.Item;
import com.hyp.entity.Partner;
import com.hyp.entity.Restaurant;
import com.hyp.entity.Tax;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;

@Service
public abstract class BaseServiceImpl<T, ID> implements BaseService<T, ID> {

	@Autowired
	private MongoRepository<T, ID> repository;

	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public T findById(ID id) {
		Optional<T> optionalEntity = repository.findById(id);
		return optionalEntity.orElse(null);
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
		return repository.save(entity);
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
		return repository.save(entity);
	}

	@Override
	public void deleteById(ID id) {
		repository.deleteById(id);
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
		if (entity instanceof Item) {
			Item item = (Item) entity;
			item.setTaxes(lookupByIds(Tax.class, item.getItemTax(), "taxes"));
		}
		if (entity instanceof Partner) {
			Partner partner = (Partner) entity;
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

	private <E> E lookupById(Class<E> entityClass, String id, String collectionName) {
		Query query = Query.query(Criteria.where("_id").in(id));
		return mongoTemplate.findOne(query, entityClass, collectionName);
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
			repository.save(entity);
		}
	}

	@Override
	public void softDeleteByRestaurant(Class<T> entityClass, ID id) {
		Query query = Query.query(Criteria.where("restaurant_id").is(id));
		List<T> entities = mongoTemplate.find(query, entityClass);

		for (T entity : entities) {
			if (entity instanceof BaseEntity) {
				((BaseEntity) entity).setDeleted(true);
			}
		}
		repository.saveAll(entities);
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
			writeModels.add(new InsertOneModel<Document>((Document) entity));
		}
		return mongoTemplate.getCollection(getCollectionName(entityClass)).bulkWrite(writeModels,
				new BulkWriteOptions().ordered(false));
	}

	public BulkWriteResult bulkUpdate(List<T> entities, Class<T> entityClass) {
		List<WriteModel<Document>> writeModels = new ArrayList<>();

		for (T entity : entities) {
			String id = ((BaseEntity) entity).getId();
			Bson filter = Filters.eq("_id", id);
		    Map<String, Object> entityMap = objectMapper.convertValue(entity, new TypeReference<Map<String, Object>>() {});
		    Document entityDoc = new Document(entityMap);
			Bson update = new Document("$set", entityDoc);
			writeModels.add(new UpdateOneModel<Document>(filter, update));
		}

		return mongoTemplate.getCollection(getCollectionName(entityClass)).bulkWrite(writeModels,
				new BulkWriteOptions().ordered(false));
	}

}