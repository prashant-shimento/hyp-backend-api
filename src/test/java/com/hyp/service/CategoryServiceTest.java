package com.hyp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyp.entity.Category;
import com.hyp.repository.CategoryRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private BaseServiceImpl<Category, String> baseService = new BaseServiceImpl<Category, String>() {};

    private CategoryService categoryService;
    private List<Category> mockCategories;
    private Category category;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService();
        ReflectionTestUtils.setField(categoryService, "categoryRepository", categoryRepository);
        ReflectionTestUtils.setField(categoryService, "mongoTemplate", mongoTemplate);

        mockCategories = new ArrayList<>();
        category = new Category();
        category.setId("123");
        category.setCategoryName("Sample Categorys");
        category.setRestaurantId("123654");
        mockCategories.add(category);
    }

    @Test
    void categoryItemsById_Succcess() {
        String categoryId = "123";

        @SuppressWarnings("unchecked")
        AggregationResults<Category> mockResults = mock(AggregationResults.class);
        when(mockResults.getMappedResults()).thenReturn(mockCategories);

        when(mongoTemplate.aggregate(any(Aggregation.class), eq("categories"), eq(Category.class)))
                .thenReturn(mockResults);

        List<Category> result = categoryService.getCategoryItemsById(categoryId);

        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("123");
        assertThat(result.get(0).getCategoryName()).isEqualTo("Sample Categorys");

        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("categories"), eq(Category.class));
    }

    @Test
    void categoryItemsById_NotFound() {
        String categoryId = "12365";

        @SuppressWarnings("unchecked")
        AggregationResults<Category> mockResults = mock(AggregationResults.class);
        when(mockResults.getMappedResults()).thenReturn(new ArrayList<>());

        when(mongoTemplate.aggregate(any(Aggregation.class), eq("categories"), eq(Category.class)))
                .thenReturn(mockResults);

        List<Category> result = categoryService.getCategoryItemsById(categoryId);

        assertThat(result).isNotNull().isEmpty();

        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("categories"), eq(Category.class));
    }

    @Test
    void getAllCategoryItemsTest() {
        String restaurantId = "123654";

        @SuppressWarnings("unchecked")
        AggregationResults<Category> mockResults = mock(AggregationResults.class);
        when(mockResults.getMappedResults()).thenReturn(mockCategories);

        when(mongoTemplate.aggregate(any(Aggregation.class), eq("categories"), eq(Category.class)))
                .thenReturn(mockResults);

        List<Category> result = categoryService.getAllCategoryItems(restaurantId);

        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0).getRestaurantId()).isEqualTo("123654");
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("categories"), eq(Category.class));
    }

    @Test
    void getAllCategoryItems_NotFound() {
        String ExpectedRestaurantId = "2565656";

        @SuppressWarnings("unchecked")
        AggregationResults<Category> mockResults = mock(AggregationResults.class);
        when(mockResults.getMappedResults()).thenReturn(mockCategories);

        when(mongoTemplate.aggregate(any(Aggregation.class), eq("categories"), eq(Category.class)))
                .thenReturn(mockResults);

        List<Category> ActualRestaurantId =
                categoryService.getAllCategoryItems(mockCategories.get(0).getRestaurantId());

        assertNotEquals(ActualRestaurantId.get(0).getRestaurantId(), ExpectedRestaurantId);

        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("categories"), eq(Category.class));
    }

    @Test
    void categoryById_Success() {
        when(categoryRepository.findById("123")).thenReturn(Optional.of(category));

        Category result = baseService.findById("123");
        assertNotNull(result);
        assertThat(result.getId()).isEqualTo("123");
        verify(categoryRepository, times(1)).findById("123");
    }

    @Test
    void categoryById_NotFound() {
        when(categoryRepository.findById("456")).thenReturn(Optional.empty());

        Category result = baseService.findById("456");
        assertThat(result).isNull();
        verify(categoryRepository, times(1)).findById("456");
    }

    @Test
    void addCategory_Success() {
        when(categoryRepository.save(category)).thenReturn(category);

        Category addedCategory = baseService.save(category);

        assertNotNull(addedCategory);
        assertThat(addedCategory.getCategoryName()).isEqualTo(category.getCategoryName());
        assertThat(addedCategory.getRestaurantId()).isEqualTo(category.getRestaurantId());
        verify(categoryRepository, times(1)).save(category);
    }

    @Test
    void findAllCategories_Success() {
        when(categoryRepository.findAll()).thenReturn(mockCategories);

        List<Category> categories = baseService.findAll();

        assertNotNull(categories);
        assertThat(categories.size()).isEqualTo(1);
        assertThat(categories.get(0).getId()).isEqualTo("123");
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    void updateCategory_Success() {
        when(categoryRepository.save(category)).thenReturn(category);

        Category updatedCategory = baseService.update(category);

        assertNotNull(updatedCategory);
        assertThat(updatedCategory.getCategoryName()).isEqualTo(category.getCategoryName());
        verify(categoryRepository, times(1)).save(category);
    }

    @Test
    void deleteCategory_Success() {
        doNothing().when(categoryRepository).deleteById("123");

        baseService.deleteById("123");

        verify(categoryRepository, times(1)).deleteById("123");
    }
}
