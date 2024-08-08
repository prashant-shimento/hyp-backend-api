package com.hyp.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.entity.AddonItem;
import com.hyp.entity.Delivery;
import com.hyp.entity.Item;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.RiderStatusType;
import com.hyp.exception.PosException;
import com.hyp.model.PosData;
import com.hyp.request.PosDataRequest;
import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.request.PosStockRequest;
import com.hyp.request.PosRiderUpdateRequest.RiderDetails;
import com.hyp.request.PosStatusRequest;
import com.hyp.translation.PosDataRequestTranslation;
import com.hyp.translation.PosOrderRequestTranslation;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class PosServiceImpl implements PosService {

	@Value("${pos.petpooja.url}")
	private String baseUrl;

	@Autowired
	ApiLogService apiRequestResponseLogService;

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

	private static final DateTimeFormatter FORMATTER_WITH_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter FORMATTER_WITHOUT_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	@Autowired
	PosOrderRequestTranslation posOrderRequestTranslation;

	public PosServiceImpl(AttributeService attributeService, CategoryService categoryService, TaxService taxService,
			OrderTypeService orderTypeService, VariationService variationService, RestaurantService restaurantService,
			DiscountService discountService, AddonGroupService addonGroupService, AddonItemService addonItemService,
			ItemService itemService) {
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
			Restaurant existingRestaurant = restaurantService
					.findById(posDataRequest.getRestaurants().get(0).getRestaurantid());
			PosData posData = PosDataRequestTranslation.getPosData(posDataRequest);
			Restaurant restaurant = PosDataRequestTranslation
					.translateToRestaurant(posDataRequest.getRestaurants().get(0), existingRestaurant);
			restaurantService.save(restaurant);
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
	public boolean createPosOrder(PosOrderRequest posOrderRequest) throws PosException {
		try {
			System.out.println("Request " + new ObjectMapper().writeValueAsString(posOrderRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
			String endpoint = "/save_order";

			String response = webClient.post().uri(endpoint).body(BodyInserters.fromValue(posOrderRequest)).retrieve()
					.bodyToMono(String.class).block();

			if (response != null && !response.isEmpty()) {
				System.out.println("Response: " + response);
				return true; // Successful order creation
			} else {
				System.err.println("Error: Empty response received");
				return false; // Failed order creation
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new PosException("POS Order Creation failed " + e.getMessage());
		}
	}

	@Override
	public String updatePosOrder(PosOrderUpdateRequest posOrderUpdateRequest) throws PosException {
		try {
			System.out.println("Request " + new ObjectMapper().writeValueAsString(posOrderUpdateRequest));
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
			e.printStackTrace();
			throw new PosException("POS Order Update failed " + e.getMessage());
		}
	}

	@Override
	public String updatePosRiderStatus(PosRiderUpdateRequest posRiderUpdateRequest) {
		try {
			System.out.println("Request " + new ObjectMapper().writeValueAsString(posRiderUpdateRequest));
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
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean updateStock(PosStockRequest stockRequest) {
		try {
			for (String id : stockRequest.getItemId()) {
				if (stockRequest.getType().equalsIgnoreCase("item")) {
					updateItemStock(id, stockRequest);
				} else {
					updateAddonItemStock(id, stockRequest);
				}
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void updateItemStock(String id, PosStockRequest stockRequest) {
		Item item = itemService.findById(id);
		if (item != null) {
			item.setActive(stockRequest.isInStock() ? "1" : "0");
			if (!stockRequest.isInStock()) {
				item.setAutoTurnOnTime(parseAutoTurnOnTime(stockRequest));
			}
			itemService.update(item);
		}
	}

	private void updateAddonItemStock(String id, PosStockRequest stockRequest) {
		AddonItem addOnItem = addonItemService.findById(id);
		if (addOnItem != null) {
			addOnItem.setActive(stockRequest.isInStock() ? "1" : "0");
			if (!stockRequest.isInStock()) {
				addOnItem.setAutoTurnOnTime(parseAutoTurnOnTime(stockRequest));
			}
			addonItemService.update(addOnItem);
		}
	}

	private LocalDateTime parseAutoTurnOnTime(PosStockRequest stockRequest) {
		String turnOnTime = stockRequest.getAutoTurnOnTime().equalsIgnoreCase("custom")
				? stockRequest.getCustomTurnOnTime()
				: stockRequest.getAutoTurnOnTime();
		if (turnOnTime.length() == 19) {
			return LocalDateTime.parse(turnOnTime, FORMATTER_WITH_SECONDS);
		} else if (turnOnTime.length() == 19) {
			return LocalDateTime.parse(turnOnTime, FORMATTER_WITHOUT_SECONDS);
		} else {
			log.error("Invalid TurnOnTime passed " + turnOnTime);
			return LocalDateTime.now();
		}
	}

	public boolean isPosUpdateRequired(DeliveryFulfillStatusType fullFillStatus) {
		return fullFillStatus == DeliveryFulfillStatusType.OUT_FOR_PICKUP
				|| fullFillStatus == DeliveryFulfillStatusType.REACHED_PICKUP
				|| fullFillStatus == DeliveryFulfillStatusType.PICKED_UP
				|| fullFillStatus == DeliveryFulfillStatusType.DELIVERED;
	}

	public void updatePosRiderStatus(Delivery delivery, Order order) {
		Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
		RiderDetails riderDetails = new RiderDetails(delivery.getFulfillment().getRider().getName(),
				delivery.getFulfillment().getRider().getMobile());

		RiderStatusType riderStatus = RiderStatusType
				.getRiderStatusByDeliveryRider(delivery.getFulfillment().getStatus());

		PosRiderUpdateRequest posRiderUpdateRequest = posOrderRequestTranslation
				.getPosRiderStatusUpdateRequest(restaurant, order, riderDetails, riderStatus);

		this.updatePosRiderStatus(posRiderUpdateRequest);
	}

	@Override
	public boolean updateRestaurant(PosStatusRequest updateStatus) {
		try {
			Restaurant restaurant = restaurantService.findByMenuSharingCode(updateStatus.getRestaurantId());
			restaurant.setActive(updateStatus.getStoreStatus().equalsIgnoreCase("1") ? true : false);
			if (!restaurant.isActive() && updateStatus.getTurnOnTime() != null
					&& !updateStatus.getTurnOnTime().isEmpty()) {
				restaurant.setTurnOnTime(LocalDateTime.parse(updateStatus.getTurnOnTime(),
						DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
			}
			restaurant.setStatusReason(updateStatus.getReason());
			restaurantService.update(restaurant);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

}
