package com.hyp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.hyp.entity.Category;
import com.hyp.request.PosDataRequest;
import com.hyp.response.Response;
import com.hyp.service.CategoryService;
import com.hyp.service.MenuService;
import com.hyp.service.PosService;
import java.util.List;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Data
@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    PosService posService;

    @Autowired
    MenuService menuService;

    @PostMapping("/extract")
    public PosDataRequest extract(@RequestBody JsonNode rawJson, @RequestParam boolean useExternalId) throws Exception {
        return menuService.extract(rawJson, useExternalId);
    }

    @PostMapping("/import")
    public ResponseEntity<Response> save(@RequestBody PosDataRequest posDataRequest) {
        posService.savePosData(posDataRequest);
        Response response = new Response(null, false, "Menu Imported");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/category")
    public ResponseEntity<Response> getCategoryItems(@RequestParam String restaurantId) {
        Response response = new Response();
        try {
            List<Category> categories = categoryService.getAllCategoryItems(restaurantId);
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
    public ResponseEntity<Response> getCategoryDetailsById(@PathVariable String categoryId) {
        Response response = new Response();
        try {
            List<Category> categories = categoryService.getCategoryItemsById(categoryId);
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
