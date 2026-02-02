package com.hyp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyp.entity.Offer;
import com.hyp.enums.OfferType;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;

@ExtendWith(MockitoExtension.class)
public class OfferServiceTest {

    @Mock
    private MongoRepository<Offer, String> offerRepository;

    @InjectMocks
    private BaseServiceImpl<Offer, String> offerService = new BaseServiceImpl<>() {};

    @Mock
    private MongoTemplate mongoTemplate;

    private Offer offer1;
    private Offer offer2;

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

        offer2 = new Offer();
        offer2.setOfferCode("OFFER2");
        offer2.setOfferType(OfferType.PERCENTAGE);
        offer2.setDiscountValue(100.0);
        offer2.setStartDate(LocalDateTime.now().minusDays(5));
        offer2.setEndDate(LocalDateTime.now().plusDays(10));
        offer2.setMaximumRedemptionLimit("3");
        offer2.setIsActive(true);
        offer2.setPartnerId("partner-456");
        offer2.setNotes("Buy one get one free on select items");
    }

    @Test
    public void testForFindById_SUCCESS() {
        when(offerRepository.findById("1")).thenReturn(Optional.of(offer1));

        Offer savedOffer = offerService.findById("1");

        assertThat(savedOffer).isNotNull();
        assertThat(offer1.getId()).isEqualTo("1");
        verify(offerRepository, times(1)).findById("1");
    }

    // UTC for find By Id when Id is null
    @Test
    public void testForFindById_InternalServerErrror() {

        String id = null;
        when(offerRepository.findById(id)).thenThrow(new IllegalArgumentException("Id can't be null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> offerService.findById(id));

        assertThat(exception).hasMessage("Id can't be null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for find By id on Not found
    @Test
    public void testForFindById_NotFound() {
        String id = "viv123";
        when(offerRepository.findById(id)).thenThrow(new EntityNotFoundException("Entity not found with ID: " + id));
        EntityNotFoundException exception =
                assertThrows(EntityNotFoundException.class, () -> offerService.findById(id));

        assertThat(exception).isInstanceOf(EntityNotFoundException.class);
        assertThat(exception).hasMessage("Entity not found with ID: " + id);
    }

    // UTC for findAll
    @Test
    public void testForFindAll_SUCCESS() {
        List<Offer> entities = Arrays.asList(offer1, offer2);

        when(offerRepository.findAll()).thenReturn(entities);

        List<Offer> result = offerService.findAll();

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(entities.size());
        assertThat(result).isEqualTo(entities);
        assertThat(result.get(0)).isEqualTo(offer1);
        verify(offerRepository, times(1)).findAll();
    }

    // UTC for findAll - Exception
    @Test
    public void testForFindAll_Exception() {

        when(offerRepository.findAll()).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> offerService.findAll());

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).hasMessage("Server error");
        verify(offerRepository, times(1)).findAll();
    }

    // Vivek -> Copying all
    // UTC for save success
    @Test
    public void testForSave_SUCCESS() {
        Offer entity = offer1;
        Offer mockeddEntity = offer1;
        when(offerRepository.save(entity)).thenReturn(mockeddEntity);

        Offer response = offerService.save(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(entity.getId());
        assertThat(response).isEqualTo(mockeddEntity);
        verify(offerRepository, times(1)).save(entity);
    }

    // UTC for save - Illegal Arguments
    @Test
    public void testForSave_IllegalArguments() {
        Offer entity = null;
        when(offerRepository.save(entity)).thenThrow(new IllegalArgumentException("Entity is null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> offerService.save(entity));

        assertThat(exception).hasMessage("Entity is null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for save run time exception
    @Test
    void testSave_RepositoryThrowsException() {
        Offer validEntity = offer1;
        when(offerRepository.save(validEntity)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> offerService.save(validEntity));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(offerRepository, times(1)).save(validEntity);
    }

    // UTC for saveALl - success
    @Test
    public void testForSaveAll_SUCCESS() {
        List<Offer> entities = Arrays.asList(offer1, offer2);
        List<Offer> mockedEntities = Arrays.asList(offer1, offer2);

        when(offerRepository.saveAll(entities)).thenReturn(mockedEntities);

        List<Offer> response = offerService.saveAll(entities);

        assertThat(response).isNotNull();
        assertThat(response.get(0).getId()).isEqualTo(entities.get(0).getId());
        assertThat(response).isEqualTo(entities);

        verify(offerRepository, times(1)).saveAll(entities);
    }

    // UTC for save All - Illegal Arguments
    @Test
    public void testForSaveAll_IllegalArguments() {
        List<Offer> entities = null;
        when(offerRepository.saveAll(entities)).thenThrow(new IllegalArgumentException("Entities are null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> offerService.saveAll(entities));

        assertThat(exception).hasMessage("Entities are null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for save All run time exception
    @Test
    void testSaveAll_RepositoryThrowsException() {
        List<Offer> entities = Arrays.asList(offer1, offer2);
        when(offerRepository.saveAll(entities)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> offerService.saveAll(entities));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(offerRepository, times(1)).saveAll(entities);
    }

    // Test for update - success
    @Test
    void testUpdate_SUCCESS() {
        Offer entity = offer1;
        Offer mockeddEntity = offer1;
        when(offerRepository.save(entity)).thenReturn(mockeddEntity);

        Offer response = offerService.update(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(entity.getId());
        assertThat(response).isEqualTo(mockeddEntity);
        verify(offerRepository, times(1)).save(entity);
    }

    // UTC for update - Illegal Arguments
    @Test
    public void testForUpdate_IllegalArguments() {
        Offer entity = null;
        when(offerRepository.save(entity)).thenThrow(new IllegalArgumentException("Entity is null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> offerService.update(entity));

        assertThat(exception).hasMessage("Entity is null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for update -  run time exception
    @Test
    void testForUpdate_RepositoryThrowsException() {
        Offer validEntity = offer1;
        when(offerRepository.save(validEntity)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> offerService.update(validEntity));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(offerRepository, times(1)).save(validEntity);
    }

    // UTC for deleteById - success
    @Test
    public void testForDeleteById_SUCCESS() {
        String id = offer1.getId();

        //	    	when(offerRepository.existsById(id)).thenReturn(true);

        offerService.deleteById(id);

        verify(offerRepository, times(1)).deleteById(id);
    }

    // UTC for is Exists By Id
    @Test
    public void testForIsExistsById_SUCCESS() {
        // arrange.
        String id = "123455";
        boolean exists = true;
        when(offerRepository.existsById(id)).thenReturn(exists);

        // act
        boolean result = offerService.isExistsById(id);

        // assert
        assertEquals(exists, result);
        verify(offerRepository, times(1)).existsById(id);
    }

    // UTC for exists by id , when id is null
    @Test
    public void testForIsExistsById_IdIsNull() {
        String id = null;
        when(offerRepository.existsById(id)).thenThrow(new IllegalArgumentException("Id cannot be null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> offerService.isExistsById(id));

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
        assertThat(exception).hasMessage("Id cannot be null");
    }

    // UTC for exists by id , Rum time Exception
    @Test
    public void testForIsExistsById_Exception() {
        String id = "faf56";
        when(offerRepository.existsById(id)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> offerService.isExistsById(id));

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).hasMessage("Server error");

        verify(offerRepository, times(1)).existsById(id);
    }

    // UTC for Find By Field -success
    @Test
    public void testForFindByField_SUCCESS() {
        // arrange
        Class<Offer> entityClass = Offer.class;
        String fieldName = "id";
        String value = "faf56";

        Offer mockEntity = offer1;

        Query expectedQuery = new Query(Criteria.where(fieldName).is(value));
        when(mongoTemplate.findOne(expectedQuery, entityClass)).thenReturn(mockEntity);

        // act
        Offer result = offerService.findByField(entityClass, fieldName, value);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockEntity);
        verify(mongoTemplate, times(1)).findOne(expectedQuery, entityClass);
    }

    // UTC for find By Restaurant
    @Test
    public void testFindByRestaurant() {
        // Arrange
        Class<Offer> entityClass = Offer.class;
        String value = "4578";
        Query expectedQuery = new Query(Criteria.where("restaurantId").is(value));
        List<Offer> mockResult = Arrays.asList(offer1, offer2);
        when(mongoTemplate.find(expectedQuery, entityClass)).thenReturn(mockResult);

        // act
        List<Offer> result = offerService.findByRestaurant(entityClass, value);

        // asserrt
        assertEquals(mockResult, result);
        verify(mongoTemplate, times(1)).find(expectedQuery, entityClass);
    }

    // UTC for find By Query
    @Test
    public void testFindByQuery() {
        Class<Offer> entityClass = Offer.class;
        Query query = new Query(Criteria.where("fieldName").is("value"));

        List<Offer> mockResult = Arrays.asList(offer1, offer2);

        when(mongoTemplate.find(query, entityClass)).thenReturn(mockResult);

        // ACT
        List<Offer> result = offerService.findByQuery(entityClass, query);

        // assert
        assertEquals(mockResult, result);
        verify(mongoTemplate, times(1)).find(query, entityClass);
    }
}
