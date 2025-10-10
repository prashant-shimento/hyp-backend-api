package com.hyp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyp.entity.OrderType;
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
public class OrderTypeServiceTest {

    @Mock
    private MongoRepository<OrderType, String> orderTypeRepository;

    @InjectMocks
    private BaseServiceImpl<OrderType, String> orderTypeService = new BaseServiceImpl<>() {};

    @Mock
    private MongoTemplate mongoTemplate;

    private OrderType orderType1;
    private OrderType orderType2;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        orderType1 = new OrderType();
        orderType1.setId("1");
        orderType1.setOrderType("Delivery");
        orderType1.setRestaurantId("4758");

        orderType2 = new OrderType();
        orderType2.setId("1");
        orderType2.setOrderType("Delivery");
        orderType2.setRestaurantId("4758");
    }

    // UTC for find By Id
    @Test
    public void testForFindById_SUCCESS() {
        when(orderTypeRepository.findById("1")).thenReturn(Optional.of(orderType1));

        OrderType savedOrderType = orderTypeService.findById("1");

        assertThat(savedOrderType).isNotNull();
        assertThat(orderType1.getId()).isEqualTo("1");
        verify(orderTypeRepository, times(1)).findById("1");
    }

    // UTC for find By Id when Id is null
    @Test
    public void testForFindById_InternalServerErrror() {

        String id = null;
        when(orderTypeRepository.findById(id)).thenThrow(new IllegalArgumentException("Id can't be null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> orderTypeService.findById(id));

        assertThat(exception).hasMessage("Id can't be null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for find By id on Not found
    @Test
    public void testForFindById_NotFound() {
        String id = "viv123";
        when(orderTypeRepository.findById(id))
                .thenThrow(new EntityNotFoundException("Entity not found with ID: " + id));
        EntityNotFoundException exception =
                assertThrows(EntityNotFoundException.class, () -> orderTypeService.findById(id));

        assertThat(exception).isInstanceOf(EntityNotFoundException.class);
        assertThat(exception).hasMessage("Entity not found with ID: " + id);
    }

    // UTC for findAll
    @Test
    public void testForFindAll_SUCCESS() {
        List<OrderType> entities = Arrays.asList(orderType1, orderType2);

        when(orderTypeRepository.findAll()).thenReturn(entities);

        List<OrderType> result = orderTypeService.findAll();

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(entities.size());
        assertThat(result).isEqualTo(entities);
        assertThat(result.get(0)).isEqualTo(orderType1);
        verify(orderTypeRepository, times(1)).findAll();
    }

    // UTC for findAll - Exception
    @Test
    public void testForFindAll_Exception() {

        when(orderTypeRepository.findAll()).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderTypeService.findAll());

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).hasMessage("Server error");
        verify(orderTypeRepository, times(1)).findAll();
    }

    // Vivek -> Copying all
    // UTC for save success
    @Test
    public void testForSave_SUCCESS() {
        OrderType entity = orderType1;
        OrderType mockeddEntity = orderType1;
        when(orderTypeRepository.save(entity)).thenReturn(mockeddEntity);

        OrderType response = orderTypeService.save(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(entity.getId());
        assertThat(response).isEqualTo(mockeddEntity);
        verify(orderTypeRepository, times(1)).save(entity);
    }

    // UTC for save - Illegal Arguments
    @Test
    public void testForSave_IllegalArguments() {
        OrderType entity = null;
        when(orderTypeRepository.save(entity)).thenThrow(new IllegalArgumentException("Entity is null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> orderTypeService.save(entity));

        assertThat(exception).hasMessage("Entity is null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for save run time exception
    @Test
    void testSave_RepositoryThrowsException() {
        OrderType validEntity = orderType1;
        when(orderTypeRepository.save(validEntity)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderTypeService.save(validEntity));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(orderTypeRepository, times(1)).save(validEntity);
    }

    // UTC for saveALl - success
    @Test
    public void testForSaveAll_SUCCESS() {
        List<OrderType> entities = Arrays.asList(orderType1, orderType2);
        List<OrderType> mockedEntities = Arrays.asList(orderType1, orderType2);

        when(orderTypeRepository.saveAll(entities)).thenReturn(mockedEntities);

        List<OrderType> response = orderTypeService.saveAll(entities);

        assertThat(response).isNotNull();
        assertThat(response.get(0).getId()).isEqualTo(entities.get(0).getId());
        assertThat(response).isEqualTo(entities);

        verify(orderTypeRepository, times(1)).saveAll(entities);
    }

    // UTC for save All - Illegal Arguments
    @Test
    public void testForSaveAll_IllegalArguments() {
        List<OrderType> entities = null;
        when(orderTypeRepository.saveAll(entities)).thenThrow(new IllegalArgumentException("Entities are null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> orderTypeService.saveAll(entities));

        assertThat(exception).hasMessage("Entities are null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for save All run time exception
    @Test
    void testSaveAll_RepositoryThrowsException() {
        List<OrderType> entities = Arrays.asList(orderType1, orderType2);
        when(orderTypeRepository.saveAll(entities)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderTypeService.saveAll(entities));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(orderTypeRepository, times(1)).saveAll(entities);
    }

    // Test for update - success
    @Test
    void testUpdate_SUCCESS() {
        OrderType entity = orderType1;
        OrderType mockeddEntity = orderType1;
        when(orderTypeRepository.save(entity)).thenReturn(mockeddEntity);

        OrderType response = orderTypeService.update(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(entity.getId());
        assertThat(response).isEqualTo(mockeddEntity);
        verify(orderTypeRepository, times(1)).save(entity);
    }

    // UTC for update - Illegal Arguments
    @Test
    public void testForUpdate_IllegalArguments() {
        OrderType entity = null;
        when(orderTypeRepository.save(entity)).thenThrow(new IllegalArgumentException("Entity is null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> orderTypeService.update(entity));

        assertThat(exception).hasMessage("Entity is null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for update -  run time exception
    @Test
    void testForUpdate_RepositoryThrowsException() {
        OrderType validEntity = orderType1;
        when(orderTypeRepository.save(validEntity)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderTypeService.update(validEntity));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(orderTypeRepository, times(1)).save(validEntity);
    }

    // UTC for deleteById - success
    @Test
    public void testForDeleteById_SUCCESS() {
        String id = orderType1.getId();

        //	    	when(orderTypeRepository.existsById(id)).thenReturn(true);

        orderTypeService.deleteById(id);

        verify(orderTypeRepository, times(1)).deleteById(id);
    }

    // UTC for is Exists By Id
    @Test
    public void testForIsExistsById_SUCCESS() {
        // arrange.
        String id = "123455";
        boolean exists = true;
        when(orderTypeRepository.existsById(id)).thenReturn(exists);

        // act
        boolean result = orderTypeService.isExistsById(id);

        // assert
        assertEquals(exists, result);
        verify(orderTypeRepository, times(1)).existsById(id);
    }

    // UTC for exists by id , when id is null
    @Test
    public void testForIsExistsById_IdIsNull() {
        String id = null;
        when(orderTypeRepository.existsById(id)).thenThrow(new IllegalArgumentException("Id cannot be null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> orderTypeService.isExistsById(id));

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
        assertThat(exception).hasMessage("Id cannot be null");
    }

    // UTC for exists by id , Rum time Exception
    @Test
    public void testForIsExistsById_Exception() {
        String id = "faf56";
        when(orderTypeRepository.existsById(id)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderTypeService.isExistsById(id));

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).hasMessage("Server error");

        verify(orderTypeRepository, times(1)).existsById(id);
    }

    // UTC for Find By Field -success
    @Test
    public void testForFindByField_SUCCESS() {
        // arrange
        Class<OrderType> entityClass = OrderType.class;
        String fieldName = "id";
        String value = "faf56";

        OrderType mockEntity = orderType1;

        Query expectedQuery = new Query(Criteria.where(fieldName).is(value));
        when(mongoTemplate.findOne(expectedQuery, entityClass)).thenReturn(mockEntity);

        // act
        OrderType result = orderTypeService.findByField(entityClass, fieldName, value);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockEntity);
        verify(mongoTemplate, times(1)).findOne(expectedQuery, entityClass);
    }

    // UTC for find By Restaurant
    @Test
    public void testFindByRestaurant() {
        // Arrange
        Class<OrderType> entityClass = OrderType.class;
        String value = "4578";
        Query expectedQuery = new Query(Criteria.where("restaurantId").is(value));
        List<OrderType> mockResult = Arrays.asList(orderType1, orderType2);
        when(mongoTemplate.find(expectedQuery, entityClass)).thenReturn(mockResult);

        // act
        List<OrderType> result = orderTypeService.findByRestaurant(entityClass, value);

        // assert
        assertEquals(mockResult, result);
        verify(mongoTemplate, times(1)).find(expectedQuery, entityClass);
    }

    // UTC for find By Query
    @Test
    public void testFindByQuery() {
        Class<OrderType> entityClass = OrderType.class;
        Query query = new Query(Criteria.where("fieldName").is("value"));

        List<OrderType> mockResult = Arrays.asList(orderType1, orderType2);

        when(mongoTemplate.find(query, entityClass)).thenReturn(mockResult);

        // ACT
        List<OrderType> result = orderTypeService.findByQuery(entityClass, query);

        // assert
        assertEquals(mockResult, result);
        verify(mongoTemplate, times(1)).find(query, entityClass);
    }
}
