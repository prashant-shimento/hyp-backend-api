package com.hyp.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import com.hyp.entity.Attribute;
import com.hyp.entity.BaseEntity;
import com.hyp.entity.Item;
import com.hyp.entity.Order;
import com.hyp.entity.Partner;
import com.hyp.entity.Restaurant;
import com.hyp.entity.Tax;

@Service
public abstract class BaseServiceImpl<T, ID> implements BaseService<T, ID> {

	@Autowired
	private MongoRepository<T, ID> repository;

	@Autowired
	private MongoTemplate mongoTemplate;

	@Override
	public T findById(ID id) {
		Optional<T> optionalEntity = repository.findById(id);
		return optionalEntity.orElse(null);
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
	public List<T> findByQuery(Class<T> entityClass, Query query) {
		return mongoTemplate.find(query, entityClass);
	}

	public T populateReferences(T entity) {
		if (entity instanceof Item) {
			Item item = (Item) entity;
			item.setTaxes(lookupByIds(Tax.class, item.getItemTax(), "taxes"));
		}
		if (entity instanceof Order) {
			Order order = (Order) entity;
			for (Order.OrderItem orderItem : order.getOrderItems()) {
				if(orderItem.getItemAttribute() == null) {
					Item item = lookupById(Item.class, orderItem.getId(), "items");
					if (item != null) {
						if (item.getItemAttributeId() != null) {
							Attribute attribute = lookupById(Attribute.class, item.getItemAttributeId(), "attributes");
							orderItem.setItemAttribute(attribute);
						}
					}
				}
			}
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
	public T findByIdWithReference(ID id,Class<T> entityClass) {
		T entity = mongoTemplate.findById(id, entityClass);
	    return populateReferences(entity);
	}

}
