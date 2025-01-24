package com.hyp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.hyp.dto.AddressDto;
import com.hyp.entity.Address;
import com.hyp.model.Location;
import com.hyp.response.Response;
import com.hyp.service.AddressService;
import com.hyp.translation.AddressTranslation;
import com.hyp.util.QueryUtils;

@ExtendWith(MockitoExtension.class)
public class AddressControllerTest {

	@Mock
	private AddressService addressService;

	@Mock
	private AddressTranslation addressTranslation;

	@InjectMocks
	private BaseController<AddressDto, Address, String> addressController = new BaseController<AddressDto, Address, String>() {
	};

	private Address address;
	private AddressDto addressDto;

	@BeforeEach
	public void setup() {
		Location location = new Location();
		location.setLatitude(12.971598);
		location.setLongitude(77.594566);

		address = new Address();
		address.setId("1");
		address.setAddressType("home");
		address.setAddressOne("123 Main St");
		address.setAddressTwo(null);
		address.setLandmark("Landmark A");
		address.setCity("City");
		address.setState("State");
		address.setCountry("Country");
		address.setPincode("12345");
		address.setCustomerId("2132123");
		address.setLocation(location);

		addressDto = new AddressDto();
		addressDto.setCustomerId("2132123");
		addressDto.setAddressType("home");
		addressDto.setAddressOne("123 Main St");
		addressDto.setAddressTwo(null);
		addressDto.setLandmark("Landmark A");
		addressDto.setCity("City");
		addressDto.setState("State");
		addressDto.setCountry("Country");
		addressDto.setPincode("12345");
		addressDto.setLocation(location);
	}

	@Test
    public void getAddressById_success() {
        when(addressService.findById("1")).thenReturn(address);
        when(addressTranslation.getDto(address)).thenReturn(addressDto);
        ResponseEntity<Response> responseEntity = addressController.getById("1");
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(addressDto));
        assertThat(responseEntity.getBody().isError()).isFalse();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
    }

	@Test
    public void getAddressById_notFound() {
        when(addressService.findById("1")).thenReturn(null);
        ResponseEntity<Response> responseEntity = addressController.getById("1");
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNull();
    }

	@Test
    public void createAddress_success() {
        when(addressTranslation.getEntity(addressDto)).thenReturn(address);
        when(addressService.save(address)).thenReturn(address);
        when(addressTranslation.getDto(address)).thenReturn(addressDto);

        ResponseEntity<Response> responseEntity = addressController.create(addressDto);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(addressDto));
        assertThat(responseEntity.getBody().isError()).isFalse();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
    }

	@Test
    public void updateAddress_success() {
        when(addressService.findById("1")).thenReturn(address);
        Mockito.doNothing().when(addressTranslation).updateEntityFromDto(addressDto, address);
        when(addressService.save(any(Address.class))).thenReturn(address);
        when(addressTranslation.getDto(address)).thenReturn(addressDto);

        ResponseEntity<Response> responseEntity = addressController.update("1", addressDto);

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().getData()).isEqualTo(Collections.singletonList(addressDto));
        assertThat(responseEntity.getBody().isError()).isFalse();
        assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
    }

	@Test
    public void updateAddress_notFound() {
        when(addressService.findById("1")).thenReturn(null);
        ResponseEntity<Response> responseEntity = addressController.update("1", addressDto);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

	@Test
    public void deleteAddress_success() {
        when(addressService.findById(address.getId())).thenReturn(address);
        doNothing().when(addressService).deleteById(address.getId());
        ResponseEntity<Response> responseEntity = addressController.delete(address.getId());
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNull();
    }

	@Test
    public void deleteAddress_notFound() {
        when(addressService.findById(address.getId())).thenReturn(null);
        ResponseEntity<Response> responseEntity = addressController.delete(address.getId());
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseEntity.getBody()).isNull();
    }

	@Test
	public void getAllAddresses_success() {
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("customerId_gt", "212321");
		Query mockQuery = mock(Query.class);
		when(QueryUtils.getFilterQuery(queryParams, QueryUtils.getAllowedParameters(Address.class.getSimpleName())))
				.thenReturn(mockQuery);
		ResponseEntity<Response> responseEntity = addressController.getAll(queryParams);
		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(responseEntity.getBody().isError()).isFalse();
		assertThat(responseEntity.getBody().getMessage()).isEqualTo("success");
	}

	@Test
	public void getAllAddresses_badRequest() {
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("customerId", "4565");

		ResponseEntity<Response> responseEntity = addressController.getAll(queryParams);

		assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(responseEntity.getBody().isError()).isTrue();
		assertThat(responseEntity.getBody().getMessage()).isEqualTo("Invalid query parameters");
	}
}
