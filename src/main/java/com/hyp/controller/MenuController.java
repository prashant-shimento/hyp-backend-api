package com.hyp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.entity.Category;
import com.hyp.response.ResponseTemplate;
import com.hyp.service.CategoryService;

import lombok.Data;

@Data
@RestController
@RequestMapping("/menu")
public class MenuController {

	private final CategoryService categoryService;

	@GetMapping("/category")
	public ResponseEntity<ResponseTemplate> getCategoryDetails() {
		ResponseTemplate response = new ResponseTemplate();
		try {
			List<Category> categories = categoryService.getAllCategoryItems();
			response.setData(categories);
			response.setMessage("Categories retrieved successfully.");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.setError(true);
			response.setMessage("Error occurred while fetching categories: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("/category/{categoryId}")
	public ResponseEntity<ResponseTemplate> getCategoryDetailsById(@PathVariable String categoryId) {
		ResponseTemplate response = new ResponseTemplate();
		try {
			List<Category> categories;
			if (categoryId != null) {
				categories = categoryService.getCategoryItemsById(categoryId);
			} else {
				categories = categoryService.getAllCategoryItems();
			}
			response.setData(categories);
			response.setMessage("Categories retrieved successfully.");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.setError(true);
			response.setMessage("Error occurred while fetching categories: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}