package com.hyp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.entity.Restaurant;
import com.hyp.model.PosData;
import com.hyp.request.PosDataRequest;
import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.translation.PosDataRequestTranslation;

import reactor.core.publisher.Mono;

@Service
public class PosServiceImpl implements PosService {

	@Value("${pos.petpooja.url}")
	private String baseUrl;

	private final RestaurantService restaurantService;
	private final TaxService taxService;
	private final OrderTypeService orderTypeService;
	private final AttributeService attributeService;
	private final DiscountService discountService;
	private final VariationService variationService;
	private final AddonItemService addonItemService;
	private final AddonGroupService addonGroupService;
	private final CategoryService categoryService;
	private final ItemService itemService;

	public PosServiceImpl(AttributeService attributeService, CategoryService categoryService,
			TaxService taxService, OrderTypeService orderTypeService, VariationService variationService,
			RestaurantService restaurantService, DiscountService discountService,
			AddonGroupService addonGroupService, AddonItemService addonItemService, ItemService itemService) {
		this.attributeService = attributeService;
		this.categoryService = categoryService;
		this.taxService = taxService;
		this.orderTypeService = orderTypeService;
		this.variationService = variationService;
		this.restaurantService = restaurantService;
		this.discountService = discountService;
		this.addonGroupService = addonGroupService;
		this.addonItemService = addonItemService;
		this.itemService = itemService;
	}

	@Override
	@Transactional
	public boolean savePosData(PosDataRequest posDataRequest) {
		try {

			PosData posData = PosDataRequestTranslation.getPosData(posDataRequest);
			Restaurant restaurant = restaurantService.save(posData.getRestaurant());
			saveEntities(restaurant, posData);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void saveEntities(Restaurant restaurant, PosData posData) {
		try {
			orderTypeService.saveAll(posData.getOrderTypes(), restaurant.getId());
			attributeService.saveAll(posData.getAttributes(), restaurant.getId());
			discountService.saveAll(posData.getDiscounts(), restaurant.getId());
			categoryService.saveAll(posData.getCategories(), restaurant.getId());
			taxService.saveAll(posData.getTaxes(), restaurant.getId());
			variationService.saveAll(posData.getVariations(), restaurant.getId());
			addonItemService.saveAll(posData.getAddonItems(), restaurant.getId());
			addonGroupService.saveAll(posData.getAddonGroups(), restaurant.getId());
			itemService.saveAll(posData.getItems(), restaurant.getId());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public String createOrder(PosOrderRequest posOrderRequest) {
		try {
			System.out.println("Request "+new ObjectMapper().writeValueAsString(posOrderRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
			String endpoint = "/save_order";
			Mono<String> saveOrderResponse = webClient.post().uri(endpoint).body(BodyInserters.fromValue(posOrderRequest))
					.retrieve().bodyToMono(String.class);
			saveOrderResponse.subscribe(response -> {
				System.out.println("Response: " + response);
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
			});
			return saveOrderResponse.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}

	@Override
	public String updateOrder(PosOrderUpdateRequest posOrderUpdateRequest) {
		try {
			System.out.println("Request "+new ObjectMapper().writeValueAsString(posOrderUpdateRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
			String endpoint = "/update_order_status";
			Mono<String> updateOrderResponse = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(posOrderUpdateRequest)).retrieve().bodyToMono(String.class);
			updateOrderResponse.subscribe(response -> {
				System.out.println("Response: " + response);
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
			});
			return updateOrderResponse.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

	}

	@Override
	public String updateRiderStatus(PosRiderUpdateRequest posRiderUpdateRequest) {
		try {
			System.out.println("Request "+new ObjectMapper().writeValueAsString(posRiderUpdateRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
			String endpoint = "/rider_status_update";
			Mono<String> riderUpdateResponse = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(posRiderUpdateRequest)).retrieve().bodyToMono(String.class);
			riderUpdateResponse.subscribe(response -> {
				System.out.println("Response: " + response);
			}, error -> {
				System.err.println("Error response: " + error.getMessage());
			});
			return riderUpdateResponse.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
