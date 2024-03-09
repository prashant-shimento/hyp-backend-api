package com.hyp.controller;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.hyp.response.ResponseTemplate;
import com.hyp.service.TranslationService;

public abstract class BaseListController<DTO, T, ID> {

	@Autowired
	protected MongoRepository<T, ID> repository;
	
	@Autowired
    protected TranslationService<DTO, T> translationService;

	@GetMapping
	public ResponseEntity<ResponseTemplate> getAll() {
		List<T> entities = repository.findAll();
		List<DTO> dtoEntities = translationService.getDtoList(entities);
		ResponseTemplate response = new ResponseTemplate(dtoEntities, false, "success");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ResponseTemplate> getById(@PathVariable ID id) {
		try {
			Optional<T> optionalEntity = repository.findById(id);
			if (optionalEntity.isPresent()) {
				DTO dto = translationService.getDto(optionalEntity.get());
				List<DTO> entity = Collections.singletonList(dto);
				ResponseTemplate response = new ResponseTemplate(entity, false, "success");
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.notFound().build();
			}
		} catch (Exception ex) {
			ResponseTemplate response = new ResponseTemplate(null, true, ex.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
