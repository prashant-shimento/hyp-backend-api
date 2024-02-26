package com.hyp.service;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public interface BaseService<T, ID> {
	T findById(ID id);

	List<T> findAll();

	T save(T entity);

	List<T> saveAll(List<T> entities);
	
	List<T> saveAll(List<T> entities, String rid);

	T update(T entity);

	void deleteById(ID id);
}
