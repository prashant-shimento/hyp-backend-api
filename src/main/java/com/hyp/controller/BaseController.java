package com.hyp.controller;

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

import com.hyp.entity.BaseEntity;
import com.hyp.response.Response;
import com.hyp.service.BaseService;
import com.hyp.service.BaseTranslationService;
import com.hyp.util.QueryUtils;

import jakarta.validation.Valid;
@Slf4j
public abstract class BaseController<DTO, T, ID> {

	@Autowired
	protected BaseService<T, ID> service;

	@Autowired
	protected BaseTranslationService<DTO, T> translationService;

	protected Class<T> entity;

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
		Query query = QueryUtils.getFilterQuery(queryParam, QueryUtils.getAllowedParameters(entity.getSimpleName()));
		if (query == null) {
			return ResponseEntity.badRequest().body(new Response(null, true, "Invalid query parameters"));
		}
		log.info("getAll Query {}", query);
		long startTime = System.currentTimeMillis();
		List<T> entities = service.findByQueryWithReferences(entity, query);
		long endTime = System.currentTimeMillis(); // End time
		long executionTime = endTime - startTime; // Execution time in milliseconds
		log.info("Query Execution Time for getAll: {} ms", executionTime);
		List<DTO> dtoEntities = translationService.getDtoList(entities);
		Response response = new Response(dtoEntities, false, "success");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<Response> getById(@PathVariable ID id) {
		try {
			T entity = service.findById(id);

			if (entity != null) {
				if (entity instanceof BaseEntity) {
					if (!((BaseEntity) entity).isDeleted()) {
						DTO dto = translationService.getDto(entity);
						Response response = new Response(Collections.singletonList(dto), false, "success");
						return ResponseEntity.ok(response);
					} else {
						return ResponseEntity.notFound().build();
					}
				} else {
					DTO dto = translationService.getDto(entity);
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

	@PostMapping
	public ResponseEntity<Response> create(@RequestBody @Valid DTO dto) {
		try {
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
			if (existingEntity != null) {
				translationService.updateEntityFromDto(dto, existingEntity);
				T updatedEntity = service.save(existingEntity);
				dto = translationService.getDto(updatedEntity);
				Response response = new Response(Collections.singletonList(dto), false, "success");
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.notFound().build();
			}
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

				List<DTO> updatedDtoList = updatedEntities.stream().map(translationService::getDto)
						.collect(Collectors.toList());

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
			if (service.findById(id) != null) {
				service.deleteById(id);
				return ResponseEntity.ok().build();
			} else {
				return ResponseEntity.notFound().build();
			}
		} catch (Exception ex) {
			Response response = new Response(null, true, ex.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

}
