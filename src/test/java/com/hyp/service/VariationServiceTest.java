package com.hyp.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.Collections;
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
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.test.util.ReflectionTestUtils;

import com.hyp.entity.Variation;
import com.hyp.repository.VariationRepository;




@ExtendWith(MockitoExtension.class)
public class VariationServiceTest {
	
	VariationService variationService;
	
	@Mock
	private VariationRepository variationRepository;
	
	 @InjectMocks
	 private BaseServiceImpl<Variation, String> baseServiceImpl = new BaseServiceImpl<>() {};
	 
	 @Mock
	    private MongoTemplate mongoTemplate;
	    
	    private Variation variation1;
	    private Variation variation2;
	    
	    @BeforeEach
	    public void setup() {
	    	MockitoAnnotations.openMocks(this);
	    	
	    	variationService = new VariationService();
	    	ReflectionTestUtils.setField(variationService, "mongoTemplate", mongoTemplate);
	    	
	    	variation1 = new Variation();
	    	variation1.setId("1");
	    	variation1.setName("2 Pieces");
	    	variation1.setPrice("233");
	    	variation1.setRestaurantId("4758");
	    	variation1.setVariationId("5656");
	    	
	    	variation2 = new Variation();
	    	variation2.setId("2");
	    	variation2.setName("4 Pieces");
	    	variation2.setPrice("422");
	    	variation2.setRestaurantId("4758");
	    	variation2.setVariationId("5657");	
	    	
	    }
	    
	    //UTC for find By Id
	    @Test
	    public void testForFindById_SUCCESS() {
	    		when(variationRepository.findById("1")).thenReturn(Optional.of(variation1));

		        Variation savedVariation = baseServiceImpl.findById("1");
		        
		        assertThat(savedVariation).isNotNull();
		        assertThat(variation1.getId()).isEqualTo("1");
		        verify(variationRepository, times(1)).findById("1");
	   
	    }
	    
	    //UTC for find By Id when Id is null
	    @Test
	    public void testForFindById_InternalServerErrror() {
	    	
	    	String id = null;
	    	when(variationRepository.findById(id)).thenThrow(new IllegalArgumentException("Id can't be null"));
	    	
	    	 IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> baseServiceImpl.findById(id));
	    	
	    	 assertThat(exception).hasMessage("Id can't be null");
	    	 assertThat(exception).isInstanceOf(IllegalArgumentException.class);
	    	
	    }
	    
	    //UTC for find By id on Not found
	    @Test
	    public void testForFindById_NotFound() {
	    	String id = "viv123";
	    	when(variationRepository.findById(id)).thenThrow(new EntityNotFoundException("Entity not found with ID: " + id));
	    	EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> baseServiceImpl.findById(id));
	    	
	        assertThat(exception)
	        .isInstanceOf(EntityNotFoundException.class);
	        assertThat(exception).hasMessage("Entity not found with ID: " + id);

	    	
	    }
	    
	  //UTC for findAll
	    @Test
	    public void testForFindAll_SUCCESS() {
	    	List<Variation> entities = Arrays.asList(variation1, variation2);
	    	
	    	when(variationRepository.findAll()).thenReturn(entities);
	    	
	    	List<Variation> result = baseServiceImpl.findAll();
	    	
	    	assertThat(result).isNotNull();
	    	assertThat(result.size()).isEqualTo(entities.size());
	    	assertThat(result).isEqualTo(entities);
	    	assertThat(result.get(0)).isEqualTo(variation1);
	        verify(variationRepository, times(1)).findAll();
	    	
	    }
	    
	    //UTC for findAll - Exception
	    @Test
	    public void testForFindAll_Exception() {
	    	
	    	when(variationRepository.findAll()).thenThrow(new RuntimeException("Server error"));
	    	
	    	 RuntimeException exception = assertThrows(RuntimeException.class, () -> baseServiceImpl.findAll());
	    	
	    	assertThat(exception).isInstanceOf(RuntimeException.class);
	    	assertThat(exception).hasMessage("Server error");
	    	verify(variationRepository, times(1)).findAll();
	    	
	    }
	    
	    
	    // Vivek -> Copying all
	    //UTC for save success
	    @Test
	    public void testForSave_SUCCESS() {
	    	Variation entity = variation1;
	    	Variation mockeddEntity = variation1;
	    	when(variationRepository.save(entity)).thenReturn(mockeddEntity);
	    	
	    	Variation response = baseServiceImpl.save(entity);
	    	
	    	assertThat(response).isNotNull();
	    	assertThat(response.getId()).isEqualTo(entity.getId());
	    	assertThat(response).isEqualTo(mockeddEntity);
	    	verify(variationRepository, times(1)).save(entity);
	    }
	    
	    //UTC for save - Illegal Arguments
	    @Test
	    public void testForSave_IllegalArguments() {
	    	Variation entity = null;
	    	when(variationRepository.save(entity)).thenThrow(new IllegalArgumentException("Entity is null"));
	    	
	    	IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> baseServiceImpl.save(entity));
	    	
	    	 assertThat(exception).hasMessage("Entity is null");
	    	 assertThat(exception).isInstanceOf(IllegalArgumentException.class);
	    	 
	    	 
	    }
	    
	    //UTC for save run time exception
	    @Test
	    void testSave_RepositoryThrowsException() {
	        Variation validEntity = variation1;
	        when(variationRepository.save(validEntity)).thenThrow(new RuntimeException("Server error"));

	        RuntimeException exception = assertThrows(RuntimeException.class, () -> baseServiceImpl.save(validEntity));

	        assertThat(exception)
	            .isInstanceOf(RuntimeException.class)
	            .hasMessage("Server error");

	        verify(variationRepository, times(1)).save(validEntity);
	    }
	    
	    //UTC for saveALl - success
	    @Test
	    public void testForSaveAll_SUCCESS() {
	    	List<Variation> entities = Arrays.asList(variation1, variation2);
	    	List<Variation> mockedEntities = Arrays.asList(variation1, variation2);
	    	
	    	when(variationRepository.saveAll(entities)).thenReturn(mockedEntities);
	    	
	    	List<Variation> response = baseServiceImpl.saveAll(entities);
	    	
	    	assertThat(response).isNotNull();  
	    	assertThat(response.get(0).getId()).isEqualTo(entities.get(0).getId());
	    	assertThat(response).isEqualTo(entities);
	    	
	    	verify(variationRepository, times(1)).saveAll(entities);
	    }
	    
	  //UTC for save All - Illegal Arguments
	    @Test
	    public void testForSaveAll_IllegalArguments() {
	    	List<Variation> entities = null;
	    	when(variationRepository.saveAll(entities)).thenThrow(new IllegalArgumentException("Entities are null"));
	    	
	    	IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> baseServiceImpl.saveAll(entities));
	    	
	    	 assertThat(exception).hasMessage("Entities are null");
	    	 assertThat(exception).isInstanceOf(IllegalArgumentException.class);

	    }
	    
	  //UTC for save All run time exception
	    @Test
	    void testSaveAll_RepositoryThrowsException() {
	    	List<Variation> entities = Arrays.asList(variation1, variation2);
	        when(variationRepository.saveAll(entities)).thenThrow(new RuntimeException("Server error"));

	        RuntimeException exception = assertThrows(RuntimeException.class, () -> baseServiceImpl.saveAll(entities));

	        assertThat(exception)
	            .isInstanceOf(RuntimeException.class)
	            .hasMessage("Server error");

	        verify(variationRepository, times(1)).saveAll(entities);
	    }
	    
	    //Test for update - success
	    @Test
	    void testUpdate_SUCCESS() {
	    	Variation entity = variation1;
	    	Variation mockeddEntity = variation1;
	    	when(variationRepository.save(entity)).thenReturn(mockeddEntity);
	    	
	    	Variation response = baseServiceImpl.update(entity);
	    	
	    	assertThat(response).isNotNull();
	    	assertThat(response.getId()).isEqualTo(entity.getId());
	    	assertThat(response).isEqualTo(mockeddEntity);
	    	verify(variationRepository, times(1)).save(entity);
	    	
	    }
	    
	    //UTC for update - Illegal Arguments
	    @Test
	    public void testForUpdate_IllegalArguments() {
	    	Variation entity = null;
	    	when(variationRepository.save(entity)).thenThrow(new IllegalArgumentException("Entity is null"));
	    	
	    	IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> baseServiceImpl.update(entity));
	    	
	    	 assertThat(exception).hasMessage("Entity is null");
	    	 assertThat(exception).isInstanceOf(IllegalArgumentException.class);

	    }
	    
	  //UTC for update -  run time exception
	    @Test
	    void testForUpdate_RepositoryThrowsException() {
	        Variation validEntity = variation1;
	        when(variationRepository.save(validEntity)).thenThrow(new RuntimeException("Server error"));

	        RuntimeException exception = assertThrows(RuntimeException.class, () -> baseServiceImpl.update(validEntity));

	        assertThat(exception)
	            .isInstanceOf(RuntimeException.class)
	            .hasMessage("Server error");

	        verify(variationRepository, times(1)).save(validEntity);
	    }
	    
	    //UTC for deleteById - success
	    @Test
	    public void testForDeleteById_SUCCESS() {
	    	String id = variation1.getId();
	    	
//	    	when(variationRepository.existsById(id)).thenReturn(true);
	    	
	    	baseServiceImpl.deleteById(id);
	    	
	    	
	    	verify(variationRepository, times(1)).deleteById(id);

	    }
	    

	  //UTC for is Exists By Id
	    @Test
	    public void testForIsExistsById_SUCCESS() {
	    	//arrange.
	    	String id = "123455";
	    	boolean exists = true;
	    	when(variationRepository.existsById(id)).thenReturn(exists);
	    	
	    	//act
	    	boolean result = baseServiceImpl.isExistsById(id);
	    	
	    	//assert
	    	assertEquals(exists, result);
	    	verify(variationRepository, times(1)).existsById(id);
	    	
	    }
	    
	    //UTC for exists by id , when id is null
	    @Test
	    public void testForIsExistsById_IdIsNull() {
	    	String id = null;
	    	when(variationRepository.existsById(id)).thenThrow(new IllegalArgumentException("Id cannot be null"));
	    	
	    	IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> baseServiceImpl.isExistsById(id));
	    	
	    	assertThat(exception).isNotNull(); 
	    	assertThat(exception).isInstanceOf(IllegalArgumentException.class);
	    	assertThat(exception).hasMessage("Id cannot be null");
	    	
	    }
	    
	  //UTC for exists by id , Rum time Exception 
	    @Test
	    public void testForIsExistsById_Exception() {
	    	String id = "faf56";
	    	when(variationRepository.existsById(id)).thenThrow(new RuntimeException("Server error"));
	    	
	    	RuntimeException exception = assertThrows(RuntimeException.class, () -> baseServiceImpl.isExistsById(id));
	    	
	    	assertThat(exception).isNotNull(); 
	    	assertThat(exception).isInstanceOf(RuntimeException.class);
	    	assertThat(exception).hasMessage("Server error");
	    	
	    	verify(variationRepository, times(1)).existsById(id);
	    	
	    }
	    
	  //UTC for Find By Field -success
	    @Test
	    public void testForFindByField_SUCCESS() {
	    	//arrange
	    	Class<Variation> entityClass = Variation.class;
	    	String fieldName = "id";
	    	String value = "faf56";
	    	
	    	Variation mockEntity = variation1;
	    	
	    	 Query expectedQuery = new Query(Criteria.where(fieldName).is(value));
	    	 when(mongoTemplate.findOne(expectedQuery, entityClass)).thenReturn(mockEntity);
	    	
	    	 
	    	 //act
	    	 Variation result = baseServiceImpl.findByField(entityClass, fieldName, value);
	    	 
	    	// Assert
	    	 assertThat(result).isNotNull();
	        assertThat(result).isEqualTo(mockEntity);
	         verify(mongoTemplate, times(1)).findOne(expectedQuery, entityClass);
	    	
	    }
	    
	  //UTC for find By Restaurant 
	    @Test
	    public void testFindByRestaurant() {
	    	//Arrange
	    	Class<Variation> entityClass = Variation.class;
	    	String value = "4578";
	    	 Query expectedQuery = new Query(Criteria.where("restaurantId").is(value));
	    	 List<Variation> mockResult = Arrays.asList(variation1, variation2);
	    	 when(mongoTemplate.find(expectedQuery, entityClass)).thenReturn(mockResult);
	    	 
	    	 //act 
	    	 List<Variation> result = baseServiceImpl.findByRestaurant(entityClass, value);
	    	 
	    	 //asserrt
	    	 assertEquals(mockResult, result);
	         verify(mongoTemplate, times(1)).find(expectedQuery, entityClass);
	    }
	    
	    
	    //UTC for find By Query
	    @Test
	    public void testFindByQuery() {
	    	Class<Variation> entityClass = Variation.class;
	    	Query query = new Query(Criteria.where("fieldName").is("value"));
	    	
	    	 List<Variation> mockResult = Arrays.asList(variation1, variation2);
	    	 
	    	 when(mongoTemplate.find(query, entityClass)).thenReturn(mockResult);
	    	 
	    	 //ACT
	    	 List<Variation> result = baseServiceImpl.findByQuery(entityClass, query);
	    	 
	    	 //assert
	    	 assertEquals(mockResult, result);
	         verify(mongoTemplate, times(1)).find(query, entityClass); 

	    }
	    
	  //UTC for Get Variations With AddonGroups And ItemsById - SUCCESS
		 @Test
		 public void testForGetVariationsWithAddonGroupsAndItemsById_SUCCESS() {
			 String variationId = "5657";
		      
		        List<Variation> mockEntitites = Arrays.asList(variation1);

		        AggregationResults<Variation> mockAggregationResults = mock(AggregationResults.class);
		        when(mockAggregationResults.getMappedResults()).thenReturn(mockEntitites);

		        when(mongoTemplate.aggregate(
		                any(Aggregation.class), eq("variations"), eq(Variation.class)
		        )).thenReturn(mockAggregationResults);

		        List<Variation> result = variationService.getVariationsWithAddonGroupsAndItemsById(variationId);

		        assertThat(result).isNotNull();
		        assertThat(result).isEqualTo(mockEntitites);

		        verify(mongoTemplate, times(1)).aggregate(
		                any(Aggregation.class), eq("variations"), eq(Variation.class)
		        );
			 
		 }
		 
		 //UTC for Get Variations With AddonGroups And Items By Id Does not exist
		 @Test
		 void testForGetVariationsWithAddonGroupsAndItemsById_DoesNotExist() {
			 String variationId = "gygudsua66";

			    AggregationResults<Variation> mockAggregationResults = mock(AggregationResults.class);
			    when(mockAggregationResults.getMappedResults()).thenReturn(Collections.emptyList());

			    when(mongoTemplate.aggregate(any(Aggregation.class), eq("variations"), eq(Variation.class)))
			        .thenReturn(mockAggregationResults);

			    List<Variation> result = variationService.getVariationsWithAddonGroupsAndItemsById(variationId);

			    assertThat(result).isNotNull(); 
			    
			    assertThat(result).isEmpty();
			    verify(mongoTemplate, times(1)).aggregate(any(Aggregation.class), eq("variations"), eq(Variation.class));
			
			}
		 
		 //UTC for get Variations With AddonGroups And Items - success
		 @Test
		 void testForGetVariationsWithAddonGroupsAndItems_SUCCESS() {
			 List<Variation> mockResults = Arrays.asList(variation1);

			    AggregationResults<Variation> mockAggregationResults = mock(AggregationResults.class);
			    when(mockAggregationResults.getMappedResults()).thenReturn(mockResults);

			    when(mongoTemplate.aggregate(any(Aggregation.class), eq("variations"), eq(Variation.class)))
			        .thenReturn(mockAggregationResults);

			    List<Variation> result = variationService.getVariationsWithAddonGroupsAndItems();

			    assertThat(result)
			        .isNotNull()
			        .hasSize(1);
			    verify(mongoTemplate, times(1)).aggregate(any(Aggregation.class), eq("variations"), eq(Variation.class));
	 

		 }
		 
		 
		 //UTC for get Variations With AddonGroups And Items - Id does not exist
		 @Test
		 void testForGetVariationsWithAddonGroupsAndItems_DoesNotExist() {

			 AggregationResults<Variation> mockAggregationResults = mock(AggregationResults.class);
			 
			 when(mockAggregationResults.getMappedResults()).thenReturn(Collections.emptyList());
			 when(mongoTemplate.aggregate(any(Aggregation.class), eq("variations"), eq(Variation.class)))
		        .thenReturn(mockAggregationResults);
			
			 List<Variation> result = variationService.getVariationsWithAddonGroupsAndItems();
			
			 assertThat(result).isNotNull().isEmpty();;  
			 verify(mongoTemplate, times(1)).aggregate(any(Aggregation.class), eq("variations"), eq(Variation.class));
		 }

}
