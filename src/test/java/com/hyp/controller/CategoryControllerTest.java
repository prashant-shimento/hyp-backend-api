package com.hyp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.hyp.dto.CategoryDto;
import com.hyp.entity.Category;
import com.hyp.entity.Item;
import com.hyp.response.Response;
import com.hyp.service.CategoryService;
import com.hyp.translation.CategoryTranslation;
import com.hyp.util.QueryUtils;

@ExtendWith(MockitoExtension.class)
public class CategoryControllerTest {
	@Mock
	private CategoryService categoryService;

	@Mock
	private CategoryTranslation categoryTranslation;

	@InjectMocks
	private BaseController<CategoryDto, Category, String> categoryController = new BaseController<CategoryDto, Category, String>() {
	};
	private Category category;
	private CategoryDto categoryDto;

	@BeforeEach
	public void setup() {

		category = new Category();
		category.setParentCategoryId("12345");
		category.setCategoryImageUrl("https://example.com/image.jpg");
		category.setCategoryTimings("9:00 AM - 9:00 PM");
		category.setActive("true");
		category.setCategoryName("Electronics");
		category.setCategoryRank("1");

		categoryDto = new CategoryDto();
		category.setParentCategoryId("12345");
		category.setCategoryImageUrl("https://example.com/image.jpg");
		category.setCategoryTimings("9:00 AM - 9:00 PM");
		category.setActive("true");
		category.setCategoryName("Electronics");
		category.setCategoryRank("1");

		Item item = new Item();
		item.setItemDescription("A premium smartphone");
		item.setItemRank("1");
		item.setItemAllowAddon("true");
		item.setVariationGroupName("Color");
		item.setAddon(Arrays.asList("Case", "Screen Protector"));
		item.setItemFavorite("true");
		item.setItemTax(Arrays.asList("18% GST", "2% CST"));
		item.setInStock(true);
		item.setItemAllowVariation("true");
		item.setVariation(Arrays.asList("Red", "Blue", "Black"));
		item.setItemPackingCharges("20");
		item.setIgnoreTaxes("false");
		item.setPrice("69999");
		item.setItemOrderType(Arrays.asList("Online", "In-store"));
		item.setMinimumPreparationTime("15 minutes");
		item.setItemAddonBasedOn("Customer Choice");
		item.setItemImageUrl("https://example.com/item-image.jpg");
		item.setItemName("Smartphone X Pro");
		item.setCuisine(Arrays.asList("Global", "Fusion"));
		item.setActive("true");
		item.setIgnoreDiscounts("false");
		item.setItemAttributeId("attr-98765");
		item.setIsRecommend("true");
		item.setGstType("Standard");
		item.setItemCategoryId("cat-12345");
		item.setAutoTurnOnTime(LocalDateTime.now().plusHours(1));

		category.setItems(Arrays.asList(item));

	}

	@Test
	public void getCategoryById_Success() {
	    when(categoryService.findById("12345")).thenReturn(category);
	    when(categoryTranslation.getDto(category)).thenReturn(categoryDto);
	    ResponseEntity<Response> responseEntity = categoryController.getById("12345");
	    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
	    assertThat(responseEntity.getBody()).isNotNull();
	    assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(categoryDto));
	    assertThat(responseEntity.getBody().isError()).isFalse();
	    assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
	}

	@Test
	public void getCategoryById_NotFound() {
	    when(categoryService.findById("12345")).thenReturn(null);
	    ResponseEntity<Response> responseEntity = categoryController.getById("12345");
	    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	    assertThat(responseEntity.getBody()).isNull();
	}

	@Test
	public void createCategory_Success() {
	    when(categoryTranslation.getEntity(categoryDto)).thenReturn(category);
	    when(categoryService.save(category)).thenReturn(category);
	    when(categoryTranslation.getDto(category)).thenReturn(categoryDto);

	    ResponseEntity<Response> responseEntity = categoryController.create(categoryDto);

	    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
	    assertThat(responseEntity.getBody()).isNotNull();
	    assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(categoryDto));
	    assertThat(responseEntity.getBody().isError()).isFalse();
	    assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
	}

	@Test
	public void updateCategory_Success() {
	    when(categoryService.findById("12345")).thenReturn(category);

	    doNothing().when(categoryTranslation).updateEntityFromDto(categoryDto, category);

	    when(categoryService.save(any(Category.class))).thenReturn(category);

	    when(categoryTranslation.getDto(category)).thenReturn(categoryDto);

	    ResponseEntity<Response> successResponseEntity = categoryController.update("12345", categoryDto);

	    assertThat(successResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
	    assertThat(successResponseEntity.getBody()).isNotNull();
	    assertThat(successResponseEntity.getBody().getData()).isEqualTo(Collections.singletonList(categoryDto));
	    assertThat(successResponseEntity.getBody().isError()).isFalse();
	    assertThat(successResponseEntity.getBody().getMessage()).isEqualTo("success");
	}

	@Test
	public void updateCategory_NotFound() {
	    when(categoryService.findById("12345")).thenReturn(null);

	    ResponseEntity<Response> responseEntity = categoryController.update("12345", categoryDto);

	    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	    assertThat(responseEntity.getBody()).isNull();
	}

	@Test
	public void deleteCategory_Success() {
	    when(categoryService.findById(category.getId())).thenReturn(category);

	    doNothing().when(categoryService).deleteById(category.getId());

	    ResponseEntity<Response> successResponseEntity = categoryController.delete(category.getId());

	    assertThat(successResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
	    assertThat(successResponseEntity.getBody()).isNull();
	}

	@Test
	public void deleteCategory_NotFound() {
	    when(categoryService.findById(category.getId())).thenReturn(null);

	    ResponseEntity<Response> successResponseEntity = categoryController.delete(category.getId());

	    assertThat(successResponseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	    assertThat(successResponseEntity.getBody()).isNull();
	}

	@Test
	public void getAllCategory_Success() {
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("categoryName_eq", "Electronics");
		Query mockQuery = mock(Query.class);
		when(QueryUtils.getFilterQuery(queryParams, QueryUtils.getAllowedParameters(Category.class.getSimpleName())))
				.thenReturn(mockQuery);

		ResponseEntity<Response> responseEntity = categoryController.getAll(queryParams);

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getBody().isError()).isFalse();
		assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
	}

	@Test
	public void getAllCategory_BadRequest() {
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("categoryName", "Electronics");

		ResponseEntity<Response> responseEntity = categoryController.getAll(queryParams);

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(responseEntity.getBody().isError()).isTrue();
		assertThat(responseEntity.getBody().getMessage()).isEqualTo("Invalid query parameters");
	}
}
