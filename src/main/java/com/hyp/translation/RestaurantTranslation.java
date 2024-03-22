package com.hyp.translation;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hyp.dto.RestaurantDto;
import com.hyp.entity.Restaurant;
import com.hyp.entity.Restaurant.DeliveryHours;
import com.hyp.entity.Restaurant.Location;
import com.hyp.entity.Restaurant.RestaurantTax;
import com.hyp.service.TranslationService;

@Service

public class RestaurantTranslation implements TranslationService<RestaurantDto, Restaurant> {
	@Override
	public Restaurant getEntity(RestaurantDto restaurantDto) {
		if (restaurantDto == null) {
			return null;
		}
		Restaurant restaurant = new Restaurant();
		restaurant.setRestaurantId(restaurantDto.getRestaurantId());
		restaurant.setActive(restaurantDto.isActive());
		RestaurantTax tax = new RestaurantTax();
		List<DeliveryHours> deliveryHours = new ArrayList<>();
		Location location = new Location();
		restaurant.setMenuSharingCode(restaurantDto.getMenuSharingCode());
		restaurant.setCurrencyHtml(restaurantDto.getCurrencyHtml());
		restaurant.setCountry(restaurantDto.getCountry());
		restaurant.setImages(restaurantDto.getImages());
		restaurant.setRestaurantName(restaurantDto.getRestaurantName());
		restaurant.setAddress(restaurantDto.getAddress());
		restaurant.setContact(restaurantDto.getContact());

		location.setLatitude((restaurantDto.getLatitude()));
		location.setLongitude(restaurantDto.getLongitude());
		restaurant.setLocation(location);
		restaurant.setLandmark(restaurantDto.getLandmark());
		restaurant.setCity(restaurantDto.getCity3());
		restaurant.setState(restaurantDto.getState());
		restaurant.setMinimumOrderAmount(restaurantDto.getMinimumOrderAmount());
		restaurant.setMinimumDeliveryTime(restaurantDto.getMinimumDeliveryTime());
		restaurant.setDeliveryCharge(restaurantDto.getDeliveryCharge());

		String deliveryHoursFrom1 = restaurantDto.getDeliveryHoursFrom1();
		String deliveryHoursTo1 = restaurantDto.getDeliveryHoursTo1();
		String deliveryHoursFrom2 = restaurantDto.getDeliveryHoursFrom2();
		String deliveryHoursTo2 = restaurantDto.getDeliveryHoursTo2();

		LocalTime fromTime1 = LocalTime.parse(deliveryHoursFrom1);
		LocalTime toTime1 = LocalTime.parse(deliveryHoursTo1);
		LocalTime fromTime2 = LocalTime.parse(deliveryHoursFrom2);
		LocalTime toTime2 = LocalTime.parse(deliveryHoursTo2);

		DeliveryHours hrs1 = new DeliveryHours();
		hrs1.setFrom(fromTime1);
		hrs1.setTo(toTime1);
		DeliveryHours hrs2 = new DeliveryHours();
		hrs2.setFrom(fromTime2);
		hrs2.setTo(toTime2);

		deliveryHours.add(hrs1);
		deliveryHours.add(hrs2);

		restaurant.setDeliveryHours(deliveryHours);

		restaurant.setCalculateTaxOnPacking(restaurantDto.getCalculateTaxOnPacking());
		tax.setPcTaxesId(restaurantDto.getPcTaxesId());
		restaurant.setCalculateTaxOnDelivery(restaurantDto.getCalculateTaxOnDelivery());
		tax.setDcTaxesId(restaurantDto.getDcTaxesId());
		restaurant.setTax(tax);
		restaurant.setPackagingApplicableOn(restaurantDto.getPackagingApplicableOn());
		restaurant.setPackagingCharge(restaurantDto.getPackagingCharge());
		restaurant.setPackagingChargeType(restaurantDto.getPackagingChargeType());
		return restaurant;

	}

	@Override
	public RestaurantDto getDto(Restaurant entity) {
		RestaurantDto r_dto = new RestaurantDto();

		r_dto.setRestaurantId(entity.getRestaurantId());
		r_dto.setActive(entity.isActive());

		r_dto.setCurrencyHtml(entity.getCurrencyHtml());
		r_dto.setCountry(entity.getCountry());
		r_dto.setMinimumOrderAmount(entity.getMinimumOrderAmount());
		r_dto.setRestaurantName(entity.getRestaurantName());
		r_dto.setPackagingApplicableOn(entity.getPackagingApplicableOn());
		r_dto.setCity3(entity.getCity());
		r_dto.setPackagingCharge(entity.getPackagingCharge());
		r_dto.setCalculateTaxOnDelivery(entity.getCalculateTaxOnDelivery());
		r_dto.setPackagingChargeType(entity.getPackagingChargeType());
		r_dto.setContact(entity.getContact());
		r_dto.setState(entity.getState());
		r_dto.setLandmark(entity.getLandmark());
		r_dto.setLatitude(entity.getLocation().getLatitude());
		r_dto.setLongitude(entity.getLocation().getLongitude());
		r_dto.setImages(entity.getImages());
		r_dto.setAddress(entity.getAddress());
		r_dto.setMenuSharingCode(entity.getMenuSharingCode());
		r_dto.setDeliveryCharge(entity.getDeliveryCharge());
		r_dto.setMinimumDeliveryTime(entity.getMinimumDeliveryTime());

		List<DeliveryHours> deliveryHours2 = entity.getDeliveryHours();
		for (int i = 0; i < deliveryHours2.size(); i++) {
			DeliveryHours d_hours = deliveryHours2.get(i);
			if (i == 0) {
				r_dto.setDeliveryHoursFrom1(d_hours.getFrom().toString());
				r_dto.setDeliveryHoursTo1(d_hours.getTo().toString());
			} else if (i == 1) {
				r_dto.setDeliveryHoursFrom2(d_hours.getFrom().toString());
				r_dto.setDeliveryHoursTo2(d_hours.getTo().toString());
			}
		}

		r_dto.setCalculateTaxOnPacking(entity.getCalculateTaxOnPacking());
		r_dto.setDcTaxesId(entity.getTax().getDcTaxesId());
		r_dto.setPcTaxesId(entity.getTax().getPcTaxesId());

		return r_dto;
	}

	@Override
	public List<RestaurantDto> getDtoList(List<Restaurant> entities) {
//		List<RestaurantDto> dtoList = new ArrayList<>();
//
//		for (Restaurant entity : entities) {
//			
//			RestaurantDto dto = getDto(entity);
//            
//			dtoList.add(dto);
//		}
//
//		return dtoList;

		List<RestaurantDto> dtos = new ArrayList<>();

		for (Restaurant entity : entities) {
			RestaurantDto dto = new RestaurantDto();
			dto.setRestaurantId(entity.getId()); // Assuming you have a getId() method in BaseEntity
			dto.setActive(entity.isActive());
			dto.setMenuSharingCode(entity.getMenuSharingCode());
			dto.setCurrencyHtml(entity.getCurrencyHtml());
			dto.setCountry(entity.getCountry());
			dto.setImages(entity.getImages());
			dto.setRestaurantName(entity.getRestaurantName());
			dto.setAddress(entity.getAddress());
			dto.setContact(entity.getContact());

			if (entity.getLocation() != null) {
				dto.setLatitude(entity.getLocation().getLatitude());
				dto.setLongitude(entity.getLocation().getLongitude());
			}

			dto.setLandmark(entity.getLandmark());
			dto.setCity3(entity.getCity());
			dto.setState(entity.getState());
			dto.setMinimumOrderAmount(entity.getMinimumOrderAmount());
			dto.setMinimumDeliveryTime(entity.getMinimumDeliveryTime());
			dto.setDeliveryCharge(entity.getDeliveryCharge());

			if (entity.getDeliveryHours() != null && entity.getDeliveryHours().size() >= 2) {
				Restaurant.DeliveryHours deliveryHours1 = entity.getDeliveryHours().get(0);
				Restaurant.DeliveryHours deliveryHours2 = entity.getDeliveryHours().get(1);

				if (deliveryHours1 != null) {
					dto.setDeliveryHoursFrom1(deliveryHours1.getFrom().toString());
					dto.setDeliveryHoursTo1(deliveryHours1.getTo().toString());
				}
				if (deliveryHours2 != null) {
					dto.setDeliveryHoursFrom2(deliveryHours2.getFrom().toString());
					dto.setDeliveryHoursTo2(deliveryHours2.getTo().toString());
				}
			}

			// Set tax details
			if (entity.getTax() != null) {
				dto.setScApplicableOn(entity.getTax().getDcTaxesId());
				dto.setScType(entity.getTax().getPcTaxesId());
			}

			dto.setCalculateTaxOnPacking(entity.getCalculateTaxOnPacking());
			dto.setPcTaxesId(entity.getTax().getPcTaxesId());
			dto.setDcTaxesId(entity.getTax().getDcTaxesId());
			dto.setPackagingApplicableOn(entity.getPackagingApplicableOn());
			dto.setPackagingCharge(entity.getPackagingCharge());
			dto.setPackagingChargeType(entity.getPackagingChargeType());

			dtos.add(dto);
		}

		return dtos;
	}

	@Override
	public Restaurant getPatchDto(Restaurant existingEntity, RestaurantDto dto) {
		return null;
	}

}
