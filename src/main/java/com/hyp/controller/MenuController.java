package com.hyp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.entity.Category;
import com.hyp.entity.Item;
import com.hyp.response.ResponseTemplate;
import com.hyp.service.CategoryService;
import com.hyp.service.ItemService;

import lombok.Data;

@RestController
@Data
public class MenuController {

	private final CategoryService categoryService;
	private final ItemService itemService;

	@GetMapping("/categories")
	public ResponseEntity<ResponseTemplate> getCategories(
			@RequestParam(required = false) Map<String, Object> parameters,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize,
			@RequestParam(required = false) String sortBy) {
		ResponseTemplate response = new ResponseTemplate();
		try {
			List<Category> categories = categoryService.getCategories(parameters, page, pageSize, sortBy);
			response.setData(categories);
			response.setError(false);
			response.setMessage("Categories retrieved successfully.");
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			response.setError(true);
			response.setMessage("Error occurred while retrieving categories: " + e.getMessage());
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/items")
	public ResponseEntity<ResponseTemplate> getItems(@RequestParam(required = false) Map<String, Object> parameters,
			@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize,
			@RequestParam(required = false) String sortBy) {
		ResponseTemplate response = new ResponseTemplate();
		try {
			List<Item> items = itemService.getItems(parameters, page, pageSize, sortBy);
			response.setData(items);
			response.setError(false);
			response.setMessage("Items retrieved successfully.");
			return new ResponseEntity<>(response, HttpStatus.OK);
		} catch (Exception e) {
			response.setError(true);
			response.setMessage("Error occurred while retrieving items: " + e.getMessage());
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}