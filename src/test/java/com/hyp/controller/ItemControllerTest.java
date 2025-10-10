package com.hyp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hyp.dto.ItemDto;
import com.hyp.entity.Item;
import com.hyp.response.Response;
import com.hyp.service.ItemService;
import com.hyp.translation.ItemTranslation;
import com.hyp.util.QueryUtils;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class ItemControllerTest {

    @Mock
    private ItemService itemService;

    @Mock
    private ItemTranslation itemTranslation;

    @InjectMocks
    private BaseListController<ItemDto, Item, String> itemController =
            new BaseListController<ItemDto, Item, String>() {};

    private Item item1;
    private Item item2;

    private ItemDto item1Dto;
    private ItemDto item2Dto;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        item1 = new Item();
        item1.setId("123");
        item1.setItemDescription("Item 1");
        item1.setItemRank("1");
        item1.setItemAllowAddon("0");
        item1.setInStock(false);
        item1.setPrice("567.00");
        item1.setItemName("Chicken Burger");
        item1.setActive("1");
        item1.setItemAttributeId("1");

        // Item 2
        item2 = new Item();
        item2.setId("7878123");
        item2.setItemDescription("Item 2");
        item2.setItemRank("1");
        item2.setItemAllowAddon("0");
        item2.setInStock(true);
        item2.setPrice("447.00");
        item2.setItemName("Fish Fillet Burger Combo");
        item2.setActive("1");
        item2.setItemAttributeId("1");

        // Item DTO
        item1Dto = new ItemDto();
        item1Dto.setId("123");
        item1Dto.setItemDescription("Item 1");
        item1Dto.setItemRank("1");
        item1Dto.setItemAllowAddon("0");
        item1Dto.setInStock(false);
        item1Dto.setPrice("567.00");
        item1Dto.setItemName("Chicken Burger");
        // item1Dto.setActive("1");
        item1Dto.setItemAttributeId("1");

        // Item 2 Dto
        item2Dto = new ItemDto();
        item2Dto.setId("7878123");
        item2Dto.setItemDescription("Item 2");
        item2Dto.setItemRank("1");
        item2Dto.setItemAllowAddon("0");
        item2Dto.setInStock(true);
        item2Dto.setPrice("447.00");
        item2Dto.setItemName("Fish Fillet Burger Combo");
        // item2Dto.setActive("1");
        item2Dto.setItemAttributeId("1");
    }

    // Test to get all

    @Test
    public void testForGetAll_SUCCESS() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("itemName_eq", "Chicken Burger");
        Query mockQuery = mock(Query.class);

        // MockedStatic method that you can use to create a mock object for a static method.
        try (MockedStatic<QueryUtils> mockedStatic = Mockito.mockStatic(QueryUtils.class)) {
            mockedStatic
                    .when(() -> QueryUtils.getFilterQuery(
                            queryParams, QueryUtils.getAllowedParameters(Item.class.getSimpleName())))
                    .thenReturn(mockQuery);
            ResponseEntity<Response> responseEntity = itemController.getAll(queryParams);
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().isError()).isFalse();
            assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Expected exception");
            e.printStackTrace();
        }
    }

    @Test
    public void testForGetAll_BadRequest() {
        // Arrange
        Map<String, String> queryParams = new HashMap<>();
        try (MockedStatic<QueryUtils> mockedStatic = Mockito.mockStatic(QueryUtils.class)) {
            mockedStatic
                    .when(() -> QueryUtils.getFilterQuery(
                            queryParams, QueryUtils.getAllowedParameters(Item.class.getSimpleName())))
                    .thenThrow(new IllegalArgumentException("Invalid query parameters"));

            // Act
            ResponseEntity<Response> responseEntity = null;
            try {
                responseEntity = itemController.getAll(queryParams);
            } catch (IllegalArgumentException e) {
                // Assert
                assertThat(e.getMessage()).isEqualTo("Invalid query parameters");
            }

            if (responseEntity != null) {
                assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(responseEntity.getBody().isError()).isTrue();
                assertThat(responseEntity.getBody().getMessage()).isEqualTo("Invalid query parameters");
            }
        }
    }

    // Test to Get By ID
    @Test
    public void testForGetById_SUCCESS() {
        // arrange
        String id = "7878123";

        try {
            when(itemService.findByIdWithReference(id, Item.class)).thenReturn(item2);
            when(itemTranslation.getDto(item2)).thenReturn(item2Dto);

            // act
            ResponseEntity<Response> responseEntity = itemController.getById(id);

            // assert
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody().isError()).isFalse();
            assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");

        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Expected exception");
            e.printStackTrace();
        }
    }

    @Test
    public void testForGetById_BadRequest() {
        // arrange
        String id = null;
        when(itemService.findByIdWithReference(id, Item.class)).thenThrow(new RuntimeException("Server error"));
        ResponseEntity<Response> responseEntity = itemController.getById(id);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody().isError()).isTrue();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
        assertThat(responseEntity.getBody().getData()).isNull();
    }

    // UTC for find By Id - Not found
    @Test
    void testForFindById_NotFound() {
        // Arrange
        String id = "787ffgg";
        when(itemService.findByIdWithReference(id, Item.class)).thenReturn(null);

        // act
        ResponseEntity<Response> responseEntity = itemController.getById(id);

        // aassert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // UTC for find By Id Server error
    @Test
    void testForFindById_InternalServerError() {
        // Arrange
        String id = "111ff";
        when(itemService.findByIdWithReference(id, Item.class)).thenThrow(new RuntimeException("Server error"));

        // act
        ResponseEntity<Response> responseEntity = itemController.getById(id);

        // Assert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody().isError()).isTrue();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
        assertThat(responseEntity.getBody().getData()).isNull();
    }
}
