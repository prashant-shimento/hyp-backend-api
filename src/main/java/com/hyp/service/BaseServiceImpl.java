package com.hyp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

import com.hyp.entity.BaseEntity;

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
		return mongoTemplate.find(query,entityClass);
	}
}
