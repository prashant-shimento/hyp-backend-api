package com.hyp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyp.entity.Order;
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
public class OrderServiceTest {

    @Mock
    private MongoRepository<Order, String> orderRepository;

    @InjectMocks
    private BaseServiceImpl<Order, String> orderService = new BaseServiceImpl<>() {};

    @Mock
    private MongoTemplate mongoTemplate;

    private Order order1;
    private Order order2;
    private Order order3;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        order1 = new Order();
        order1.setId("1");
        order1.setCustomerId("aass5656");
        order1.setOrderType("delivery");
        order1.setDiscountAmount(10);
        order1.setDeliveryTrackingLink("http://okok.com/7878");
        order1.setGrandTotalAmount(122.90);

        order2 = new Order();
        order1.setId("2");
        order1.setCustomerId("acass5126");
        order1.setOrderType("pickup");
        order1.setDiscountAmount(100);
        order1.setDeliveryTrackingLink("http://okok.com/78122278");
        order1.setGrandTotalAmount(134.00);

        order3 = new Order();
        order1.setId("3");
        order1.setCustomerId("viv126");
        order1.setOrderType("delivery");
        order1.setDiscountAmount(50);
        order1.setDeliveryTrackingLink("http://okok.com/1dd8122278");
        order1.setGrandTotalAmount(89.00);
    }

    // UTC for find By Id
    @Test
    public void testForFindById_SUCCESS() {

        String id = order1.getId();
        when(orderRepository.findById(id)).thenReturn(Optional.of(order1));

        Order savedOrder = orderService.findById(id);

        assertThat(savedOrder).isNotNull();
        assertThat(savedOrder.getId()).isEqualTo(id);
        verify(orderRepository, times(1)).findById(id);
    }

    // UTC for find By Id when Id is null
    @Test
    public void testForFindById_InternalServerErrror() {

        String id = null;
        when(orderRepository.findById(id)).thenThrow(new IllegalArgumentException("Id can't be null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> orderService.findById(id));

        assertThat(exception).hasMessage("Id can't be null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for find By id on Not found
    @Test
    public void testForFindById_NotFound() {
        String id = "viv123";
        when(orderRepository.findById(id)).thenThrow(new EntityNotFoundException("Entity not found with ID: " + id));
        EntityNotFoundException exception =
                assertThrows(EntityNotFoundException.class, () -> orderService.findById(id));

        assertThat(exception).isInstanceOf(EntityNotFoundException.class);
        assertThat(exception).hasMessage("Entity not found with ID: " + id);
    }

    // UTC for findAll
    @Test
    public void testForFindAll_SUCCESS() {
        List<Order> entities = Arrays.asList(order1, order2, order3);

        when(orderRepository.findAll()).thenReturn(entities);

        List<Order> result = orderService.findAll();

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(entities.size());
        assertThat(result).isEqualTo(entities);
        assertThat(result.get(0)).isEqualTo(order1);
        verify(orderRepository, times(1)).findAll();
    }

    // UTC for findAll - Exception
    @Test
    public void testForFindAll_Exception() {

        when(orderRepository.findAll()).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderService.findAll());

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).hasMessage("Server error");
        verify(orderRepository, times(1)).findAll();
    }

    // UTC for save success
    @Test
    public void testForSave_SUCCESS() {
        Order entity = order1;
        Order mockeddEntity = order1;
        when(orderRepository.save(entity)).thenReturn(mockeddEntity);

        Order response = orderService.save(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(entity.getId());
        assertThat(response).isEqualTo(mockeddEntity);
        verify(orderRepository, times(1)).save(entity);
    }

    // UTC for save - Illegal Arguments
    @Test
    public void testForSave_IllegalArguments() {
        Order entity = null;
        when(orderRepository.save(entity)).thenThrow(new IllegalArgumentException("Entity is null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> orderService.save(entity));

        assertThat(exception).hasMessage("Entity is null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for save run time exception
    @Test
    void testSave_RepositoryThrowsException() {
        Order validEntity = order1;
        when(orderRepository.save(validEntity)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderService.save(validEntity));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(orderRepository, times(1)).save(validEntity);
    }

    // UTC for saveALl - success
    @Test
    public void testForSaveAll_SUCCESS() {
        List<Order> entities = Arrays.asList(order1, order2, order3);
        List<Order> mockedEntities = Arrays.asList(order1, order2, order3);

        when(orderRepository.saveAll(entities)).thenReturn(mockedEntities);

        List<Order> response = orderService.saveAll(entities);

        assertThat(response).isNotNull();
        assertThat(response.get(0).getId()).isEqualTo(entities.get(0).getId());
        assertThat(response).isEqualTo(entities);

        verify(orderRepository, times(1)).saveAll(entities);
    }

    // UTC for save All - Illegal Arguments
    @Test
    public void testForSaveAll_IllegalArguments() {
        List<Order> entities = null;
        when(orderRepository.saveAll(entities)).thenThrow(new IllegalArgumentException("Entities are null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> orderService.saveAll(entities));

        assertThat(exception).hasMessage("Entities are null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for save All run time exception
    @Test
    void testSaveAll_RepositoryThrowsException() {
        List<Order> entities = Arrays.asList(order1, order2, order3);
        when(orderRepository.saveAll(entities)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderService.saveAll(entities));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(orderRepository, times(1)).saveAll(entities);
    }

    // Test for update - success
    @Test
    void testUpdate_SUCCESS() {
        Order entity = order1;
        Order mockeddEntity = order1;
        when(orderRepository.save(entity)).thenReturn(mockeddEntity);

        Order response = orderService.update(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(entity.getId());
        assertThat(response).isEqualTo(mockeddEntity);
        verify(orderRepository, times(1)).save(entity);
    }

    // UTC for update - Illegal Arguments
    @Test
    public void testForUpdate_IllegalArguments() {
        Order entity = null;
        when(orderRepository.save(entity)).thenThrow(new IllegalArgumentException("Entity is null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> orderService.update(entity));

        assertThat(exception).hasMessage("Entity is null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for update -  run time exception
    @Test
    void testForUpdate_RepositoryThrowsException() {
        Order validEntity = order1;
        when(orderRepository.save(validEntity)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderService.update(validEntity));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(orderRepository, times(1)).save(validEntity);
    }

    // UTC for deleteById - success
    @Test
    public void testForDeleteById_SUCCESS() {
        String id = order1.getId();

        //    	when(orderRepository.existsById(id)).thenReturn(true);

        orderService.deleteById(id);

        verify(orderRepository, times(1)).deleteById(id);
    }

    // UTC for is Exists By Id
    @Test
    public void testForIsExistsById_SUCCESS() {
        // arrange.
        String id = "123455";
        boolean exists = true;
        when(orderRepository.existsById(id)).thenReturn(exists);

        // act
        boolean result = orderService.isExistsById(id);

        // assert
        assertEquals(exists, result);
        verify(orderRepository, times(1)).existsById(id);
    }

    // UTC for exists by id , when id is null
    @Test
    public void testForIsExistsById_IdIsNull() {
        String id = null;
        when(orderRepository.existsById(id)).thenThrow(new IllegalArgumentException("Id cannot be null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> orderService.isExistsById(id));

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
        assertThat(exception).hasMessage("Id cannot be null");
    }

    // UTC for exists by id , Rum time Exception
    @Test
    public void testForIsExistsById_Exception() {
        String id = "faf56";
        when(orderRepository.existsById(id)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> orderService.isExistsById(id));

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).hasMessage("Server error");

        verify(orderRepository, times(1)).existsById(id);
    }

    // UTC for Find By Field -success
    @Test
    public void testForFindByField_SUCCESS() {
        // arrange
        Class<Order> entityClass = Order.class;
        String fieldName = "id";
        String value = "faf56";

        Order mockEntity = order1;

        Query expectedQuery = new Query(Criteria.where(fieldName).is(value));
        when(mongoTemplate.findOne(expectedQuery, entityClass)).thenReturn(mockEntity);

        // act
        Order result = orderService.findByField(entityClass, fieldName, value);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockEntity);
        verify(mongoTemplate, times(1)).findOne(expectedQuery, entityClass);
    }

    // UTC for find By Restaurant
    @Test
    public void testFindByRestaurant() {
        // Arrange
        Class<Order> entityClass = Order.class;
        String value = "4578";
        Query expectedQuery = new Query(Criteria.where("restaurantId").is(value));
        List<Order> mockResult = Arrays.asList(order1, order2);
        when(mongoTemplate.find(expectedQuery, entityClass)).thenReturn(mockResult);

        // act
        List<Order> result = orderService.findByRestaurant(entityClass, value);

        // asserrt
        assertEquals(mockResult, result);
        verify(mongoTemplate, times(1)).find(expectedQuery, entityClass);
    }

    // UTC for find By Query
    @Test
    public void testFindByQuery() {
        Class<Order> entityClass = Order.class;
        Query query = new Query(Criteria.where("fieldName").is("value"));

        List<Order> mockResult = Arrays.asList(order1, order2);

        when(mongoTemplate.find(query, entityClass)).thenReturn(mockResult);

        // ACT
        List<Order> result = orderService.findByQuery(entityClass, query);

        // assert
        assertEquals(mockResult, result);
        verify(mongoTemplate, times(1)).find(query, entityClass);
    }
}
