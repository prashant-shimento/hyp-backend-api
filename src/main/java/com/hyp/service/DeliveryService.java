package com.hyp.service;

import org.springframework.stereotype.Service;

@Service
public class DeliveryService {

	private static final double DELIVERY_RADIUS_KM = 5.0;

	public boolean isLocationDeliverable(double userLatitude, double userLongitude, double restaurantLatitude,
			double restaurantLongitude) {
		double distance = calculateDistance(userLatitude, userLongitude, restaurantLatitude, restaurantLongitude);
		return distance <= DELIVERY_RADIUS_KM;
	}

	private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
		double earthRadius = 6371;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return earthRadius * c;
	}
}
