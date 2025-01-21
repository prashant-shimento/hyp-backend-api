package com.hyp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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



import com.hyp.dto.PartnerDto;
import com.hyp.entity.Partner;
import com.hyp.enums.PartnerType;
import com.hyp.response.Response;
import com.hyp.service.PartnerService;
import com.hyp.translation.PartnerTranslation;
import com.hyp.util.QueryUtils;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class PartnerControllerTest {
	
	   
	@Mock
	private PartnerService partnerService;
	
	@Mock
	private PartnerTranslation partnerTranslation;
	
	
	
	@InjectMocks
	private BaseController<PartnerDto, Partner, String> partnerController = new BaseController<PartnerDto, Partner, String>() {};

	Partner partner;
	Partner partner2;
	PartnerDto partnerDto;
	PartnerDto partner2Dto;
	
	
	@BeforeEach
	public void setup() {
		 MockitoAnnotations.openMocks(this);
		 
		 	partner = new Partner();
	        partner.setId("123455");
	        partner.setName("Vivek");
	        partner.setType(PartnerType.NOTIFICATION);
	        partner.setIntegrated(true);
	        partner.setDomain("www.vivek.co.uk");
	        partner.setLogoUrl("www.viviwk.co.logo:/23");
	        
	        
	        partnerDto = new PartnerDto();
	        partnerDto.setId("123455");
	        partnerDto.setName("Vivek");
	        partnerDto.setType(PartnerType.NOTIFICATION);
	        partnerDto.setIntegrated(true);
	        partnerDto.setDomain("www.vivek.co.uk");
	        
	        
	        partner2 = new Partner();
	       
	        partner2.setId("27878");
	        partner2.setName("Partner Two");
	        partner2.setType(PartnerType.DELIVERY);
	        partner2.setIntegrated(false);
	        partner2.setDomain("www.partnertwo.com");
	        partner2.setLogoUrl("www.partnertwo.com/logo");	        
	        
	        partner2Dto = new PartnerDto();
	        partner2Dto.setId("27878");
	        partner2Dto.setName("Partner Two");
	        partner2Dto.setType(PartnerType.DELIVERY);
	        partner2Dto.setIntegrated(false);
	        partner2Dto.setDomain("www.partnertwo.com");
	}
	
	
	//Test to get all
	
	
		@Test
		public void testForGetAll_SUCCESS() {
			Map<String, String> queryParams = new HashMap<>();
			queryParams.put("name_eq", "Vivek");
			Query mockQuery = mock(Query.class);

			//MockedStatic method that you can use to create a mock object for a static method.
			try (MockedStatic<QueryUtils> mockedStatic = Mockito.mockStatic(QueryUtils.class)) {
				mockedStatic.when(() -> QueryUtils.getFilterQuery(queryParams,
						QueryUtils.getAllowedParameters(Partner.class.getSimpleName()))).thenReturn(mockQuery);
				ResponseEntity<Response> responseEntity = partnerController.getAll(queryParams);
				assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
				assertThat(responseEntity.getBody().isError()).isFalse();
				assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
				}catch (RuntimeException e) {
				    assertThat(e.getMessage()).isEqualTo("Expected exception");
				    e.printStackTrace();
				}
			
		}
		
		@Test
		public void testForGetAll_BadRequest() {
		    //Arrange
		    Map<String, String> queryParams = new HashMap<>(); 
		    try (MockedStatic<QueryUtils> mockedStatic = Mockito.mockStatic(QueryUtils.class)) {
		        mockedStatic.when(() -> QueryUtils.getFilterQuery(queryParams,
		                QueryUtils.getAllowedParameters(Partner.class.getSimpleName())))
		                .thenThrow(new IllegalArgumentException("Invalid query parameters"));

		        //Act
		        ResponseEntity<Response> responseEntity = null;
		        try {
		            responseEntity = partnerController.getAll(queryParams);
		        } catch (IllegalArgumentException e) {
		            //Assert
		            assertThat(e.getMessage()).isEqualTo("Invalid query parameters");
		        }

		    
		        if (responseEntity != null) {
		            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		            assertThat(responseEntity.getBody().isError()).isTrue();
		            assertThat(responseEntity.getBody().getMessage()).isEqualTo("Invalid query parameters");
		        }
		    }
		}
		
		//Test to Get By ID
		@Test
		public void testForGetById_SUCCESS() {
			//arrange
			String id = "27878";
			 
			try {
				when(partnerService.findById(id)).thenReturn(partner2);
				when(partnerTranslation.getDto(partner2)).thenReturn(partner2Dto);
				
				//act
				ResponseEntity<Response> responseEntity = partnerController.getById(id);
				
				//assert
				 assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
			     assertThat(responseEntity.getBody().isError()).isFalse();
			     assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
			    
				
			}catch (RuntimeException e) {
			    assertThat(e.getMessage()).isEqualTo("Expected exception");
			    e.printStackTrace();
			}
			
		}
		

		@Test
		public void testForGetById_BadRequest() {
			//arrange
			String id = null;
			when(partnerService.findById(id)).thenThrow(new RuntimeException("Server error"));
			ResponseEntity<Response> responseEntity = partnerController.getById(id);
			
			 	assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		        assertThat(responseEntity.getBody().isError()).isTrue();
		        assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
		        assertThat(responseEntity.getBody().getData()).isNull();
		}
		
		//UTC for find By Id - Not found
		  @Test
		    void testForFindById_NotFound() {
		        // Arrange
		        String id = "111ff";
		        when(partnerService.findById(id)).thenReturn(null);

		        //act
		        ResponseEntity<Response> responseEntity = partnerController.getById(id);

		        //aassert
		        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		    }
		  
		  //UTC for find By Id Server error
		  @Test
		    void testFindById_InternalServerError() {
		        //Arrange
			   String id = "111ff";
		        when(partnerService.findById(id)).thenThrow(new RuntimeException("Server error"));

		        //act
		        ResponseEntity<Response> responseEntity = partnerController.getById(id);

		        // Assert
		        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		        assertThat(responseEntity.getBody().isError()).isTrue();
		        assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
		        assertThat(responseEntity.getBody().getData()).isNull();
		    }
		  
		  
		  // UTC for create on success
		  @Test
		  public void testForCreate_success() {
			  
			//arrange
			    when(partnerTranslation.getEntity(partnerDto)).thenReturn(partner);
				when(partnerService.save(partner)).thenReturn(partner);
				when(partnerTranslation.getDto(partner)).thenReturn(partnerDto);
				
				//act
				 ResponseEntity<Response> responseEntity = partnerController.create(partnerDto);
				 
				 //assert
				 assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
				 assertThat(responseEntity.getBody()).isNotNull();
				 assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(partnerDto));
				 assertThat(responseEntity.getBody().isError()).isFalse();
				 assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
		  }
		  
		  //UTC for create on server error
		  
		  @Test
		  public void testForCreate_InternalServerError() {

		      when(partnerTranslation.getEntity(partnerDto)).thenReturn(partner);
		      when(partnerService.save(partner))
		          .thenThrow(new RuntimeException("Server error"));


		      ResponseEntity<Response> responseEntity = partnerController.create(partnerDto);

		      assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		      assertThat(responseEntity.getBody()).isNotNull();
		      assertThat(responseEntity.getBody().isError()).isTrue();
		      assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
		  }
		  
		  
		 //UTC for update success
		  @Test
			public void testForUpdate() {
			  //Arrange
			    when(partnerService.findById("123455")).thenReturn(partner);
			    doNothing().when(partnerTranslation).updateEntityFromDto(partnerDto, partner);
			    when(partnerService.save(partner)).thenReturn(partner);
			    when(partnerTranslation.getDto(partner)).thenReturn(partnerDto);
			    
			    //act
			    ResponseEntity<Response> responseEntity = partnerController.update("123455", partnerDto);

			    //assert
			    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
			    assertThat(responseEntity.getBody()).isNotNull();
			    assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(partnerDto));
			    assertThat(responseEntity.getBody().isError()).isFalse();
			    assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
			    
			   
			}
		  
		  //UTC for update not found
		  @Test
		    void testForUpdate_NotFound() {
		        when(partnerService.findById("123455")).thenReturn(null);

		        ResponseEntity<Response> response = partnerController.update("123455", partnerDto);

		        assertNotNull(response);
		        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		        assertNull(response.getBody());
		    }
		  
		  //Test update - server error
		  @Test
		    void testForUpdate_InternalServerError() throws Exception {
		        when(partnerService.findById("123455")).thenThrow(new RuntimeException("Server error"));

		        ResponseEntity<Response> responseEntity = partnerController.update("123455", partnerDto);

		        assertNotNull(responseEntity);
		        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
			      assertThat(responseEntity.getBody()).isNotNull();
			      assertThat(responseEntity.getBody().isError()).isTrue();
			      assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
		        
		        
		    }
		  
		  //UTC for delete - success
		  @Test
			public void testForDelete_SUCCESS() {
				when(partnerService.findById("123455")).thenReturn(partner);
				doNothing().when(partnerService).deleteById("123455");
				
				ResponseEntity<Response> responseEntity = partnerController.delete("123455");
				assertThat(responseEntity).isNotNull();
				assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
				assertThat(responseEntity.getBody()).isNull(); 
			}
		  
		  //UTC for delete - not found case
		  @Test
		  public void testForDelete_NotFound() {
			  when(partnerService.findById("123455")).thenReturn(null);
			  
			  ResponseEntity<Response> responseEntity = partnerController.delete("123455");
			  
			  assertThat(responseEntity).isNotNull();
			  assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
			  assertThat(responseEntity.getBody()).isNull(); 
		  }
		  
		  // Test for Server error while deleting
		  @Test
		  public void testForDelete_InternalServerError() {
			  when(partnerService.findById("123455")).thenThrow(new RuntimeException("Server error"));
			  
			  ResponseEntity<Response> responseEntity = partnerController.delete("123455");
			  
			  assertThat(responseEntity).isNotNull();
			  assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
			  assertThat(responseEntity.getBody()).isNotNull();
		      assertThat(responseEntity.getBody().isError()).isTrue();
		      assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
		  
		  }
		  
		  //Test for deletion while the id is null
		  @Test
		  public void testForDelete_IdIsNull() {
			  String id = null;
			  
			  when(partnerService.findById(id)).thenThrow(new RuntimeException("Server error"));
			  
			  ResponseEntity<Response> responseEntity = partnerController.delete(id);
			  
			  assertThat(responseEntity).isNotNull();
			  assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
			  assertThat(responseEntity.getBody()).isNotNull();
		  }
 
	}
	

