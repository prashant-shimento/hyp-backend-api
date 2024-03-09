package com.hyp.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hyp.dto.RestaurantDto;
import com.hyp.entity.Restaurant;
import com.hyp.entity.Restaurant.DeliveryHours;
import com.hyp.entity.Restaurant.Location;
import com.hyp.entity.Restaurant.RestaurantTax;

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
		DeliveryHours hrs = new DeliveryHours();
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
		LocalTime fromTime = LocalTime.parse(deliveryHoursFrom1);
		LocalTime parse = LocalTime.parse(deliveryHoursTo1);
		LocalTime parse2 = LocalTime.parse(deliveryHoursFrom2);
		LocalTime parse3 = LocalTime.parse(deliveryHoursTo2);
		hrs.setFrom(fromTime);
		hrs.setFrom(parse2);
		hrs.setTo(parse);
		hrs.setFrom(parse3);
		deliveryHours.add(hrs);

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
		for (DeliveryHours d_hours : deliveryHours2) {
			r_dto.setDeliveryHoursFrom1((d_hours.getFrom().toString()));
			r_dto.setDeliveryHoursTo1(d_hours.getTo().toString());
			r_dto.setDeliveryHoursFrom2(d_hours.getFrom().toString());
			r_dto.setDeliveryHoursTo2(d_hours.getTo().toString());
		}

		r_dto.setCalculateTaxOnPacking(entity.getCalculateTaxOnPacking());
		r_dto.setDcTaxesId(entity.getTax().getDcTaxesId());
		r_dto.setPcTaxesId(entity.getTax().getPcTaxesId());

		return r_dto;
	}

	@Override
	public List<RestaurantDto> getDtoList(List<Restaurant> entities) {
		List<RestaurantDto> dtoList = new ArrayList<>();

		for (Restaurant entity : entities) {
			RestaurantDto dto = getDto(entity);

			dtoList.add(dto);
		}

		return dtoList;
	}

	@Override
	public Restaurant getPatchDto(Restaurant existingEntity, RestaurantDto dto) {
		return null;
	}

}
