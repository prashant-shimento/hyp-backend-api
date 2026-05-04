package com.hyp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.hyp.entity.Category;
import com.hyp.entity.Item;
import com.hyp.request.PosDataRequest;
import com.hyp.request.UpdateItemsRequest;
import com.hyp.response.Response;
import com.hyp.service.CategoryService;
import com.hyp.service.MenuService;
import com.hyp.service.PosServiceFactory;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Data
@RestController
@RequestMapping("/menu")
public class MenuController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    PosServiceFactory posServiceFactory;

    @Autowired
    MenuService menuService;

    @PostMapping("/extract")
    public PosDataRequest extract(@RequestBody JsonNode rawJson, @RequestParam boolean useExternalId) throws Exception {
        return menuService.extract(rawJson, useExternalId);
    }

    @PostMapping("/import")
    public ResponseEntity<Response> save(@RequestBody PosDataRequest posDataRequest) {
        var allRestaurantIds = posDataRequest.getRestaurants().stream()
                .map(it -> it.getRestaurantid())
                .toList();

        if (allRestaurantIds == null || allRestaurantIds.isEmpty()) {
            var response = Response.builder()
                    .error(true)
                    .message("At least one restaurant id needed")
                    .build();

            return ResponseEntity.badRequest().body(response);
        }

        var posService = posServiceFactory.forRestaurant(allRestaurantIds.get(0));

        log.info("Import all restaurants {}", allRestaurantIds);

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

    @PatchMapping("/category/{categoryId}/items")
    public ResponseEntity<Response> updateCategoryItems(
            @PathVariable String categoryId, @RequestBody UpdateItemsRequest request) {
        Response response = new Response();
        try {
            List<Category> categories = categoryService.getCategoryItemsById(categoryId);
            if (categories.isEmpty() || categories.get(0).getItems() == null) {
                response.setError(true);
                response.setMessage("No items found for the given category.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            List<Item> itemsToUpdate = categories.get(0).getItems();
            if (request.getItemIds() != null && !request.getItemIds().isEmpty()) {
                itemsToUpdate = itemsToUpdate.stream()
                        .filter(item -> request.getItemIds().contains(item.getId()))
                        .collect(Collectors.toList());
            }
            categoryService.updateItems(itemsToUpdate, request.getFields());
            response.setMessage("items updated successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.setError(true);
            response.setMessage("Error occurred while updating items: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
