package com.hyp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hyp.dto.AddressDto;
import com.hyp.entity.Restaurant;
import com.hyp.model.Location;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

	@Spy
	private LocationService locationService;

	@Test
	void getServiceableRestaurants_Found() {
		AddressDto addressPlaceData = new AddressDto();
		addressPlaceData.setLocation(new Location(28.7041, 77.1025));

		List<Restaurant> restaurants = new ArrayList<>();

		Restaurant rest1 = new Restaurant();
		rest1.setId("rest1");
		rest1.setLocation(new Location(28.7042, 77.10216));
		rest1.setDeliveryRadius(1.0);
		restaurants.add(rest1);

		Restaurant rest2 = new Restaurant();
		rest2.setId("rest2");
		rest2.setLocation(new Location(28.7043, 77.1025));
		rest2.setDeliveryRadius(2.0);
		restaurants.add(rest2);

		Restaurant rest3 = new Restaurant();
		rest3.setId("rest3");
		rest3.setLocation(new Location(19.0760, 72.8777));
		rest3.setDeliveryRadius(1.0);
		restaurants.add(rest3);

		List<String> serviceableRestaurants = locationService.getServiceableRestaurants(addressPlaceData, restaurants);

		assertNotNull(serviceableRestaurants, "Serviceable restaurants list should not be null.");
		assertEquals(2, serviceableRestaurants.size(), "Mismatch in expected deliverable restaurant count.");
		assertTrue(serviceableRestaurants.contains("rest1"), "Expected rest1 to be deliverable.");
		assertTrue(serviceableRestaurants.contains("rest2"), "Expected rest2 to be deliverable.");
		assertFalse(serviceableRestaurants.contains("rest3"), "Expected rest3 to be non-deliverable.");
	}

	@Test
	void getServiceableRestaurants_NoneFound() {
		AddressDto addressPlaceData = new AddressDto();
		addressPlaceData.setLocation(new Location(19.0760, 72.8777)); // Different location

		List<Restaurant> restaurants = new ArrayList<>();

		Restaurant rest1 = new Restaurant();
		rest1.setId("rest1");
		rest1.setLocation(new Location(28.7042, 77.10216));
		rest1.setDeliveryRadius(1.0);
		restaurants.add(rest1);

		Restaurant rest2 = new Restaurant();
		rest2.setId("rest2");
		rest2.setLocation(new Location(28.7043, 77.1025));
		rest2.setDeliveryRadius(1.0);
		restaurants.add(rest2);

		Restaurant rest3 = new Restaurant();
		rest3.setId("rest3");
		rest3.setLocation(new Location(28.7041, 77.1025));
		rest3.setDeliveryRadius(0.0);
		restaurants.add(rest3);

		List<String> serviceableRestaurants = locationService.getServiceableRestaurants(addressPlaceData, restaurants);

		assertNotNull(serviceableRestaurants, "Serviceable restaurants list should not be null.");
		assertEquals(0, serviceableRestaurants.size(), "Expected no restaurants to be deliverable.");
		assertFalse(serviceableRestaurants.contains("rest1"), "Expected rest1 to be non-deliverable.");
		assertFalse(serviceableRestaurants.contains("rest2"), "Expected rest2 to be non-deliverable.");
		assertFalse(serviceableRestaurants.contains("rest3"), "Expected rest3 to be non-deliverable.");
	}

	@Test
	void locationDeliverable_WithinRadius() {
	    when(locationService.isLocationDeliverable(28.7041, 77.1025, 28.7042, 77.10216, 30))
	            .thenReturn(true); 
	    boolean result = locationService.isLocationDeliverable(28.7041, 77.1025, 28.7042, 77.10216, 30);
	    assertTrue(result, "Expected location to be deliverable within the radius.");
	}

	@Test
	void locationDeliverable_AtRadius() {
	    when(locationService.isLocationDeliverable(28.7041, 77.1025, 28.7041, 78.1025, 100))
	            .thenReturn(true); 
	    boolean result = locationService.isLocationDeliverable(28.7041, 77.1025, 28.7041, 78.1025, 100);
	    assertTrue(result, "Expected location to be deliverable exactly at the radius.");
	}

	@Test
	void locationDeliverable_ZeroRadius() {
	    when(locationService.isLocationDeliverable(28.7041, 77.1025, 28.7041, 77.1025, 0))
	            .thenReturn(true);
	    boolean result = locationService.isLocationDeliverable(28.7041, 77.1025, 28.7041, 77.1025, 0);
	    assertTrue(result, "Expected location to be deliverable when radius is 0 and locations are the same.");
	}

	@Test
	void locationNotDeliverable_OutsideRadius() {
	    when(locationService.isLocationDeliverable(28.7041, 77.1025, 19.0760, 72.8777, 1000))
	            .thenReturn(false); 
	    boolean result = locationService.isLocationDeliverable(28.7041, 77.1025, 19.0760, 72.8777, 1000);
	    assertFalse(result, "Expected location not to be deliverable beyond the radius.");
	}

	@Test
	void locationNotDeliverable_ZeroRadiusAndDifferentLocation() {
	    when(locationService.isLocationDeliverable(28.7041, 77.1025, 28.5355, 77.3910, 0))
	            .thenReturn(false); 
	    boolean result = locationService.isLocationDeliverable(28.7041, 77.1025, 28.5355, 77.3910, 0);
	    assertFalse(result, "Expected location not to be deliverable when radius is 0 and locations are different.");


	}
}