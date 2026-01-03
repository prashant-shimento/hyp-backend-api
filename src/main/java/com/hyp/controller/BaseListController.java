package com.hyp.controller;

import com.hyp.entity.BaseEntity;
import com.hyp.response.Response;
import com.hyp.service.BaseService;
import com.hyp.service.BaseTranslationService;
import com.hyp.util.QueryUtils;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
public abstract class BaseListController<DTO, T, ID> {

    @Autowired
    protected BaseTranslationService<DTO, T> translationService;

    @Autowired
    protected BaseService<T, ID> service;

    protected Class<T> entity;

    @SuppressWarnings("unchecked")
    public BaseListController() {
        Class<?>[] typeArgs = GenericTypeResolver.resolveTypeArguments(getClass(), BaseListController.class);
        if (typeArgs != null && typeArgs.length >= 2) {
            this.entity = (Class<T>) typeArgs[1];
        } else {
            throw new IllegalStateException("Cannot resolve entity class");
        }
    }

    @GetMapping
    public ResponseEntity<Response> getAll(@RequestParam(required = false) Map<String, String> queryParam) {
        Query query = QueryUtils.getFilterQuery(queryParam, QueryUtils.getAllowedParameters(entity.getSimpleName()));
        if (query == null) {
            return ResponseEntity.badRequest().body(new Response(null, true, "Invalid query parameters"));
        }
        log.info("getAll {}", query);
        long startTime = System.currentTimeMillis();
        List<T> entities = service.findByQueryWithReferences(entity, query);
        log.info("Query Execution Time for getAll: {} ms", System.currentTimeMillis() - startTime);
        List<DTO> dtoEntities = translationService.getDtoList(entities);
        Response response = new Response(dtoEntities, false, "success");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getById(@PathVariable ID id) {
        try {
            T entityById = service.findByIdWithReference(id, entity);
            if (entityById != null) {
                if (entityById instanceof BaseEntity) {
                    if (!((BaseEntity) entityById).isDeleted()) {
                        DTO dto = translationService.getDto(entityById);
                        Response response = new Response(Collections.singletonList(dto), false, "success");
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.notFound().build();
                    }
                } else {
                    DTO dto = translationService.getDto(entityById);
                    Response response = new Response(Collections.singletonList(dto), false, "success");
                    return ResponseEntity.ok(response);
                }
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception ex) {
            Response response = new Response(null, true, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
