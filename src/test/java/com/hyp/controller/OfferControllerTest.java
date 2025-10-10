package com.hyp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hyp.dto.OfferDto;
import com.hyp.entity.Item;
import com.hyp.entity.Offer;
import com.hyp.response.Response;
import com.hyp.service.OfferService;
import com.hyp.translation.OfferTranslation;
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
public class OfferControllerTest {

    @Mock
    private OfferService offerService;

    @Mock
    private OfferTranslation offerTranslation;

    @InjectMocks
    private BaseController<OfferDto, Offer, String> offerController = new BaseController<OfferDto, Offer, String>() {};

    private Offer offer1;
    private Offer offer2;

    private OfferDto offer1Dto;
    private OfferDto offer2Dto;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        offer1 = new Offer();
        offer1.setId("1");
        offer1.setTitle("Special Discount");
        offer1.setDescription("Get 20% off on your next order!");
        offer1.setCouponCode("DISCOUNT_PRO");
        offer1.setDiscountType("percentage");
        offer1.setMaxDiscount(50);

        // Offer 1 DTO
        offer1Dto = new OfferDto();
        offer1Dto.setId("1");
        offer1Dto.setTitle("Special Discount");
        offer1Dto.setDescription("Get 20% off on your next order!");
        offer1Dto.setCouponCode("DISCOUNT_PRO");
        // offer1Dto.setDiscountType("percentage");
        offer1Dto.setMaxDiscount(50);

        offer2 = new Offer();
        offer2.setId("2");
        offer2.setTitle("Buy One Get One Free");
        offer2.setDescription("Enjoy a buy-one-get-one-free offer on select Offers!");
        offer2.setCouponCode("BOGOFREE");
        offer2.setDiscountType("percentage");
        offer2.setMaxDiscount(100);

        // offer2 Dto
        offer2Dto = new OfferDto();
        offer2Dto.setId("2");
        offer2Dto.setTitle("Buy One Get One Free");
        offer2Dto.setDescription("Enjoy a buy-one-get-one-free offer on select Offers!");
        offer2Dto.setCouponCode("BOGOFREE");
        // offer2Dto.setDiscountType("percentage");
        offer2Dto.setMaxDiscount(100);
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
            ResponseEntity<Response> responseEntity = offerController.getAll(queryParams);
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
                responseEntity = offerController.getAll(queryParams);
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
            when(offerService.findById(id)).thenReturn(offer2);
            when(offerTranslation.getDto(offer2)).thenReturn(offer2Dto);

            // act
            ResponseEntity<Response> responseEntity = offerController.getById(id);

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
        when(offerService.findById(id)).thenThrow(new RuntimeException("Server error"));
        ResponseEntity<Response> responseEntity = offerController.getById(id);

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
        when(offerService.findById(id)).thenReturn(null);

        // act
        ResponseEntity<Response> responseEntity = offerController.getById(id);

        // aassert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // UTC for find By Id Server error
    @Test
    void testFindById_InternalServerError() {
        // Arrange
        String id = "111ff";
        when(offerService.findById(id)).thenThrow(new RuntimeException("Server error"));

        // act
        ResponseEntity<Response> responseEntity = offerController.getById(id);

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
        when(offerTranslation.getEntity(offer1Dto)).thenReturn(offer1);
        when(offerService.save(offer1)).thenReturn(offer1);
        when(offerTranslation.getDto(offer1)).thenReturn(offer1Dto);

        // act
        ResponseEntity<Response> responseEntity = offerController.create(offer1Dto);

        // assert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(offer1Dto));
        assertThat(responseEntity.getBody().isError()).isFalse();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
    }

    // UTC for create on server error

    @Test
    public void testForCreate_InternalServerError() {

        when(offerTranslation.getEntity(offer1Dto)).thenReturn(offer1);
        when(offerService.save(offer1)).thenThrow(new RuntimeException("Server error"));

        ResponseEntity<Response> responseEntity = offerController.create(offer1Dto);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().isError()).isTrue();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
    }

    // UTC for update success
    @Test
    public void testForUpdate() {
        // Arrange
        when(offerService.findById("123455")).thenReturn(offer1);
        doNothing().when(offerTranslation).updateEntityFromDto(offer1Dto, offer1);
        when(offerService.save(offer1)).thenReturn(offer1);
        when(offerTranslation.getDto(offer1)).thenReturn(offer1Dto);

        // act
        ResponseEntity<Response> responseEntity = offerController.update("123455", offer1Dto);

        // assert
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(offer1Dto));
        assertThat(responseEntity.getBody().isError()).isFalse();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
    }

    // UTC for update not found
    @Test
    void testForUpdate_NotFound() {
        when(offerService.findById("123455")).thenReturn(null);

        ResponseEntity<Response> response = offerController.update("123455", offer1Dto);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    // Test update - server error
    @Test
    void testForUpdate_InternalServerError() throws Exception {
        when(offerService.findById("123455")).thenThrow(new RuntimeException("Server error"));

        ResponseEntity<Response> responseEntity = offerController.update("123455", offer1Dto);

        assertNotNull(responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().isError()).isTrue();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
    }

    // UTC for delete - success
    @Test
    public void testForDelete_SUCCESS() {
        when(offerService.findById("1")).thenReturn(offer1);
        doNothing().when(offerService).deleteById("1");

        ResponseEntity<Response> responseEntity = offerController.delete("1");
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNull();
    }

    // UTC for delete - not found case
    @Test
    public void testForDelete_NotFound() {
        when(offerService.findById("123455")).thenReturn(null);

        ResponseEntity<Response> responseEntity = offerController.delete("123455");

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNull();
    }

    // Test for Server error while deleting
    @Test
    public void testForDelete_InternalServerError() {
        when(offerService.findById("123455")).thenThrow(new RuntimeException("Server error"));

        ResponseEntity<Response> responseEntity = offerController.delete("123455");

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

        when(offerService.findById(id)).thenThrow(new RuntimeException("Server error"));

        ResponseEntity<Response> responseEntity = offerController.delete(id);

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseEntity.getBody()).isNotNull();
    }
}
