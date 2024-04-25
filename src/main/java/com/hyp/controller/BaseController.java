package com.hyp.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.hyp.response.Response;
import com.hyp.service.BaseService;
import com.hyp.service.TranslationService;
import com.hyp.util.QueryUtils;

import jakarta.validation.Valid;

public abstract class BaseController<DTO, T, ID> {

	@Autowired
	protected BaseService<T, ID> service;

	@Autowired
	protected TranslationService<DTO, T> translationService;

	protected Class<T> entity;

	@SuppressWarnings("unchecked")
	public BaseController() {
		Class<?>[] typeArgs = GenericTypeResolver.resolveTypeArguments(getClass(), BaseListController.class);
		if (typeArgs != null && typeArgs.length >= 2) {
			this.entity = (Class<T>) typeArgs[1];
		} else {
			throw new IllegalStateException("Cannot resolve entity class");
		}
	}
	
	@GetMapping
	public ResponseEntity<Response> getAll(@RequestParam Map<String, String> queryParam, @RequestParam int limit,
			@RequestParam int offset) {
		List<T> entities;
		if (!queryParam.isEmpty()) {
			Query query = QueryUtils.getFilterQuery(queryParam, limit, offset,
					QueryUtils.getAllowedParameters(entity.getSimpleName()));
			if (query == null) {
				return ResponseEntity.badRequest().body(new Response(null, true, "Invalid query parameters"));
			}
			entities = service.findByQuery(entity, query);
		} else {
			entities = service.findAll();
		}

		List<DTO> dtoEntities = translationService.getDtoList(entities);
		Response response = new Response(dtoEntities, false, "success");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<Response> getById(@PathVariable ID id) {
		try {
			T entity = service.findById(id);
			if (entity != null) {
				DTO dto = translationService.getDto(entity);
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

	@PostMapping
	public ResponseEntity<Response> create(@RequestBody  @Valid DTO dto) {
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

	@PutMapping("/{id}")
	public ResponseEntity<Response> update(@PathVariable ID id, @RequestBody DTO dto) {
		try {
			if (service.findById(id) != null) {
				T entity = translationService.getEntity(dto);
				T updatedEntity = service.save(entity);
				dto = translationService.getDto(updatedEntity);
				// Need to revisit returning list logic
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
//	@PutMapping("/{id}")
//	public ResponseEntity<ResponseTemplate> update(@PathVariable ID id, @RequestBody DTO dto) {
//		try {
//			// Check if the entity with the given ID exists
//			T existingEntity = service.findById(id);
//			if (existingEntity != null) {
//				// Translate DTO to entity
//				T updatedEntity = translationService.getEntity(dto);
//
//				// Set the ID of the existing entity to ensure the correct entity is updated
//				updatedEntity.setId(id);
//
//				// Save the updated entity
//				T savedEntity = service.save(updatedEntity);
//
//				// Translate the updated entity back to DTO
//				DTO updatedDto = translationService.getDto(savedEntity);
//
//				// Return the updated DTO in the response
//				ResponseTemplate response = new ResponseTemplate(Collections.singletonList(updatedDto), false,
//						"Success");
//				return ResponseEntity.ok(response);
//			} else {
//				// If the entity with the given ID does not exist, return 404 Not Found
//				return ResponseEntity.notFound().build();
//			}
//		} catch (Exception ex) {
//			// If an exception occurs during the update process, return an error response
//			ResponseTemplate response = new ResponseTemplate(null, true, ex.getMessage());
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//		}
//	}

//	@PatchMapping("/{id}")
//    public ResponseEntity<ResponseTemplate> patch(@PathVariable ID id, @RequestBody DTO dto) {
//        try {
//        	if (repository.existsById(id)) {
//        		 T existingEntity = repository.findById(id).get();
//                 Entity updatedEntity = translationService.updateEntityFromPartialDto(existingEntity, dto);
//
//        		 ResponseTemplate response = new ResponseTemplate(updated, false, "success");
//				return ResponseEntity.ok(response);
//			} else {
//				return ResponseEntity.notFound().build();
//			}
//            // Retrieve the existing entity from the repository
//            Optional<Entity> optionalEntity = repository.findById(id);
//            if (!optionalEntity.isPresent()) {
//                // If the entity does not exist, return 404 Not Found
//                return ResponseEntity.notFound().build();
//            }
//
//            // Translate the partial DTO to entity
//            Entity existingEntity = optionalEntity.get();
//            Entity updatedEntity = translationService.updateEntityFromPartialDto(existingEntity, partialDto);
//
//            // Save the updated entity
//            Entity savedEntity = repository.save(updatedEntity);
//
//            // Translate the saved entity back to DTO
//            DTO savedDto = translationService.translateToDto(savedEntity);
//
//            // Construct and return the response
//            ResponseTemplate<DTO> response = new ResponseTemplate<>(savedDto, HttpStatus.OK.toString(), false, "Entity updated successfully");
//            return ResponseEntity.ok(response);
//        } catch (Exception ex) {
//            // If an exception occurs, return 500 Internal Server Error
//            ResponseTemplate<DTO> response = new ResponseTemplate<>(null, HttpStatus.INTERNAL_SERVER_ERROR.toString(), true, "Internal Server Error");
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }

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
