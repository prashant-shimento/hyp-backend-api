package com.hyp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Service;

@Service
public abstract class BaseServiceImpl<T, ID, R extends MongoRepository<T, ID>> implements BaseService<T, ID> {

	@Autowired
	private R repository;

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
	public T update(T entity) {
		return repository.save(entity);
	}

	@Override
	public void deleteById(ID id) {
		repository.deleteById(id);
	}
}
