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

import com.hyp.response.Response;
import com.hyp.service.TranslationService;

public abstract class BaseListController<DTO, T, ID> {

	@Autowired
	protected MongoRepository<T, ID> repository;
	
	@Autowired
    protected TranslationService<DTO, T> translationService;

	@GetMapping
	public ResponseEntity<Response> getAll() {
		List<T> entities = repository.findAll();
		List<DTO> dtoEntities = translationService.getDtoList(entities);
		Response response = new Response(dtoEntities, false, "success");
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}")
	public ResponseEntity<Response> getById(@PathVariable ID id) {
		try {
			Optional<T> optionalEntity = repository.findById(id);
			if (optionalEntity.isPresent()) {
				DTO dto = translationService.getDto(optionalEntity.get());
				List<DTO> entity = Collections.singletonList(dto);
				Response response = new Response(entity, false, "success");
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.notFound().build();
			}
		} catch (Exception ex) {
			Response response = new Response(null, true, ex.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
