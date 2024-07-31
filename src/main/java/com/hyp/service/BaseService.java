package com.hyp.service;

import java.util.List;

import org.springframework.data.mongodb.core.query.Query;
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
	
	public boolean isExistsById(ID id);
	
    public T findByField(Class<T> entityClass, String fieldName, Object value);
    
    public List<T> findByQuery(Class<T> entityClass, Query query);
    
    public List<T> findByQueryWithReferences(Class<T> entityClass, Query query);

}
