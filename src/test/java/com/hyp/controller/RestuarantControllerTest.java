package com.hyp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hyp.dto.RestaurantDto;
import com.hyp.entity.Item;
import com.hyp.entity.Restaurant;
import com.hyp.response.Response;
import com.hyp.service.RestaurantService;
import com.hyp.translation.RestaurantTranslation;
import com.hyp.util.QueryUtils;
import java.util.Collections;
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
public class RestuarantControllerTest {

    @Mock
    private RestaurantService restaurantService;

    @Mock
    private RestaurantTranslation restaurantTranslation;

    @InjectMocks
    private BaseController<RestaurantDto, Restaurant, String> restaurantController =
            new BaseController<RestaurantDto, Restaurant, String>() {};

    private Restaurant restaurant1;
    private Restaurant restaurant2;

    private RestaurantDto restaurant1Dto;
    private RestaurantDto restaurant2Dto;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        restaurant1 = new Restaurant();
        restaurant1.setId("1");
        restaurant1.setActive(true);
        restaurant1.setCountry("India");
        restaurant1.setCity("Hyderbad");
        restaurant1.setContact("909011122");
        restaurant1.setMenuSharingCode("ffttftft");

        restaurant1Dto = new RestaurantDto();
        restaurant1Dto.setId("1");
        restaurant1Dto.setActive(true);
        restaurant1Dto.setCountry("India");
        restaurant1Dto.setCity("Hyderbad");
        // restaurant1Dto.setContact("909011122");
        restaurant1Dto.setMenuSharingCode("ffttftft");

        restaurant2 = new Restaurant();
        restaurant2.setId("2");
        restaurant2.setActive(true);
        restaurant2.setCountry("India");
        restaurant2.setCity("Hyderbad");
        restaurant2.setContact("90911122");
        restaurant2.setMenuSharingCode("okokok");

        restaurant2Dto = new RestaurantDto();
        restaurant2Dto.setId("2");
        restaurant2Dto.setActive(true);
        restaurant2Dto.setCountry("India");
        restaurant2Dto.setCity("Hyderbad");
        // restaurant2Dto.setContact("90911122");
        restaurant2Dto.setMenuSharingCode("okokok");
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
            ResponseEntity<Response> responseEntity = restaurantController.getAll(queryParams);
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
                responseEntity = restaurantController.getAll(queryParams);
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
        String id = "1";

        try {
            when(restaurantService.findById(id)).thenReturn(restaurant2);
            when(restaurantTranslation.getDto(restaurant2)).thenReturn(restaurant2Dto);

            // act
            ResponseEntity<Response> responseEntity = restaurantController.getById(id);

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
        when(restaurantService.findById(id)).thenThrow(new RuntimeException("Server error"));
        ResponseEntity<Response> responseEntity = restaurantController.getById(id);

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
        when(restaurantService.findById(id)).thenReturn(null);

        // act
        ResponseEntity<Response> responseEntity = restaurantController.getById(id);

        // aassert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // UTC for find By Id Server error
    @Test
    void testFindById_InternalServerError() {
        // Arrange
        String id = "111ff";
        when(restaurantService.findById(id)).thenThrow(new RuntimeException("Server error"));

        // act
        ResponseEntity<Response> responseEntity = restaurantController.getById(id);

        // Assert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody().isError()).isTrue();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
        assertThat(responseEntity.getBody().getData()).isNull();
    }

    // UTC for create on success
    @Test
    public void testForCreate_success() {

        // arrange
        when(restaurantTranslation.getEntity(restaurant1Dto)).thenReturn(restaurant1);
        when(restaurantService.save(restaurant1)).thenReturn(restaurant1);
        when(restaurantTranslation.getDto(restaurant1)).thenReturn(restaurant1Dto);

        // act
        ResponseEntity<Response> responseEntity = restaurantController.create(restaurant1Dto);

        // assert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(restaurant1Dto));
        assertThat(responseEntity.getBody().isError()).isFalse();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
    }

    // UTC for create on server error

    @Test
    public void testForCreate_InternalServerError() {

        when(restaurantTranslation.getEntity(restaurant1Dto)).thenReturn(restaurant1);
        when(restaurantService.save(restaurant1)).thenThrow(new RuntimeException("Server error"));

        ResponseEntity<Response> responseEntity = restaurantController.create(restaurant1Dto);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().isError()).isTrue();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
    }

    // UTC for update success
    @Test
    public void testForUpdate() {
        // Arrange
        when(restaurantService.findById("123455")).thenReturn(restaurant1);
        doNothing().when(restaurantTranslation).updateEntityFromDto(restaurant1Dto, restaurant1);
        when(restaurantService.save(restaurant1)).thenReturn(restaurant1);
        when(restaurantTranslation.getDto(restaurant1)).thenReturn(restaurant1Dto);

        // act
        ResponseEntity<Response> responseEntity = restaurantController.update("123455", restaurant1Dto);

        // assert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(restaurant1Dto));
        assertThat(responseEntity.getBody().isError()).isFalse();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
    }

    // UTC for update not found
    @Test
    void testForUpdate_NotFound() {
        when(restaurantService.findById("123455")).thenReturn(null);

        ResponseEntity<Response> response = restaurantController.update("123455", restaurant1Dto);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    // Test update - server error
    @Test
    void testForUpdate_InternalServerError() throws Exception {
        when(restaurantService.findById("123455")).thenThrow(new RuntimeException("Server error"));

        ResponseEntity<Response> responseEntity = restaurantController.update("123455", restaurant1Dto);

        assertNotNull(responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().isError()).isTrue();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
    }

    // UTC for delete - success
    @Test
    public void testForDelete_SUCCESS() {
        when(restaurantService.findById("1")).thenReturn(restaurant1);
        doNothing().when(restaurantService).deleteById("1");

        ResponseEntity<Response> responseEntity = restaurantController.delete("1");
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNull();
    }

    // UTC for delete - not found case
    @Test
    public void testForDelete_NotFound() {
        when(restaurantService.findById("123455")).thenReturn(null);

        ResponseEntity<Response> responseEntity = restaurantController.delete("123455");

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNull();
    }

    // Test for Server error while deleting
    @Test
    public void testForDelete_InternalServerError() {
        when(restaurantService.findById("123455")).thenThrow(new RuntimeException("Server error"));

        ResponseEntity<Response> responseEntity = restaurantController.delete("123455");

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().isError()).isTrue();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
    }

    // Test for deletion while the id is null
    @Test
    public void testForDelete_IdIsNull() {
        String id = null;

        when(restaurantService.findById(id)).thenThrow(new RuntimeException("Server error"));

        ResponseEntity<Response> responseEntity = restaurantController.delete(id);

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody()).isNotNull();
    }
}
