package com.hyp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyp.entity.Tax;
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
public class TaxServiceTest {

    @Mock
    private MongoRepository<Tax, String> taxRepository;

    @InjectMocks
    private BaseServiceImpl<Tax, String> taxService = new BaseServiceImpl<>() {};

    @Mock
    private MongoTemplate mongoTemplate;

    private Tax tax1;
    private Tax tax2;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        tax1 = new Tax();
        tax1.setId("1");
        tax1.setTaxName("CGST");
        tax1.setTax("2.7");
        tax1.setRestaurantId("4758");

        tax2 = new Tax();
        tax2.setId("1");
        tax2.setTaxName("CGST");
        tax2.setTax("2.7");
        tax2.setRestaurantId("4758");
    }

    // UTC for find By Id
    @Test
    public void testForFindById_SUCCESS() {
        when(taxRepository.findById("1")).thenReturn(Optional.of(tax1));

        Tax savedTax = taxService.findById("1");

        assertThat(savedTax).isNotNull();
        assertThat(tax1.getId()).isEqualTo("1");
        verify(taxRepository, times(1)).findById("1");
    }

    // UTC for find By Id when Id is null
    @Test
    public void testForFindById_InternalServerErrror() {

        String id = null;
        when(taxRepository.findById(id)).thenThrow(new IllegalArgumentException("Id can't be null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> taxService.findById(id));

        assertThat(exception).hasMessage("Id can't be null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for find By id on Not found
    @Test
    public void testForFindById_NotFound() {
        String id = "viv123";
        when(taxRepository.findById(id)).thenThrow(new EntityNotFoundException("Entity not found with ID: " + id));
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> taxService.findById(id));

        assertThat(exception).isInstanceOf(EntityNotFoundException.class);
        assertThat(exception).hasMessage("Entity not found with ID: " + id);
    }

    // UTC for findAll
    @Test
    public void testForFindAll_SUCCESS() {
        List<Tax> entities = Arrays.asList(tax1, tax2);

        when(taxRepository.findAll()).thenReturn(entities);

        List<Tax> result = taxService.findAll();

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(entities.size());
        assertThat(result).isEqualTo(entities);
        assertThat(result.get(0)).isEqualTo(tax1);
        verify(taxRepository, times(1)).findAll();
    }

    // UTC for findAll - Exception
    @Test
    public void testForFindAll_Exception() {

        when(taxRepository.findAll()).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> taxService.findAll());

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).hasMessage("Server error");
        verify(taxRepository, times(1)).findAll();
    }

    // Vivek -> Copying all
    // UTC for save success
    @Test
    public void testForSave_SUCCESS() {
        Tax entity = tax1;
        Tax mockeddEntity = tax1;
        when(taxRepository.save(entity)).thenReturn(mockeddEntity);

        Tax response = taxService.save(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(entity.getId());
        assertThat(response).isEqualTo(mockeddEntity);
        verify(taxRepository, times(1)).save(entity);
    }

    // UTC for save - Illegal Arguments
    @Test
    public void testForSave_IllegalArguments() {
        Tax entity = null;
        when(taxRepository.save(entity)).thenThrow(new IllegalArgumentException("Entity is null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> taxService.save(entity));

        assertThat(exception).hasMessage("Entity is null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for save run time exception
    @Test
    void testSave_RepositoryThrowsException() {
        Tax validEntity = tax1;
        when(taxRepository.save(validEntity)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> taxService.save(validEntity));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(taxRepository, times(1)).save(validEntity);
    }

    // UTC for saveALl - success
    @Test
    public void testForSaveAll_SUCCESS() {
        List<Tax> entities = Arrays.asList(tax1, tax2);
        List<Tax> mockedEntities = Arrays.asList(tax1, tax2);

        when(taxRepository.saveAll(entities)).thenReturn(mockedEntities);

        List<Tax> response = taxService.saveAll(entities);

        assertThat(response).isNotNull();
        assertThat(response.get(0).getId()).isEqualTo(entities.get(0).getId());
        assertThat(response).isEqualTo(entities);

        verify(taxRepository, times(1)).saveAll(entities);
    }

    // UTC for save All - Illegal Arguments
    @Test
    public void testForSaveAll_IllegalArguments() {
        List<Tax> entities = null;
        when(taxRepository.saveAll(entities)).thenThrow(new IllegalArgumentException("Entities are null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> taxService.saveAll(entities));

        assertThat(exception).hasMessage("Entities are null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for save All run time exception
    @Test
    void testSaveAll_RepositoryThrowsException() {
        List<Tax> entities = Arrays.asList(tax1, tax2);
        when(taxRepository.saveAll(entities)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> taxService.saveAll(entities));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(taxRepository, times(1)).saveAll(entities);
    }

    // Test for update - success
    @Test
    void testUpdate_SUCCESS() {
        Tax entity = tax1;
        Tax mockeddEntity = tax1;
        when(taxRepository.save(entity)).thenReturn(mockeddEntity);

        Tax response = taxService.update(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(entity.getId());
        assertThat(response).isEqualTo(mockeddEntity);
        verify(taxRepository, times(1)).save(entity);
    }

    // UTC for update - Illegal Arguments
    @Test
    public void testForUpdate_IllegalArguments() {
        Tax entity = null;
        when(taxRepository.save(entity)).thenThrow(new IllegalArgumentException("Entity is null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> taxService.update(entity));

        assertThat(exception).hasMessage("Entity is null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for update -  run time exception
    @Test
    void testForUpdate_RepositoryThrowsException() {
        Tax validEntity = tax1;
        when(taxRepository.save(validEntity)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> taxService.update(validEntity));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(taxRepository, times(1)).save(validEntity);
    }

    // UTC for deleteById - success
    @Test
    public void testForDeleteById_SUCCESS() {
        String id = tax1.getId();

        //	    	when(taxRepository.existsById(id)).thenReturn(true);

        taxService.deleteById(id);

        verify(taxRepository, times(1)).deleteById(id);
    }

    // UTC for is Exists By Id
    @Test
    public void testForIsExistsById_SUCCESS() {
        // arrange.
        String id = "123455";
        boolean exists = true;
        when(taxRepository.existsById(id)).thenReturn(exists);

        // act
        boolean result = taxService.isExistsById(id);

        // assert
        assertEquals(exists, result);
        verify(taxRepository, times(1)).existsById(id);
    }

    // UTC for exists by id , when id is null
    @Test
    public void testForIsExistsById_IdIsNull() {
        String id = null;
        when(taxRepository.existsById(id)).thenThrow(new IllegalArgumentException("Id cannot be null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> taxService.isExistsById(id));

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
        assertThat(exception).hasMessage("Id cannot be null");
    }

    // UTC for exists by id , Rum time Exception
    @Test
    public void testForIsExistsById_Exception() {
        String id = "faf56";
        when(taxRepository.existsById(id)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> taxService.isExistsById(id));

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).hasMessage("Server error");

        verify(taxRepository, times(1)).existsById(id);
    }

    // UTC for Find By Field -success
    @Test
    public void testForFindByField_SUCCESS() {
        // arrange
        Class<Tax> entityClass = Tax.class;
        String fieldName = "id";
        String value = "faf56";

        Tax mockEntity = tax1;

        Query expectedQuery = new Query(Criteria.where(fieldName).is(value));
        when(mongoTemplate.findOne(expectedQuery, entityClass)).thenReturn(mockEntity);

        // act
        Tax result = taxService.findByField(entityClass, fieldName, value);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockEntity);
        verify(mongoTemplate, times(1)).findOne(expectedQuery, entityClass);
    }

    // UTC for find By Restaurant
    @Test
    public void testFindByRestaurant() {
        // Arrange
        Class<Tax> entityClass = Tax.class;
        String value = "4578";
        Query expectedQuery = new Query(Criteria.where("restaurantId").is(value));
        List<Tax> mockResult = Arrays.asList(tax1, tax2);
        when(mongoTemplate.find(expectedQuery, entityClass)).thenReturn(mockResult);

        // act
        List<Tax> result = taxService.findByRestaurant(entityClass, value);

        // asserrt
        assertEquals(mockResult, result);
        verify(mongoTemplate, times(1)).find(expectedQuery, entityClass);
    }

    // UTC for find By Query
    @Test
    public void testFindByQuery() {
        Class<Tax> entityClass = Tax.class;
        Query query = new Query(Criteria.where("fieldName").is("value"));

        List<Tax> mockResult = Arrays.asList(tax1, tax2);

        when(mongoTemplate.find(query, entityClass)).thenReturn(mockResult);

        // ACT
        List<Tax> result = taxService.findByQuery(entityClass, query);

        // assert
        assertEquals(mockResult, result);
        verify(mongoTemplate, times(1)).find(query, entityClass);
    }
}
