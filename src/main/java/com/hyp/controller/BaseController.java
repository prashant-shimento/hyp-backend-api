package com.hyp.controller;

import com.hyp.entity.BaseEntity;
import com.hyp.response.Response;
import com.hyp.security.principal.RestaurantContext;
import com.hyp.service.BaseService;
import com.hyp.service.BaseTranslationService;
import com.hyp.util.QueryUtils;
import com.hyp.validation.BaseValidator;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
public abstract class BaseController<DTO, T, ID> {

    @Autowired
    protected BaseService<T, ID> service;

    @Autowired
    protected BaseTranslationService<DTO, T> translationService;

    @Autowired(required = false)
    protected BaseValidator<DTO> validator;

    protected Class<T> entity;

    /**
     * Scoping mode for this controller
     */
    protected ScopingMode scopingMode = ScopingMode.LEGACY; // Default: no changes

    public enum ScopingMode {
        LEGACY, // Old behavior: no scoping (default)
        SMART, // Apply scoping only when context exists
        STRICT_SCOPED // Always require and enforce scoping
    }

    @SuppressWarnings("unchecked")
    public BaseController() {
        Class<?>[] typeArgs = GenericTypeResolver.resolveTypeArguments(getClass(), BaseController.class);
        if (typeArgs != null && typeArgs.length >= 2) {
            this.entity = (Class<T>) typeArgs[1];
        } else {
            throw new IllegalStateException("Cannot resolve entity class");
        }
    }

    @GetMapping
    public ResponseEntity<Response> getAll(@RequestParam(required = false) Map<String, String> queryParam) {
        try {
            Query query =
                    QueryUtils.getFilterQuery(queryParam, QueryUtils.getAllowedParameters(entity.getSimpleName()));

            if (query == null) {
                return ResponseEntity.badRequest().body(new Response(null, true, "Invalid query parameters"));
            }

            log.info("getAll Query {} with scopingMode={}", query, scopingMode);
            long startTime = System.currentTimeMillis();

            List<T> entities =
                    switch (scopingMode) {
                        case LEGACY -> service.findByQueryWithReferences(entity, query);
                        case SMART -> service.findByQuerySmart(entity, query);
                        case STRICT_SCOPED -> service.findByQueryScoped(entity, query);
                    };

            long endTime = System.currentTimeMillis();
            log.info("Query Execution Time: {} ms", endTime - startTime);

            List<DTO> dtoEntities = translationService.getDtoList(entities);
            Response response = new Response(dtoEntities, false, "success");
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            log.error("Restaurant context error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new Response(null, true, e.getMessage()));
        } catch (Exception ex) {
            log.error("Error in getAll", ex);
            Response response = new Response(null, true, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Response> getById(@PathVariable ID id) {
        try {
            T entity =
                    switch (scopingMode) {
                        case LEGACY -> service.findById(id);
                        case SMART -> {
                            String restaurantId = RestaurantContext.getRestaurantId();
                            if (restaurantId != null && !restaurantId.isBlank()) {
                                yield service.findByIdScoped(id);
                            } else {
                                yield service.findById(id);
                            }
                        }
                        case STRICT_SCOPED -> service.findByIdScoped(id);
                    };

            if (entity == null) {
                return ResponseEntity.notFound().build();
            }

            // Owner check for SMART/STRICT_SCOPED modes
            if (scopingMode != ScopingMode.LEGACY && !service.isEntityOwner(entity)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Response(null, true, "Access denied"));
            }

            if (entity instanceof BaseEntity && ((BaseEntity) entity).isDeleted()) {
                return ResponseEntity.notFound().build();
            }

            DTO dto = translationService.getDto(entity);
            Response response = new Response(Collections.singletonList(dto), false, "success");
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            log.error("Restaurant context error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new Response(null, true, e.getMessage()));
        } catch (Exception ex) {
            log.error("Error in getById", ex);
            Response response = new Response(null, true, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Response> create(@RequestBody @Valid DTO dto) {
        try {

            if (validator != null) {
                validator.validate(dto);
            }
            T entity = translationService.getEntity(dto);
            T savedEntity = service.save(entity);
            dto = translationService.getDto(savedEntity);
            // Need to revisit returning list logic
            Response response = new Response(Collections.singletonList(dto), false, "success");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            Response response = new Response(null, true, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Response> update(@PathVariable ID id, @RequestBody DTO dto) {
        try {
            T existingEntity = service.findById(id);
            if (existingEntity == null) {
                return ResponseEntity.notFound().build();
            }

            // Owner check for SMART/STRICT_SCOPED modes
            if (scopingMode != ScopingMode.LEGACY && !service.isEntityOwner(existingEntity)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Response(null, true, "Access denied"));
            }

            translationService.updateEntityFromDto(dto, existingEntity);
            T updatedEntity = service.save(existingEntity);
            dto = translationService.getDto(updatedEntity);
            Response response = new Response(Collections.singletonList(dto), false, "success");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            Response response = new Response(null, true, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PatchMapping
    public ResponseEntity<Response> updateAll(@RequestBody DTO dto) {
        try {
            List<T> entities = service.findAll();

            if (entities != null && !entities.isEmpty()) {
                for (T entity : entities) {
                    translationService.updateEntityFromDto(dto, entity);
                }

                List<T> updatedEntities = service.saveAll(entities);

                List<DTO> updatedDtoList =
                        updatedEntities.stream().map(translationService::getDto).collect(Collectors.toList());

                Response response = new Response(updatedDtoList, false, "success");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            Response response = new Response(null, true, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Response> delete(@PathVariable ID id) {
        try {
            T existingEntity = service.findById(id);
            if (existingEntity == null) {
                return ResponseEntity.notFound().build();
            }

            // Owner check for SMART/STRICT_SCOPED modes
            if (scopingMode != ScopingMode.LEGACY && !service.isEntityOwner(existingEntity)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new Response(null, true, "Access denied"));
            }

            service.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            Response response = new Response(null, true, ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
