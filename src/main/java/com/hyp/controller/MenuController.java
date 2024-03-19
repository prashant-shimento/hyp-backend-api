package com.hyp.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.entity.Category;
import com.hyp.response.ResponseTemplate;
import com.hyp.service.CategoryService;

import lombok.Data;

@Data
@RestController
@RequestMapping("/api/menu")
public class MenuController {

	private final CategoryService categoryService;

	@GetMapping()
	public ResponseEntity<ResponseTemplate> getCategoryItems() {
		ResponseTemplate response = new ResponseTemplate();
		try {
			List<Category> categoryItems = categoryService.getCategoryItems();
			response.setData(categoryItems);
			response.setError(false);
			response.setMessage("Data retrieved successfully.");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response.setError(true);
			response.setMessage("Error occurred while fetching data: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}

	}
}