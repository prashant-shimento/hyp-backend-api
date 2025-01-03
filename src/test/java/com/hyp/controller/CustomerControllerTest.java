package com.hyp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.hyp.dto.CustomerDto;
import com.hyp.entity.Customer;
import com.hyp.response.Response;
import com.hyp.service.CustomerService;
import com.hyp.translation.CustomerTranslation;
import com.hyp.util.QueryUtils;

@ExtendWith(MockitoExtension.class)
public class CustomerControllerTest {

	@Mock
	private CustomerService customerService;

	@Mock
	private CustomerTranslation customerTranslation;

	@InjectMocks
	private BaseController<CustomerDto, Customer, String> customerController = new BaseController<CustomerDto, Customer, String>() {
	};

	private Customer customer;
	private CustomerDto customerDto;

	@BeforeEach
	public void setup() {
		customer = new Customer();
		customer.setId("100010");
		customer.setName("Aasif Khan");
		customer.setEmail("aasifkhan1088@gmail.com");
		customer.setMobile("9182650986");
		customer.setVerified(true);
		customer.setCreatedAt(LocalDateTime.now());
		customer.setUpdatedAt(LocalDateTime.now());

		customerDto = new CustomerDto();
		customerDto.setId("100010");
		customerDto.setName("Aasif Khan");
		customerDto.setEmail("aasifkhan1088@gmail.com");
		customerDto.setMobile("9182650986");
	}

	@Test
	public void getById_success() {
        when(customerService.findById("100010")).thenReturn(customer);
        when(customerTranslation.getDto(customer)).thenReturn(customerDto);

        ResponseEntity<Response> responseEntity = customerController.getById("100010");

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(customerDto));
        assertThat(responseEntity.getBody().isError()).isFalse();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
    }

	@Test
	public void getById_notFound() {
        when(customerService.findById("100010")).thenReturn(null);
        ResponseEntity<Response> responseEntity = customerController.getById("100010");

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNull();
    }

	@Test
	public void createCustomer_success() {
        when(customerTranslation.getEntity(customerDto)).thenReturn(customer);
        when(customerService.save(customer)).thenReturn(customer);
        when(customerTranslation.getDto(customer)).thenReturn(customerDto);

        ResponseEntity<Response> responseEntity = customerController.create(customerDto);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(customerDto));
        assertThat(responseEntity.getBody().isError()).isFalse();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
    }

	@Test
	public void updateCustomer_success() {
        when(customerService.findById("100010")).thenReturn(customer);
        Mockito.doNothing().when(customerTranslation).updateEntityFromDto(customerDto, customer);
        when(customerService.save(any(Customer.class))).thenReturn(customer);
        when(customerTranslation.getDto(customer)).thenReturn(customerDto);

        ResponseEntity<Response> responseEntity = customerController.update("100010", customerDto);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(customerDto));
        assertThat(responseEntity.getBody().isError()).isFalse();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
    }

	@Test
	public void updateCustomer_notFound() {
        when(customerService.findById("100010")).thenReturn(null);
        ResponseEntity<Response> responseEntity = customerController.update("100010", customerDto);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

	@Test
	public void deleteCustomer_success() {
        when(customerService.findById("100010")).thenReturn(customer);
        doNothing().when(customerService).deleteById("100010");

        ResponseEntity<Response> responseEntity = customerController.delete("100010");

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNull();
    }

	@Test
	public void deleteCustomer_notFound() {
        when(customerService.findById("100010")).thenReturn(null);

        ResponseEntity<Response> responseEntity = customerController.delete("100010");

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNull();
    }

	@Test
	public void getAll_success() {
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("name_eq", "Aasif");
		Query mockQuery = mock(Query.class);

		when(QueryUtils.getFilterQuery(queryParams, QueryUtils.getAllowedParameters(Customer.class.getSimpleName())))
				.thenReturn(mockQuery);

		ResponseEntity<Response> responseEntity = customerController.getAll(queryParams);

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getBody().isError()).isFalse();
		assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
	}

	@Test
	public void getAll_badRequest() {
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("name", "Aasif");

		ResponseEntity<Response> responseEntity = customerController.getAll(queryParams);

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(responseEntity.getBody().isError()).isTrue();
		assertThat(responseEntity.getBody().getMessage()).isEqualTo("Invalid query parameters");
	}
}
