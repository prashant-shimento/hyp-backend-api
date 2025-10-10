package com.hyp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hyp.entity.Item;
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
public class ItemServiceTest {

    @Mock
    private MongoRepository<Item, String> itemRepository;

    @InjectMocks
    private BaseServiceImpl<Item, String> itemService = new BaseServiceImpl<>() {};

    @Mock
    private MongoTemplate mongoTemplate;

    private Item item1;
    private Item item2;
    private Item item3;

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

        // Item 3
        item3 = new Item();
        item3.setId("faf56");
        item3.setItemDescription("Item 3");
        item3.setItemRank("1");
        item3.setItemAllowAddon("0");
        item3.setInStock(true);
        item3.setPrice("447.00");
        item3.setItemName("Big Baik Spicy Sandwich");
        item3.setActive("1");
        item3.setItemAttributeId("1");
    }

    // UTC for find By Id
    @Test
    public void testForFindById_SUCCESS() {
        when(itemRepository.findById("faf56")).thenReturn(Optional.of(item3));

        Item savedItem = itemService.findById("faf56");

        assertThat(savedItem).isNotNull();
        assertThat(item3.getId()).isEqualTo("faf56");
        verify(itemRepository, times(1)).findById("faf56");
    }

    // UTC for find By Id when Id is null
    @Test
    public void testForFindById_InternalServerErrror() {

        String id = null;
        when(itemRepository.findById(id)).thenThrow(new IllegalArgumentException("Id can't be null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> itemService.findById(id));

        assertThat(exception).hasMessage("Id can't be null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for find By id on Not found
    @Test
    public void testForFindById_NotFound() {
        String id = "viv123";
        when(itemRepository.findById(id)).thenThrow(new EntityNotFoundException("Entity not found with ID: " + id));
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> itemService.findById(id));

        assertThat(exception).isInstanceOf(EntityNotFoundException.class);
        assertThat(exception).hasMessage("Entity not found with ID: " + id);
    }

    // UTC for findAll
    @Test
    public void testForFindAll_SUCCESS() {
        List<Item> entities = Arrays.asList(item1, item2, item3);

        when(itemRepository.findAll()).thenReturn(entities);

        List<Item> result = itemService.findAll();

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(entities.size());
        assertThat(result).isEqualTo(entities);
        assertThat(result.get(0)).isEqualTo(item1);
        verify(itemRepository, times(1)).findAll();
    }

    // UTC for findAll - Exception
    @Test
    public void testForFindAll_Exception() {

        when(itemRepository.findAll()).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> itemService.findAll());

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).hasMessage("Server error");
        verify(itemRepository, times(1)).findAll();
    }

    // UTC for save success
    @Test
    public void testForSave_SUCCESS() {
        Item entity = item1;
        Item mockeddEntity = item1;
        when(itemRepository.save(entity)).thenReturn(mockeddEntity);

        Item response = itemService.save(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(entity.getId());
        assertThat(response).isEqualTo(mockeddEntity);
        verify(itemRepository, times(1)).save(entity);
    }

    // UTC for save - Illegal Arguments
    @Test
    public void testForSave_IllegalArguments() {
        Item entity = null;
        when(itemRepository.save(entity)).thenThrow(new IllegalArgumentException("Entity is null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> itemService.save(entity));

        assertThat(exception).hasMessage("Entity is null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for save run time exception
    @Test
    void testSave_RepositoryThrowsException() {
        Item validEntity = item1;
        when(itemRepository.save(validEntity)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> itemService.save(validEntity));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(itemRepository, times(1)).save(validEntity);
    }

    // UTC for saveALl - success
    @Test
    public void testForSaveAll_SUCCESS() {
        List<Item> entities = Arrays.asList(item1, item2, item3);
        List<Item> mockedEntities = Arrays.asList(item1, item2, item3);

        when(itemRepository.saveAll(entities)).thenReturn(mockedEntities);

        List<Item> response = itemService.saveAll(entities);

        assertThat(response).isNotNull();
        assertThat(response.get(0).getId()).isEqualTo(entities.get(0).getId());
        assertThat(response).isEqualTo(entities);

        verify(itemRepository, times(1)).saveAll(entities);
    }

    // UTC for save All - Illegal Arguments
    @Test
    public void testForSaveAll_IllegalArguments() {
        List<Item> entities = null;
        when(itemRepository.saveAll(entities)).thenThrow(new IllegalArgumentException("Entities are null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> itemService.saveAll(entities));

        assertThat(exception).hasMessage("Entities are null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for save All run time exception
    @Test
    void testSaveAll_RepositoryThrowsException() {
        List<Item> entities = Arrays.asList(item1, item2, item3);
        when(itemRepository.saveAll(entities)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> itemService.saveAll(entities));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(itemRepository, times(1)).saveAll(entities);
    }

    // Test for update - success
    @Test
    void testUpdate_SUCCESS() {
        Item entity = item1;
        Item mockeddEntity = item1;
        when(itemRepository.save(entity)).thenReturn(mockeddEntity);

        Item response = itemService.update(entity);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(entity.getId());
        assertThat(response).isEqualTo(mockeddEntity);
        verify(itemRepository, times(1)).save(entity);
    }

    // UTC for update - Illegal Arguments
    @Test
    public void testForUpdate_IllegalArguments() {
        Item entity = null;
        when(itemRepository.save(entity)).thenThrow(new IllegalArgumentException("Entity is null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> itemService.update(entity));

        assertThat(exception).hasMessage("Entity is null");
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
    }

    // UTC for update -  run time exception
    @Test
    void testForUpdate_RepositoryThrowsException() {
        Item validEntity = item1;
        when(itemRepository.save(validEntity)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> itemService.update(validEntity));

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("Server error");

        verify(itemRepository, times(1)).save(validEntity);
    }

    // UTC for deleteById - success
    @Test
    public void testForDeleteById_SUCCESS() {
        String id = item1.getId();

        //    	when(itemRepository.existsById(id)).thenReturn(true);

        itemService.deleteById(id);

        verify(itemRepository, times(1)).deleteById(id);
    }

    // UTC for is Exists By Id
    @Test
    public void testForIsExistsById_SUCCESS() {
        // arrange.
        String id = "123455";
        boolean exists = true;
        when(itemRepository.existsById(id)).thenReturn(exists);

        // act
        boolean result = itemService.isExistsById(id);

        // assert
        assertEquals(exists, result);
        verify(itemRepository, times(1)).existsById(id);
    }

    // UTC for exists by id , when id is null
    @Test
    public void testForIsExistsById_IdIsNull() {
        String id = null;
        when(itemRepository.existsById(id)).thenThrow(new IllegalArgumentException("Id cannot be null"));

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> itemService.isExistsById(id));

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(IllegalArgumentException.class);
        assertThat(exception).hasMessage("Id cannot be null");
    }

    // UTC for exists by id , Rum time Exception
    @Test
    public void testForIsExistsById_Exception() {
        String id = "faf56";
        when(itemRepository.existsById(id)).thenThrow(new RuntimeException("Server error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> itemService.isExistsById(id));

        assertThat(exception).isNotNull();
        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception).hasMessage("Server error");

        verify(itemRepository, times(1)).existsById(id);
    }

    // UTC for Find By Field -success
    @Test
    public void testForFindByField_SUCCESS() {
        // arrange
        Class<Item> entityClass = Item.class;
        String fieldName = "id";
        String value = "faf56";

        Item mockEntity = item1;

        Query expectedQuery = new Query(Criteria.where(fieldName).is(value));
        when(mongoTemplate.findOne(expectedQuery, entityClass)).thenReturn(mockEntity);

        // act
        Item result = itemService.findByField(entityClass, fieldName, value);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(mockEntity);
        verify(mongoTemplate, times(1)).findOne(expectedQuery, entityClass);
    }

    // UTC for find By Restaurant
    @Test
    public void testFindByRestaurant() {
        // Arrange
        Class<Item> entityClass = Item.class;
        String value = "4578";
        Query expectedQuery = new Query(Criteria.where("restaurantId").is(value));
        List<Item> mockResult = Arrays.asList(item1, item2);
        when(mongoTemplate.find(expectedQuery, entityClass)).thenReturn(mockResult);

        // act
        List<Item> result = itemService.findByRestaurant(entityClass, value);

        // asserrt
        assertEquals(mockResult, result);
        verify(mongoTemplate, times(1)).find(expectedQuery, entityClass);
    }

    // UTC for find By Query
    @Test
    public void testFindByQuery() {
        Class<Item> entityClass = Item.class;
        Query query = new Query(Criteria.where("fieldName").is("value"));

        List<Item> mockResult = Arrays.asList(item1, item2);

        when(mongoTemplate.find(query, entityClass)).thenReturn(mockResult);

        // ACT
        List<Item> result = itemService.findByQuery(entityClass, query);

        // assert
        assertEquals(mockResult, result);
        verify(mongoTemplate, times(1)).find(query, entityClass);
    }
}
