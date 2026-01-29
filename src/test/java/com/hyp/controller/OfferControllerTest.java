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
import com.hyp.enums.OfferType;
import com.hyp.response.Response;
import com.hyp.service.OfferService;
import com.hyp.translation.OfferTranslation;
import com.hyp.util.QueryUtils;
import java.time.LocalDateTime;
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
        offer1.setOfferCode("OFFER1");
        offer1.setOfferType(OfferType.PERCENTAGE);
        offer1.setDiscountValue(20.0);
        offer1.setStartDate(LocalDateTime.now());
        offer1.setEndDate(LocalDateTime.now().plusDays(30));
        offer1.setMaximumRedemptionLimit("3");
        offer1.setIsActive(true);
        offer1.setPartnerId("partner-123");
        offer1.setNotes("Get 20% off on your next order!");

        offer1Dto = new OfferDto();
        offer1Dto.setOfferCode(offer1.getOfferCode());
        offer1Dto.setOfferType(offer1.getOfferType());
        offer1Dto.setDiscountValue(offer1.getDiscountValue());
        offer1Dto.setStartDate(offer1.getStartDate());
        offer1Dto.setEndDate(offer1.getEndDate());
        offer1Dto.setMaximumRedemptionLimit(offer1.getMaximumRedemptionLimit());
        offer1Dto.setIsActive(offer1.getIsActive());
        offer1Dto.setPartnerId(offer1.getPartnerId());
        offer1Dto.setNotes(offer1.getNotes());

        offer2 = new Offer();
        offer2.setOfferCode("OFFER2");
        offer2.setOfferType(OfferType.FLAT);
        offer2.setDiscountValue(100.0);
        offer2.setStartDate(LocalDateTime.now().minusDays(5));
        offer2.setEndDate(LocalDateTime.now().plusDays(10));
        offer2.setMaximumRedemptionLimit("3");
        offer2.setIsActive(true);
        offer2.setPartnerId("partner-456");
        offer2.setNotes("Buy one get one free on select items");

        offer2Dto = new OfferDto();
        offer2Dto.setOfferCode(offer2.getOfferCode());
        offer2Dto.setOfferType(offer2.getOfferType());
        offer2Dto.setDiscountValue(offer2.getDiscountValue());
        offer2Dto.setStartDate(offer2.getStartDate());
        offer2Dto.setEndDate(offer2.getEndDate());
        offer2Dto.setMaximumRedemptionLimit(offer2.getMaximumRedemptionLimit());
        offer2Dto.setIsActive(offer2.getIsActive());
        offer2Dto.setPartnerId(offer2.getPartnerId());
        offer2Dto.setNotes(offer2.getNotes());
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
