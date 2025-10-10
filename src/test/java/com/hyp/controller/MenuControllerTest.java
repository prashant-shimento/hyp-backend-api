package com.hyp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.hyp.entity.Category;
import com.hyp.response.Response;
import com.hyp.service.CategoryService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class MenuControllerTest {

    @Mock
    CategoryService categoryService;

    MenuController menuController;

    private Category category1;
    private Category category2;

    @BeforeEach
    public void setup() {
        menuController = new MenuController(categoryService);
        ReflectionTestUtils.setField(menuController, "categoryService", categoryService);
        category1 = new Category();
        category1.setId("76342");
        category1.setCategoryName("Rice");
        category1.setCategoryImageUrl("something.url://@@338989");
        category1.setRestaurantId("4758");

        category2 = new Category();
        category2.setId("12345");
        category2.setCategoryName("Curry");
        category2.setCategoryImageUrl("curry.url://@@338989");
        category2.setRestaurantId("4758");
    }

    // getCategoryItems
    @Test
    public void testForGetCategoryItems_SUCCESS() {
        String restaurantId = "4758";
        List<Category> mockkedCategories = Arrays.asList(category1, category2);

        when(categoryService.getAllCategoryItems(restaurantId)).thenReturn(mockkedCategories);

        List<Category> categories = categoryService.getAllCategoryItems(restaurantId);

        ResponseEntity<Response> responseEntity = menuController.getCategoryDetailsById(restaurantId);

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // getCategoryItems when Id is null
    @Test
    public void testForGetCategoryItems_IdIsNull() {
        String restaurantId = null;

        when(categoryService.getAllCategoryItems(restaurantId)).thenThrow(new RuntimeException("Id is null"));

        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> categoryService.getAllCategoryItems(restaurantId));

        assertThat(exception).isInstanceOf(RuntimeException.class);
        // assertThat(exception.getMessage()).isEqualTo("Id is null");
    }

    // get category Items when the id does not exist
    @Test
    public void testForGetCategoryItems_DoesNotExist() {
        String restaurantId = "4751111118";

        when(categoryService.getAllCategoryItems(restaurantId)).thenReturn(null);

        List<Category> response = categoryService.getAllCategoryItems(restaurantId);

        assertThat(response).isNull();
    }

    // Get category by category By Id
    @Test
    public void testForGetCategoryDetailsById() {
        String categoryId = "123";
        List<Category> mockkedCategories = Arrays.asList(category1, category2);

        when(categoryService.getCategoryItemsById(categoryId)).thenReturn(mockkedCategories);

        List<Category> categories = categoryService.getCategoryItemsById(categoryId);

        ResponseEntity<Response> responseEntity = menuController.getCategoryDetailsById(categoryId);

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Get category by category By Id - when Id is null
    @Test
    public void testForGetCategoryDetailsById_IdIsNull() {
        String categoryId = null;

        when(categoryService.getCategoryItemsById(categoryId)).thenThrow(new RuntimeException("Id is null"));

        RuntimeException exception =
                assertThrows(RuntimeException.class, () -> categoryService.getCategoryItemsById(categoryId));

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo("Id is null");
    }

    // Get category by category By Id - Does not exist
    @Test
    public void testForGetCategoryDetailsById_DoesNotExist() {
        String categoryId = "1ff4523";

        when(categoryService.getCategoryItemsById(categoryId)).thenReturn(null);

        List<Category> response = categoryService.getCategoryItemsById(categoryId);

        assertThat(response).isNull();
    }
}
