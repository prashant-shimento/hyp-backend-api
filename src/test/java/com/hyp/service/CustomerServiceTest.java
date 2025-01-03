package com.hyp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.hyp.dto.CustomerDto;
import com.hyp.entity.Customer;

@ExtendWith(MockitoExtension.class)
public class CustomerServiceTest {

	@Mock
	private MongoRepository<Customer, String> customerRepository;

	@InjectMocks
	private BaseServiceImpl<Customer, String> customerService = new BaseServiceImpl<>() {
	};

	private Customer mockCustomer;
	private CustomerDto customerDto;

	@BeforeEach
	public void setup() {
		mockCustomer = new Customer();
		mockCustomer.setId("100010");
		mockCustomer.setName("Aasif Khan");
		mockCustomer.setEmail("aasifkhan1088@gmail.com");
		mockCustomer.setMobile("9182650986");
		mockCustomer.setVerified(true);
		mockCustomer.setCreatedAt(LocalDateTime.now());
		mockCustomer.setUpdatedAt(LocalDateTime.now());

		customerDto = new CustomerDto();
		customerDto.setId("100010");
		customerDto.setName("Aasif Khan");
		customerDto.setEmail("aasifkhan1088@gmail.com");
		customerDto.setMobile("9182650986");
	}

	@Test
    void addCustomer_success() {
        when(customerRepository.save(mockCustomer)).thenReturn(mockCustomer);

        Customer addedCustomer = customerService.save(mockCustomer);

        assertNotNull(addedCustomer);
        assertEquals(mockCustomer.getId(), addedCustomer.getId());
        assertEquals(mockCustomer.getName(), addedCustomer.getName());
        assertEquals(mockCustomer.getEmail(), addedCustomer.getEmail());

        verify(customerRepository, times(1)).save(mockCustomer);
    }

	@Test
    void findCustomerById_success() {
        when(customerRepository.findById("100010")).thenReturn(Optional.of(mockCustomer));

        Customer customer = customerService.findById("100010");

        assertNotNull(customer);
        assertEquals("100010", customer.getId());
        verify(customerRepository, times(1)).findById("100010");
    }

	@Test
    void findCustomerById_notFound() {
        when(customerRepository.findById("cust67890")).thenReturn(Optional.empty());

        Customer customer = customerService.findById("cust67890");

        assertNull(customer);
        verify(customerRepository, times(1)).findById("cust67890");
    }

	@Test
	void findAllCustomers_success() {
		List<Customer> mockCustomers = Arrays.asList(mockCustomer, mockCustomer);
		when(customerRepository.findAll()).thenReturn(mockCustomers);

		List<Customer> customers = customerService.findAll();

		assertNotNull(customers);
		assertEquals(2, customers.size());
		verify(customerRepository, times(1)).findAll();
	}

	@Test
    void updateCustomer_success() {
        when(customerRepository.save(mockCustomer)).thenReturn(mockCustomer);

        Customer updatedCustomer = customerService.update(mockCustomer);

        assertNotNull(updatedCustomer);
        assertEquals(mockCustomer.getId(), updatedCustomer.getId());
        verify(customerRepository, times(1)).save(mockCustomer);
    }

	@Test
	void deleteCustomer_success() {
		doNothing().when(customerRepository).deleteById("100010");
		customerService.deleteById("100010");
		verify(customerRepository, times(1)).deleteById("100010");
	}

}
