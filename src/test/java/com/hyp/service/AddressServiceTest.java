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

import com.hyp.entity.Address;
import com.hyp.model.Location;

@ExtendWith(MockitoExtension.class)
public class AddressServiceTest {

	@Mock
	private MongoRepository<Address, String> addressRepository;

	@InjectMocks
	private BaseServiceImpl<Address, String> addressService = new BaseServiceImpl<>() {
	};

	private Address mockAddress;

	@BeforeEach
	void setUp() {
		Location location = new Location();
		location.setLatitude(12.971598);
		location.setLongitude(77.594566);

		mockAddress = new Address();
		mockAddress.setId("addr12345");
		mockAddress.setAddressType("Home");
		mockAddress.setAddressOne("123 Main Street");
		mockAddress.setAddressTwo("Apt 101");
		mockAddress.setLandmark("Near Central Park");
		mockAddress.setCity("Bangalore");
		mockAddress.setState("Karnataka");
		mockAddress.setCountry("India");
		mockAddress.setPincode("560001");
		mockAddress.setCustomerId("cust56789");
		mockAddress.setLocation(location);
		mockAddress.setCreatedAt(LocalDateTime.now());
		mockAddress.setUpdatedAt(LocalDateTime.now());
		mockAddress.setRestaurantId("rest43210");
	}

	@Test
    void addAddress_success() {
        when(addressRepository.save(mockAddress)).thenReturn(mockAddress);

        Address addedAddress = addressService.save(mockAddress);

        assertNotNull(addedAddress);
        assertEquals(mockAddress.getId(), addedAddress.getId());
        assertEquals(mockAddress.getAddressType(), addedAddress.getAddressType());
        assertEquals(mockAddress.getCity(), addedAddress.getCity());

        verify(addressRepository, times(1)).save(mockAddress);
    }

	@Test
    void findById_success() {
        when(addressRepository.findById("addr12345")).thenReturn(Optional.of(mockAddress));

        Address address = addressService.findById("addr12345");

        assertNotNull(address);
        assertEquals("addr12345", address.getId());
        verify(addressRepository, times(1)).findById("addr12345");
    }

	@Test
    void findById_notFound() {
        when(addressRepository.findById("addr67890")).thenReturn(Optional.empty());

        Address address = addressService.findById("addr67890");

        assertNull(address);
        verify(addressRepository, times(1)).findById("addr67890");
    }

	@Test
	void findAll_success() {
		List<Address> mockAddresses = Arrays.asList(mockAddress, mockAddress);
		when(addressRepository.findAll()).thenReturn(mockAddresses);

		List<Address> addresses = addressService.findAll();

		assertNotNull(addresses);
		assertEquals(2, addresses.size());
		verify(addressRepository, times(1)).findAll();
	}

	@Test
    void updateAddress_success() {
        when(addressRepository.save(mockAddress)).thenReturn(mockAddress);

        Address updatedAddress = addressService.update(mockAddress);

        assertNotNull(updatedAddress);
        assertEquals(mockAddress.getId(), updatedAddress.getId());
        verify(addressRepository, times(1)).save(mockAddress);
    }

	@Test
	void deleteAddress_success() {
		doNothing().when(addressRepository).deleteById("addr12345");
		addressService.deleteById("addr12345");
		verify(addressRepository, times(1)).deleteById("addr12345");
	}
}
