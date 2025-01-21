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


import com.hyp.dto.TaxDto;
import com.hyp.entity.Item;

import com.hyp.entity.Tax;
import com.hyp.response.Response;

import com.hyp.service.TaxService;

import com.hyp.translation.TaxTranslation;
import com.hyp.util.QueryUtils;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class TaxControllerTest {
	
	   
	@Mock
	private TaxService taxService;
	
	@Mock
	private TaxTranslation taxTranslation;
	
	
	
	@InjectMocks
	private BaseController<TaxDto, Tax, String> offerController = new BaseController<TaxDto, Tax, String>() {};

	 private Tax tax1;
	 private Tax tax2;
	 
	 private TaxDto tax1Dto;
	 private TaxDto tax2Dto;
	  
	
	
	@BeforeEach
	public void setup() {
		 MockitoAnnotations.openMocks(this);
		    tax1 = new Tax();
	    	tax1.setId("1");
	    	tax1.setTaxName("CGST");
	    	tax1.setTax("2.7");
	    	tax1.setRestaurantId("4758");
	    	
	    	tax1Dto = new TaxDto();
	    	tax1Dto.setId("1");
	    	//tax1Dto.setTaxName("CGST");
	    	tax1Dto.setTax("2.7");
	    	tax1Dto.setRestaurantId("4758");
	    	
	    	
	    	tax2 = new Tax();
	    	tax2.setId("1");
	    	tax2.setTaxName("CGST");
	    	tax2.setTax("2.7");
	    	tax2.setRestaurantId("4758");
	    	
	    	tax2Dto = new TaxDto();
	    	tax2Dto.setId("1");
	    	//tax2Dto.setTaxName("CGST");
	    	tax2Dto.setTax("2.7");
	    	tax2Dto.setRestaurantId("4758");
		 
		  
		 
		 	
	}
	
	
	//Test to get all
	
	
		@Test
		public void testForGetAll_SUCCESS() {
			Map<String, String> queryParams = new HashMap<>();
			queryParams.put("itemName_eq", "Chicken Burger");
			Query mockQuery = mock(Query.class);

			//MockedStatic method that you can use to create a mock object for a static method.
			try (MockedStatic<QueryUtils> mockedStatic = Mockito.mockStatic(QueryUtils.class)) {
				mockedStatic.when(() -> QueryUtils.getFilterQuery(queryParams,
						QueryUtils.getAllowedParameters(Item.class.getSimpleName()))).thenReturn(mockQuery);
				ResponseEntity<Response> responseEntity = offerController.getAll(queryParams);
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
		                QueryUtils.getAllowedParameters(Item.class.getSimpleName())))
		                .thenThrow(new IllegalArgumentException("Invalid query parameters"));

		        //Act
		        ResponseEntity<Response> responseEntity = null;
		        try {
		            responseEntity = offerController.getAll(queryParams);
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
			String id = "1";
			 
			try {
				when(taxService.findById(id)).thenReturn(tax2);
				when(taxTranslation.getDto(tax2)).thenReturn(tax2Dto);
				
				//act
				ResponseEntity<Response> responseEntity = offerController.getById(id);
				
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
			when(taxService.findById(id)).thenThrow(new RuntimeException("Server error"));
			ResponseEntity<Response> responseEntity = offerController.getById(id);
			
			 	assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		        assertThat(responseEntity.getBody().isError()).isTrue();
		        assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
		        assertThat(responseEntity.getBody().getData()).isNull();
		}
		
		//UTC for find By Id - Not found
		  @Test
		    void testForFindById_NotFound() {
		        // Arrange
		        String id = "787ffgg";
		        when(taxService.findById(id)).thenReturn(null);

		        //act
		        ResponseEntity<Response> responseEntity = offerController.getById(id);

		        //aassert
		        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		    }
		  
		  //UTC for find By Id Server error
		  @Test
		    void testFindById_InternalServerError() {
		        //Arrange
			   String id = "111ff";
		        when(taxService.findById(id)).thenThrow(new RuntimeException("Server error"));

		        //act
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
			  
			//arrange
			    when(taxTranslation.getEntity(tax1Dto)).thenReturn(tax1);
				when(taxService.save(tax1)).thenReturn(tax1);
				when(taxTranslation.getDto(tax1)).thenReturn(tax1Dto);
				
				//act
				 ResponseEntity<Response> responseEntity = offerController.create(tax1Dto);
				 
				 //assert
				 assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
				 assertThat(responseEntity.getBody()).isNotNull();
				 assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(tax1Dto));
				 assertThat(responseEntity.getBody().isError()).isFalse();
				 assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
		  }
		  
		  //UTC for create on server error
		  
		  @Test
		  public void testForCreate_InternalServerError() {

		      when(taxTranslation.getEntity(tax1Dto)).thenReturn(tax1);
		      when(taxService.save(tax1))
		          .thenThrow(new RuntimeException("Server error"));


		      ResponseEntity<Response> responseEntity = offerController.create(tax1Dto);

		      assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		      assertThat(responseEntity.getBody()).isNotNull();
		      assertThat(responseEntity.getBody().isError()).isTrue();
		      assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
		  }
		  
		  
		 //UTC for update success
		  @Test
			public void testForUpdate() {
			  //Arrange
			    when(taxService.findById("123455")).thenReturn(tax1);
			    doNothing().when(taxTranslation).updateEntityFromDto(tax1Dto, tax1);
			    when(taxService.save(tax1)).thenReturn(tax1);
			    when(taxTranslation.getDto(tax1)).thenReturn(tax1Dto);
			    
			    //act
			    ResponseEntity<Response> responseEntity = offerController.update("123455", tax1Dto);

			    //assert
			    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
			    assertThat(responseEntity.getBody()).isNotNull();
			    assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(tax1Dto));
			    assertThat(responseEntity.getBody().isError()).isFalse();
			    assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
			    
			   
			}
		  
		  //UTC for update not found
		  @Test
		    void testForUpdate_NotFound() {
		        when(taxService.findById("123455")).thenReturn(null);

		        ResponseEntity<Response> response = offerController.update("123455", tax1Dto);

		        assertNotNull(response);
		        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		        assertNull(response.getBody());
		    }
		  
		  //Test update - server error
		  @Test
		    void testForUpdate_InternalServerError() throws Exception {
		        when(taxService.findById("123455")).thenThrow(new RuntimeException("Server error"));

		        ResponseEntity<Response> responseEntity = offerController.update("123455", tax1Dto);

		        assertNotNull(responseEntity);
		        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
			      assertThat(responseEntity.getBody()).isNotNull();
			      assertThat(responseEntity.getBody().isError()).isTrue();
			      assertThat(responseEntity.getBody().getMessage()).isEqualTo("Server error");
		        
		        
		    }
		  
		  //UTC for delete - success
		  @Test
			public void testForDelete_SUCCESS() {
				when(taxService.findById("1")).thenReturn(tax1);
				doNothing().when(taxService).deleteById("1");
				
				ResponseEntity<Response> responseEntity = offerController.delete("1");
				assertThat(responseEntity).isNotNull();
				assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
				assertThat(responseEntity.getBody()).isNull(); 
			}
		  
		  //UTC for delete - not found case
		  @Test
		  public void testForDelete_NotFound() {
			  when(taxService.findById("123455")).thenReturn(null);
			  
			  ResponseEntity<Response> responseEntity = offerController.delete("123455");
			  
			  assertThat(responseEntity).isNotNull();
			  assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
			  assertThat(responseEntity.getBody()).isNull(); 
		  }
		  
		  // Test for Server error while deleting
		  @Test
		  public void testForDelete_InternalServerError() {
			  when(taxService.findById("123455")).thenThrow(new RuntimeException("Server error"));
			  
			  ResponseEntity<Response> responseEntity = offerController.delete("123455");
			  
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
			  
			  when(taxService.findById(id)).thenThrow(new RuntimeException("Server error"));
			  
			  ResponseEntity<Response> responseEntity = offerController.delete(id);
			  
			  assertThat(responseEntity).isNotNull();
			  assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
			  assertThat(responseEntity.getBody()).isNotNull();
		  }
 
	}
	

