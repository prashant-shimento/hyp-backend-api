package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hyp.response.ResponseTemplate;
import com.hyp.service.BaseService;
import com.hyp.service.TranslationService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class BaseController<DTO, T, ID> {

	@Autowired
	protected BaseService<T, ID> service;
	
	@Autowired
    protected TranslationService<DTO, T> translationService;

	@GetMapping
	public ResponseEntity<ResponseTemplate> getAll() {
		List<T> entities = service.findAll();
		List<DTO> dtoEntities = translationService.getDtoList(entities);
		ResponseTemplate response = new ResponseTemplate(dtoEntities, false, "success");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ResponseTemplate> getById(@PathVariable ID id) {
		try {
			T entity = service.findById(id);
			if (entity != null) {
				DTO dto = translationService.getDto(entity);
				ResponseTemplate response = new ResponseTemplate(Collections.singletonList(dto), false, "success");
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.notFound().build();
			}
		} catch (Exception ex) {
			ResponseTemplate response = new ResponseTemplate(null, true, ex.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@PostMapping
	public ResponseEntity<ResponseTemplate> create(@RequestBody DTO dto) {
		try {
			T entity = translationService.getEntity(dto);
			T savedEntity = service.save(entity);
			dto = translationService.getDto(savedEntity);
			//Need to revisit returning list logic
			ResponseTemplate response = new ResponseTemplate(Collections.singletonList(dto), false, "success");
			return ResponseEntity.ok(response);
		} catch (Exception ex) {
			ResponseTemplate response = new ResponseTemplate(null, true, ex.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity<ResponseTemplate> update(@PathVariable ID id, @RequestBody DTO dto) {
		try {
			if (service.findById(id) != null) {
				T entity = translationService.getEntity(dto);
				T updatedEntity = service.save(entity);
				dto = translationService.getDto(updatedEntity);
				//Need to revisit returning list logic
				ResponseTemplate response = new ResponseTemplate(Collections.singletonList(dto), false, "success");
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.notFound().build();
			}
		} catch (Exception ex) {
			ResponseTemplate response = new ResponseTemplate(null, true, ex.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
	
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
	public ResponseEntity<ResponseTemplate> delete(@PathVariable ID id) {
		try {
			if (service.findById(id) != null) {
				service.deleteById(id);
				return ResponseEntity.ok().build();
			} else {
				return ResponseEntity.notFound().build();
			}
		} catch (Exception ex) {
			ResponseTemplate response = new ResponseTemplate(null, true, ex.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
