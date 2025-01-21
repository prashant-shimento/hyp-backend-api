package com.hyp.service;
import com.hyp.entity.Partner;
import com.hyp.enums.PartnerType;
import com.hyp.repository.PartnerRepository;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.MongoTemplate;


@ExtendWith(MockitoExtension.class)
public class PartnerServiceTest {

    
//    @Mock
//    private MongoRepository<Partner, String> partnerRepository;
    
	@Mock
	private PartnerRepository partnerRepository;
    
    private PartnerService partnerService;

    @InjectMocks
    private BaseServiceImpl<Partner, String> baseServiceImpl = new BaseServiceImpl<>() {};
 

    @Mock
    private MongoTemplate mongoTemplate;

//    private baseServiceImpl baseServiceImpl;

    private Partner partner;
    private Partner partner2;
    private Partner partner3;
    

    @BeforeEach
    public void setUp() {
    	 MockitoAnnotations.openMocks(this);
    	 
    	 partnerService = new PartnerService();
	     ReflectionTestUtils.setField(partnerService, "partnerRepository", partnerRepository);
       
        partner = new Partner();
        PartnerType type = PartnerType.NOTIFICATION;

        partner.setId("123455");
        partner.setName("Vivek");
        partner.setType(type);
        partner.setIntegrated(true);
        partner.setDomain("www.vivek.co.uk");
        partner.setLogoUrl("www.viviwk.co.logo:/23");
        
      
        partner2 = new Partner();
        partner2.setId("27878");
        partner2.setName("Partner Two");
        partner2.setType(PartnerType.NOTIFICATION);
        partner2.setIntegrated(false);
        partner2.setDomain("www.partnertwo.com");
        partner2.setLogoUrl("www.partnertwo.com/logo");
        
        partner3 = new Partner();
        partner3.setId("27878");
        partner3.setName("Partner Two");
        partner3.setType(PartnerType.DELIVERY);
        partner3.setIntegrated(false);
        partner3.setDomain("www.partnertwo.com");
        partner3.setLogoUrl("www.partnertwo.com/logo");
    }

    //UTC for FindById
    @Test
	 public void testForFindById() {
    	
	        when(partnerRepository.findById("123455")).thenReturn(Optional.of(partner));

	        Partner savedPartner3 = baseServiceImpl.findById("123455");

	        assertNotNull(savedPartner3);
	        assertEquals("123455", partner.getId());
	        verify(partnerRepository, times(1)).findById("123455");
		 
	 }
    
    //UTC for Find all method
    @Test
    public void testForFindAll() {
    	//Arrange 
    	 List<Partner> expectedPartners = Arrays.asList(partner, partner2);
    	 when(partnerRepository.findAll()).thenReturn(expectedPartners);
    	 
    	 //ACT
    	 List<Partner> actualPartners = baseServiceImpl.findAll();
    	 
    	 //Assert
    	 assertEquals(actualPartners, expectedPartners);
    	 verify(partnerRepository, times(1)).findAll();
    	
    }
    

    
    //UTC for save method
    @Test
    public void testForSave() {
    	//ARRANGE
    	Partner entity = partner;
    	Partner mockSavedEntity = partner;
    	
    	when(partnerRepository.save(entity)).thenReturn(mockSavedEntity);
    	
    	// Act
    	Partner results = baseServiceImpl.save(entity);
    	
    	// Assert
        assertEquals(mockSavedEntity, results); 
        verify(partnerRepository, times(1)).save(entity); 
    }
    
    //UTC for saveALL method 
    @Test
    public void testForSaveAll() {
    	//arrange
    	List<Partner> entities = Arrays.asList(partner, partner2);
    	List<Partner> mockSavedEntities = Arrays.asList(partner, partner2);
    	
    	when(partnerRepository.saveAll(entities)).thenReturn(mockSavedEntities);
    	
    	//act
    	List<Partner> results = baseServiceImpl.saveAll(entities);
    	
    	//assert
    	  assertEquals(mockSavedEntities, results); 
          verify(partnerRepository, times(1)).saveAll(entities); 
    }
    
    //UTC for update
    @Test
    public void testForUpdate() {
    	//Arrange 
    	Partner entity = partner;
    	Partner mockSavedEntity = partner;
    	
    	when(partnerRepository.save(entity)).thenReturn(mockSavedEntity);
    	
    	//ACT
    	Partner result = baseServiceImpl.update(entity);
    	
    	//Assert
    	assertEquals(mockSavedEntity, result);
    	verify(partnerRepository, times(1)).save(entity); 
    }
    
    //UTC for delete By ID
    @Test
    public void testForDeleteById() {
    	//Arrange
    	String id = "123455";
    	
    	//ACt
    	baseServiceImpl.deleteById(id);
    	
    	//assert
    	verify(partnerRepository, times(1)).deleteById(id); 
    }
    
    //UTC for is Exists By Id
    @Test
    public void testForIsExistsById() {
    	//arrange.
    	String id = "123455";
    	boolean exists = true;
    	when(partnerRepository.existsById(id)).thenReturn(exists);
    	
    	//act
    	boolean result = baseServiceImpl.isExistsById(id);
    	
    	//assert
    	assertEquals(exists, result);
    	verify(partnerRepository, times(1)).existsById(id);
    	
    	//act
    	
    }
    
    //UTC for Find By Field
    @Test
    public void testForFindByField() {
    	//arrange
    	Class<Partner> entityClass = Partner.class;
    	String fieldName = "name";
    	String value = "Vivek";
    	
    	Partner mockEntity = partner;
    	
    	 Query expectedQuery = new Query(Criteria.where(fieldName).is(value));
    	 when(mongoTemplate.findOne(expectedQuery, entityClass)).thenReturn(mockEntity);
    	
    	 
    	 //act
    	 Partner result = baseServiceImpl.findByField(entityClass, fieldName, value);
    	 
    	// Assert
         assertEquals(mockEntity, result); // Verify the returned value matches the mocked result
         verify(mongoTemplate, times(1)).findOne(expectedQuery, entityClass);
    	
    }
    
    //UTC for find By Restaurant 
    @Test
    public void testFindByRestaurant() {
    	//Arrange
    	Class<Partner> entityClass = Partner.class;
    	String value = "4578";
    	 Query expectedQuery = new Query(Criteria.where("restaurantId").is(value));
    	 List<Partner> mockResult = Arrays.asList(partner, partner2);
    	 when(mongoTemplate.find(expectedQuery, entityClass)).thenReturn(mockResult);
    	 
    	 //act 
    	 List<Partner> result = baseServiceImpl.findByRestaurant(entityClass, value);
    	 
    	 //asserrt
    	 assertEquals(mockResult, result);
         verify(mongoTemplate, times(1)).find(expectedQuery, entityClass);
    }
    
    
    //UTC for find By Query
    @Test
    public void testFindByQuery() {
    	Class<Partner> entityClass = Partner.class;
    	Query query = new Query(Criteria.where("fieldName").is("value"));
    	
    	 List<Partner> mockResult = Arrays.asList(partner, partner2);
    	 
    	 when(mongoTemplate.find(query, entityClass)).thenReturn(mockResult);
    	 
    	 //ACT
    	 List<Partner> result = baseServiceImpl.findByQuery(entityClass, query);
    	 
    	 //assert
    	 assertEquals(mockResult, result);
         verify(mongoTemplate, times(1)).find(query, entityClass); 

    }
    
  //UTC for find By Partner type
    @Test
    public void testForFindByPartnerType() {
    	PartnerType type = PartnerType.DELIVERY;
    	List<Partner> entities = Arrays.asList(partner2, partner3);
    	
    	when(partnerRepository.findByType(type)).thenReturn(entities);
    	
    	List<Partner> result = partnerService.findByPartnerType(type);
    	
    	assertThat(result).isNotNull();
    
    }
    
  //UTC for find By Partner type - Type Does not exist
    @Test
    public void testForFindByPartnerType_DoesNotExist() {
    	
    	PartnerType type = PartnerType.ALERT;
    	
    	when(partnerRepository.findByType(type)).thenReturn(null);
    	
    	List<Partner> result = partnerService.findByPartnerType(type);
    	
    	assertThat(result).isNull();
    
    }
  //UTC for find By Partner type - When Type is null
    @Test
    public void testForFindByPartnerType_Exception() {
    	
    	PartnerType type = null;
    	
    	when(partnerRepository.findByType(type)).thenThrow(new RuntimeException("Server error"));
    	
    	RuntimeException exception = assertThrows(RuntimeException.class, () -> partnerService.findByPartnerType(type));
    	
    	assertThat(exception).isInstanceOf(RuntimeException.class);
    	assertThat(exception.getMessage()).isEqualTo("Server error");
    
    }
    
    //UTC for find Partners By RestaurantId
    @Test
    public void testForfindPartnersByRestaurantId_SUCCESS() {
    	String restaurantId = "4758";
    	PartnerType type = PartnerType.DELIVERY;
    	Partner mockkedEntity = partner;
    	
    	when(partnerRepository.findByRestaurantsContainingAndType(restaurantId, type)).thenReturn(partner);
    	
    	Partner response = partnerService.findPartnersByRestaurantId(restaurantId, type);
    	
    	assertThat(response).isNotNull();
    	assertThat(response).isEqualTo(mockkedEntity);
    	
    }
    
    //UTC for find Partners By RestaurantId
    @Test
    public void testForfindPartnersByRestaurantId_DoesNotExist() {
    	String restaurantId = "47581";
    	PartnerType type = PartnerType.ALERT;
    	
    	when(partnerRepository.findByRestaurantsContainingAndType(restaurantId, type)).thenReturn(null);
    	
    	Partner response = partnerService.findPartnersByRestaurantId(restaurantId, type);
    	
    	assertThat(response).isNull();
    	
    }
    
    
    //UTC for find Partners By RestaurantId when restaurantId or partnerId are null
    @Test
    public void testForfindPartnersByRestaurantId_IdIsNull() {
    	String restaurantId =  null;
    	PartnerType type = null;
    	
    	when(partnerRepository.findByRestaurantsContainingAndType(restaurantId, type)).thenThrow(new RuntimeException("Id or type are null"));
    	
    	RuntimeException exception = assertThrows(RuntimeException.class, ()-> partnerService.findPartnersByRestaurantId(restaurantId, type));
    
    	
    	assertThat(exception).isInstanceOf(RuntimeException.class);
    	assertThat(exception.getMessage()).isEqualTo("Id or type are null");
    	
    }
       

}