package com.hyp.service;

import java.util.List;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public interface BaseService<T, ID> {

    T findById(ID id);

    List<T> findByIds(List<ID> ids);

    List<T> findAll();

    T save(T entity);

    List<T> saveAll(List<T> entities);

    List<T> saveAll(List<T> entities, String rid);

    T update(T entity);

    void deleteById(ID id);

    void softDeleteById(ID id);

    void softDeleteByRestaurant(Class<T> entityClass, ID id);

    void deleteAll(Class<T> entityClass);

    void softDeleteAll(List<T> entities);

    public boolean isExistsById(ID id);

    public T findByField(Class<T> entityClass, String fieldName, Object value);

    public List<T> findByQuery(Class<T> entityClass, Query query);

    public List<T> findByQueryWithReferences(Class<T> entityClass, Query query);

    T findByIdWithReference(ID id, Class<T> entityClass);

    List<T> findByRestaurant(Class<T> entityClass, Object value);

    List<T> findAllByIdIn(List<ID> ids);

    T findByIdScoped(ID id);

    List<T> findByIdsScoped(List<ID> ids);

    List<T> findAllScoped();

    T findByFieldScoped(Class<T> entityClass, String fieldName, Object value);

    List<T> findByQueryScoped(Class<T> entityClass, Query query);

    List<T> findByQuerySmart(Class<T> entityClass, Query query);

    boolean isEntityOwner(T entity);
}
