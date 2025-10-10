package com.hyp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hyp.dto.VariationDto;
import com.hyp.entity.Item;
import com.hyp.entity.Variation;
import com.hyp.response.Response;
import com.hyp.service.VariationService;
import com.hyp.translation.VariationTranslation;
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
public class VariationControllerTest {

    @Mock
    private VariationService variationService;

    @Mock
    private VariationTranslation variationTranslation;

    @InjectMocks
    private BaseListController<VariationDto, Variation, String> variationController =
            new BaseListController<VariationDto, Variation, String>() {};

    private Variation variation1;
    private Variation variation2;

    private VariationDto variation1Dto;
    private VariationDto variation2Dto;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        variation1 = new Variation();
        variation1.setId("1");
        variation1.setName("2 Pieces");
        variation1.setPrice("233");
        variation1.setRestaurantId("4758");
        variation1.setVariationId("5656");

        variation1Dto = new VariationDto();
        variation1Dto.setId("1");
        variation1Dto.setName("2 Pieces");
        variation1Dto.setPrice("233");
        variation1Dto.setRestaurantId("4758");
        // variation1Dto.setVariation_id("5656");

        variation2 = new Variation();
        variation2.setId("2");
        variation2.setName("4 Pieces");
        variation2.setPrice("422");
        variation2.setRestaurantId("4758");
        variation2.setVariationId("5657");

        variation2Dto = new VariationDto();
        variation2Dto.setId("2");
        variation2Dto.setName("4 Pieces");
        variation2Dto.setPrice("422");
        variation2Dto.setRestaurantId("4758");
        // variation2Dto.setVariation_id("5657");

    }

    // Test to get all

    @Test
    public void testForGetAll_SUCCESS() {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("name_eq", "4 Pieces");
        Query mockQuery = mock(Query.class);

        // MockedStatic method that you can use to create a mock object for a static method.
        try (MockedStatic<QueryUtils> mockedStatic = Mockito.mockStatic(QueryUtils.class)) {
            mockedStatic
                    .when(() -> QueryUtils.getFilterQuery(
                            queryParams, QueryUtils.getAllowedParameters(Item.class.getSimpleName())))
                    .thenReturn(mockQuery);
            ResponseEntity<Response> responseEntity = variationController.getAll(queryParams);
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
                responseEntity = variationController.getAll(queryParams);
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
            // when(variationService.findById(id)).thenReturn(variation1);
            // when(variationTranslation.getDto(variation1)).thenReturn(variation1Dto);

            when(variationService.findByIdWithReference(id, Variation.class)).thenReturn(variation1);
            when(variationTranslation.getDto(variation1)).thenReturn(variation1Dto);

            // act
            ResponseEntity<Response> responseEntity = variationController.getById(id);

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
        when(variationService.findByIdWithReference(id, Variation.class))
                .thenThrow(new RuntimeException("Server error"));
        ResponseEntity<Response> responseEntity = variationController.getById(id);

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
        when(variationService.findByIdWithReference(id, Variation.class)).thenReturn(null);

        // act
        ResponseEntity<Response> responseEntity = variationController.getById(id);

        // aassert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // UTC for find By Id Server error
    @Test
    void testFindById_InternalServerError() {
        // Arrange
        String id = "111ff";
        when(variationService.findByIdWithReference(id, Variation.class))
                .thenThrow(new RuntimeException("Server error"));

        // act
        ResponseEntity<Response> responseEntity = variationController.getById(id);

        // Assert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody().isError()).isTrue();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
        assertThat(responseEntity.getBody().getData()).isNull();
    }
}
