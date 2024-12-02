package com.hyp.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
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
import com.hyp.model.DeliveryOrderStatus.Rider;
import com.hyp.model.PosData;
import com.hyp.request.FileUploadRequest;
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

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	RedisTemplate<String, Object> redisTemplate;

	private final ExecutorService executorService = Executors.newFixedThreadPool(8);

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
	private final BucketService bucketService;

	private static final DateTimeFormatter FORMATTER_WITH_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final DateTimeFormatter FORMATTER_WITHOUT_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	@Autowired
	PosOrderRequestTranslation posOrderRequestTranslation;

	public PosServiceImpl(AttributeService attributeService, CategoryService categoryService, TaxService taxService,
			OrderTypeService orderTypeService, VariationService variationService, RestaurantService restaurantService,
			DiscountService discountService, AddonGroupService addonGroupService, AddonItemService addonItemService,
			ItemService itemService, BucketService bucketService) {
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
		this.bucketService = bucketService;
	}

	@Override
	@Transactional
	public boolean savePosData(PosDataRequest posDataRequest) {
		try {
			long existingRestQuery = System.currentTimeMillis();
			Restaurant existingRestaurant = restaurantService
					.findById(posDataRequest.getRestaurants().get(0).getRestaurantid());
			log.info("Time taken for existingRestQuery: " + (System.currentTimeMillis() - existingRestQuery) + "ms");
			long restaurantTranslation = System.currentTimeMillis();
			Restaurant restaurant = PosDataRequestTranslation
					.translateToRestaurant(posDataRequest.getRestaurants().get(0), existingRestaurant);
			log.info("Time taken for restaurantTranslation: " + (System.currentTimeMillis() - restaurantTranslation)
					+ "ms");
			long posDataTranslation = System.currentTimeMillis();
			PosData posData = PosDataRequestTranslation.getPosData(posDataRequest);
			log.info("Time taken for posDataTranslation: " + (System.currentTimeMillis() - posDataTranslation) + "ms");
			long restaurantSave = System.currentTimeMillis();
			restaurantService.save(restaurant);
			log.info("Time taken for restaurantSave: " + (System.currentTimeMillis() - restaurantSave) + "ms");
			saveEntities(restaurant, posData);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void saveEntities(Restaurant restaurant, PosData posData) {
		long startTime = System.currentTimeMillis();

		try {
			CompletableFuture<Void> orderTypeFuture = logEntityInsert("order types",
					() -> orderTypeService.saveAll(posData.getOrderTypes(), restaurant.getId()));

			CompletableFuture<Void> attributeFuture = logEntityInsert("attributes",
					() -> attributeService.saveAll(posData.getAttributes(), restaurant.getId()));

			CompletableFuture<Void> discountFuture = logEntityInsert("discounts",
					() -> discountService.saveAll(posData.getDiscounts(), restaurant.getId()));

			CompletableFuture<Void> categoryFuture = logEntityInsert("categories",
					() -> categoryService.saveAll(posData.getCategories(), restaurant.getId()));

			CompletableFuture<Void> taxFuture = logEntityInsert("taxes",
					() -> taxService.saveAll(posData.getTaxes(), restaurant.getId()));

			CompletableFuture<Void> variationFuture = logEntityInsert("variations",
					() -> variationService.saveAll(posData.getVariations(), restaurant.getId()));

			CompletableFuture<Void> addonItemFuture = logEntityInsert("addon items",
					() -> addonItemService.saveAll(posData.getAddonItems(), restaurant.getId()));

			CompletableFuture<Void> addonGroupFuture = logEntityInsert("addon groups",
					() -> addonGroupService.saveAll(posData.getAddonGroups(), restaurant.getId()));

			CompletableFuture<List<Item>> itemsFuture = logItemInsert("items",
					() -> itemService.saveAll(posData.getItems(), restaurant.getId()));

			itemsFuture.thenAccept(items -> {
				try {
					uploadImagesAsync(items, restaurant.getId());
				} catch (Exception e) {
					log.error("Error occurred during image upload: {}", e.getMessage(), e);
				}
			});

			// Log time taken for each save operation
			CompletableFuture
					.allOf(orderTypeFuture, attributeFuture, discountFuture, categoryFuture, taxFuture, variationFuture,
							addonItemFuture, addonGroupFuture)
					.thenRun(() -> log.info("Total time for saveEntities: {} ms",
							System.currentTimeMillis() - startTime));

		} catch (Exception e) {
			log.error("Error occurred during saveEntities for restaurant {}: {}", restaurant.getId(), e);
		}
	}

	@Override
	public boolean createPosOrder(PosOrderRequest posOrderRequest) throws PosException {
		try {
			log.info("createPosOrder Request {}", objectMapper.writeValueAsString(posOrderRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
			String endpoint = "/save_order";

			String response = webClient.post().uri(endpoint).body(BodyInserters.fromValue(posOrderRequest)).retrieve()
					.bodyToMono(String.class).block();
			log.info("createPosOrder Response {}", objectMapper.writeValueAsString(response));
			if (response != null && !response.isEmpty()) {
				return true;
			}
		} catch (Exception e) {
			log.error("Error occured during createPosOrder {}", e);
			throw new PosException("POS Order Creation failed " + e.getMessage());
		}
		return false;
	}

	@Override
	public String updatePosOrder(PosOrderUpdateRequest posOrderUpdateRequest) throws PosException {
		try {
			log.info("updatePosOrder Request {}", objectMapper.writeValueAsString(posOrderUpdateRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
			String endpoint = "/update_order_status";
			String updateOrderResponse = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(posOrderUpdateRequest)).retrieve().bodyToMono(String.class).block();
			log.info("updatePosOrder Response {}", objectMapper.writeValueAsString(updateOrderResponse));
			return updateOrderResponse;
		} catch (Exception e) {
			log.error("Error occured during updatePosOrder {}", e);
			throw new PosException("POS Order Update failed " + e.getMessage());
		}
	}

	@Override
	public String updatePosRiderStatus(PosRiderUpdateRequest posRiderUpdateRequest) {
		try {
			log.info("updatePosOrder Request {}", objectMapper.writeValueAsString(posRiderUpdateRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
			String endpoint = "/rider_status_update";
			String riderUpdateResponse = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(posRiderUpdateRequest)).retrieve().bodyToMono(String.class).block();
			log.info("updatePosOrder Response {}", objectMapper.writeValueAsString(riderUpdateResponse));
			return riderUpdateResponse;
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

	private void setRedisData(String itemType, String id, long ttl, boolean inStock) {
		String redisKey = itemType + ":" + id + ":stock";
		redisTemplate.opsForValue().set(redisKey, inStock, Duration.ofSeconds(ttl));
	}

	private void removeRedisKey(String itemType, String id) {
		String redisKey = itemType + ":" + id + ":stock";
		redisTemplate.delete(redisKey);
	}

	private void updateItemStock(String id, PosStockRequest stockRequest) {
		Item item = itemService.findById(id);
		if (item != null) {
			item.setActive(stockRequest.isInStock() ? "1" : "0");
			if (!stockRequest.isInStock()) {
				LocalDateTime autoTurnOnTime = parseAutoTurnOnTime(stockRequest);
				item.setAutoTurnOnTime(autoTurnOnTime);
				long ttl = calculateTTLInSeconds(autoTurnOnTime);
				if (ttl > 0) {
					setRedisData(stockRequest.getType(), id, ttl, stockRequest.isInStock());
				}
				log.info("Calculated AutoTuronOnTime {} and TTL {} for Item {}", autoTurnOnTime, ttl, id);
			} else {
				removeRedisKey(stockRequest.getType(), id);
			}
			itemService.update(item);
		}
	}

	private void updateAddonItemStock(String id, PosStockRequest stockRequest) {
		AddonItem addOnItem = addonItemService.findById(id);
		if (addOnItem != null) {
			addOnItem.setActive(stockRequest.isInStock() ? "1" : "0");
			if (!stockRequest.isInStock()) {
				LocalDateTime autoTurnOnTime = parseAutoTurnOnTime(stockRequest);
				addOnItem.setAutoTurnOnTime(autoTurnOnTime);
				long ttl = calculateTTLInSeconds(autoTurnOnTime);
				if (ttl > 0) {
					setRedisData(stockRequest.getType(), id, ttl, stockRequest.isInStock());
				}
				log.info("Calculated AutoTuronOnTime {} and TTL {} for AddonItem {}", autoTurnOnTime, ttl, id);
			} else {
				removeRedisKey(stockRequest.getType(), id);
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
		} else if (turnOnTime.length() == 16) {
			return LocalDateTime.parse(turnOnTime, FORMATTER_WITHOUT_SECONDS);
		} else {
			log.error("Invalid TurnOnTime passed " + turnOnTime);
			return LocalDateTime.now().plusHours(2);
		}
	}

	private long calculateTTLInSeconds(LocalDateTime turnOnTime) {
		ZonedDateTime utcTurnOnTime = turnOnTime.atZone(ZoneId.of("Asia/Kolkata"))
				.withZoneSameInstant(ZoneId.of("UTC"));

		ZonedDateTime currentUtcTime = ZonedDateTime.now(ZoneId.of("UTC"));
		Duration duration = Duration.between(currentUtcTime, utcTurnOnTime);
		return Math.max(0, duration.getSeconds());
	}

	public boolean isPosUpdateRequired(DeliveryFulfillStatusType fullFillStatus) {
		return fullFillStatus == DeliveryFulfillStatusType.OUT_FOR_PICKUP
				|| fullFillStatus == DeliveryFulfillStatusType.REACHED_PICKUP
				|| fullFillStatus == DeliveryFulfillStatusType.PICKED_UP
				|| fullFillStatus == DeliveryFulfillStatusType.DELIVERED;
	}

	public void updatePosRiderStatus(Delivery delivery, Order order) {
		Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
		RiderDetails riderDetails = null;
		RiderStatusType riderStatus = null;
		if (delivery != null && delivery.getFulfillment() != null) {
			Rider rider = delivery.getFulfillment().getRider();
			if (rider != null) {
				riderDetails = new RiderDetails(rider.getName(), rider.getMobile());
			}
			riderStatus = RiderStatusType.getRiderStatusByDeliveryRider(delivery.getFulfillment().getStatus());
		} else {
			riderStatus = RiderStatusType.getRiderStatusByOrderStatusType(order.getStatus());
		}
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

	private void uploadImagesAsync(List<Item> items, String restaurantId) {
		List<CompletableFuture<Void>> uploadFutures = new ArrayList<>();

		items.forEach(item -> {
			if (item.getItemImageUrl() != null && !item.getItemImageUrl().isEmpty()) {
				CompletableFuture<Void> uploadFuture = CompletableFuture.runAsync(() -> {
					try {
						String uploadedImageUrl = bucketService.uploadFile(
								FileUploadRequest.builder().fileName(String.join("-", item.getId(), item.getItemName()))
										.folderName(restaurantId).fileUrl(item.getItemImageUrl()).build());

						if (uploadedImageUrl != null) {
							item.setItemImageUrl(uploadedImageUrl);
							log.info("Uploaded image URL for item {}: {}", item.getId(), uploadedImageUrl);
						} else {
							log.warn("Image upload failed for item: {}", item.getId());
						}
					} catch (Exception e) {
						log.error("Error during image upload for item {}: {}", item.getId(), e.getMessage());
					}
				});
				uploadFutures.add(uploadFuture);
			} else {
				log.info("Skipping upload for item {} as itemImageUrl is empty or null", item.getId());
			}

		});

		CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
			log.info("All image uploads completed.");
			itemService.saveAll(items);
		});
	}

	private CompletableFuture<Void> logEntityInsert(String entityName, Runnable action) {
		return CompletableFuture.runAsync(() -> {
			long start = System.currentTimeMillis();
			action.run();
			log.info("Time taken for saving {}: {} ms", entityName, (System.currentTimeMillis() - start));
		}, executorService);
	}

	private <T> CompletableFuture<T> logItemInsert(String label, Supplier<T> supplier) {
		long startTime = System.currentTimeMillis();
		return CompletableFuture.supplyAsync(() -> {
			T result = supplier.get();
			log.info("Time taken for saving {}: {} ms", label, (System.currentTimeMillis() - startTime));
			return result;
		});
	}

}
