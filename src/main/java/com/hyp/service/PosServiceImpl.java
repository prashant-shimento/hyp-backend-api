package com.hyp.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.hyp.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.constants.Constants;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.PartnerType;
import com.hyp.enums.RiderStatusType;
import com.hyp.exception.PosException;
import com.hyp.exception.RequestTranslationException;
import com.hyp.model.DeliveryOrderStatus.Rider;
import com.hyp.model.PosData;
import com.hyp.request.FileUploadRequest;
import com.hyp.request.PosDataRequest;
import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest.RiderDetails;
import com.hyp.request.PosStatusRequest;
import com.hyp.request.PosStockRequest;
import com.hyp.translation.PosDataRequestTranslation;
import com.hyp.translation.PosOrderRequestTranslation;
import com.hyp.util.CommonUtils;

import lombok.extern.slf4j.Slf4j;

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
	MongoTemplate mongoTemplate;

	@Autowired
	RedisService redisService;

	@Autowired
	CustomerService customerService;

	@Autowired
	NotificationService notificationService;

	@Autowired
	PartnerService partnerService;

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
	public void processPosOrder(Order order) {
		Customer customer = customerService.findById(order.getCustomerId());
		Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
		try {
			PosOrderRequest posOrderRequest = posOrderRequestTranslation.getPosOrderRequest(restaurant, order,
					customer);
			if (partnerService.findPartnersByRestaurantId(order.getRestaurantId(), PartnerType.THEATRE) != null) {
				String name = String.format("%s-%s-%s", Optional.ofNullable(order.getScreen()).orElse("N/A"),
						Optional.ofNullable(order.getSeat()).orElse("N/A"),customer.getName());
				posOrderRequest.getOrderInfo().getOrderInfoDetails().getCustomer().getCustomerDetails().setName(name);
			}
			createPosOrder(posOrderRequest);
		} catch (RequestTranslationException e) {
			log.error("Error occurred on RequestTranslationException for order {} cause: {}", order.getId(),
					e.getMessage());
		} catch (PosException e) {
			log.error("Error occurred on PosException for order {} cause: {}", order.getId(), e.getMessage());
		}
	}

	@Override
	@Transactional
	public boolean savePosData(PosDataRequest posDataRequest) {
		try {
			long existingRestQuery = System.currentTimeMillis();
			String existingRestaurantId = posDataRequest.getRestaurants().get(0).getRestaurantid();
			Restaurant existingRestaurant = restaurantService
					.findById(existingRestaurantId);
			log.info("Time taken for existingRestQuery: {} ms", System.currentTimeMillis() - existingRestQuery);
			if (existingRestaurant != null) {
				deletePosData(existingRestaurant.getId());
			}
			long restaurantTranslation = System.currentTimeMillis();
			Restaurant restaurant = PosDataRequestTranslation
					.translateToRestaurant(posDataRequest.getRestaurants().get(0), existingRestaurant);
            log.info("Time taken for restaurantTranslation: {} ms", System.currentTimeMillis() - restaurantTranslation);
			long restaurantSave = System.currentTimeMillis();
			restaurantService.save(restaurant);
            log.info("Time taken for restaurantSave: {} ms", System.currentTimeMillis() - restaurantSave);
			long posDataTranslation = System.currentTimeMillis();
			PosData posData = PosDataRequestTranslation.getPosData(posDataRequest);
			log.info("Time taken for posDataTranslation: {} ms", System.currentTimeMillis() - posDataTranslation);
			saveEntities(restaurant, posData);

			List<String> parameters = CommonUtils.buildStringList(
					posDataRequest.getRestaurants().get(0).getDetails().getRestaurantname(),
					posDataRequest.getRestaurants().get(0).getRestaurantid(),
					posDataRequest.getRestaurants().get(0).getDetails().getMenusharingcode());

			notificationService.sendInternalGroupNotification(Constants.META_MENU_PUSH_ALERT_TEMPLATE, parameters);

			return true;
		} catch (Exception e) {
			log.error("Exception occurred while saving POS data: {}", e.getMessage(), e);
			PosException posException = new PosException("Error in savePosData API call: " + e.getMessage(), e);
			sendAlert(posException);
			return false;
		}
	}

	@Transactional
	public void deletePosData(String restaurantId) {
		try {
			long itemDelete = System.currentTimeMillis();
			itemService.softDeleteByRestaurant(Item.class, restaurantId);
			log.info("Time taken for itemDelete: {} ms", System.currentTimeMillis() - itemDelete);
			long categoryDelete = System.currentTimeMillis();
			categoryService.softDeleteByRestaurant(Category.class, restaurantId);
			log.info("Time taken for categoryDelete: {} ms", System.currentTimeMillis() - categoryDelete);
			long orderTypeDelete = System.currentTimeMillis();
			orderTypeService.deleteAll(OrderType.class);
			log.info("Time taken for orderTypeDelete: {} ms", System.currentTimeMillis() - orderTypeDelete);


		} catch (Exception e) {
			log.error("Exception occurred in deletePosData {}", e.getMessage());
		}
	}

	private void saveEntities(Restaurant restaurant, PosData posData) {
		long startTime = System.currentTimeMillis();

		try {
			CompletableFuture<Void> orderTypeFuture = logEntityInsert("order types", () -> {
				posData.getOrderTypes().forEach(orderType -> orderType.setRestaurantId(restaurant.getId()));
				orderTypeService.saveAll(posData.getOrderTypes());
			});

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

			CompletableFuture
					.allOf(orderTypeFuture, attributeFuture, discountFuture, categoryFuture, taxFuture, variationFuture,
							addonItemFuture, addonGroupFuture)
					.thenRun(() -> log.info("Total time for saveEntities: {} ms",
							System.currentTimeMillis() - startTime));

		} catch (Exception e) {
			log.error("Error occurred during saveEntities for restaurant {}: {}", restaurant.getId(), e.getMessage());
		}
	}

	private CompletableFuture<Void> saveAsync(Runnable task, String name) {
		return CompletableFuture.runAsync(() -> {
			long t = System.currentTimeMillis();
			try {
				task.run();
				log.info("Saved {} in {} ms", name, System.currentTimeMillis() - t);
			} catch (Exception e) {
				log.error("Failed to save {}: {}", name, e.getMessage(), e);
			}
		});
	}

	@Override
	public void createPosOrder(PosOrderRequest posOrderRequest) throws PosException {
		try {
			log.info("createPosOrder Request {}", objectMapper.writeValueAsString(posOrderRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
			String endpoint = "/save_order";
			String response = webClient.post().uri(endpoint).body(BodyInserters.fromValue(posOrderRequest)).retrieve()
					.bodyToMono(String.class).block();
			log.info("createPosOrder Response {}", objectMapper.writeValueAsString(response));

        } catch (Exception e) {
			log.error("Error occurred during createPosOrder {}", e.getMessage());
			throw new PosException("POS Order Creation failed " + e.getMessage());
		}
	}

	@Override
	public void updatePosOrder(PosOrderUpdateRequest posOrderUpdateRequest) throws PosException {
		try {
			log.info("updatePosOrder Request {}", objectMapper.writeValueAsString(posOrderUpdateRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
			String endpoint = "/update_order_status";
			String updateOrderResponse = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(posOrderUpdateRequest)).retrieve().bodyToMono(String.class).block();
			log.info("updatePosOrder Response {}", objectMapper.writeValueAsString(updateOrderResponse));
		} catch (Exception e) {
			log.error("Error occurred during updatePosOrder {}", e.getMessage());
			throw new PosException("POS Order Update failed " + e.getMessage());
		}
	}

	@Override
	public String updatePosRiderStatus(PosRiderUpdateRequest posRiderUpdateRequest) {
		try {
			log.info("updatePosRiderStatus Request {}", objectMapper.writeValueAsString(posRiderUpdateRequest));
			WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
			String endpoint = "/rider_status_update";
			String riderUpdateResponse = webClient.post().uri(endpoint)
					.body(BodyInserters.fromValue(posRiderUpdateRequest)).retrieve().bodyToMono(String.class).block();
			log.info("updatePosRiderStatus Response {}", objectMapper.writeValueAsString(riderUpdateResponse));
			return riderUpdateResponse;
		} catch (Exception e) {
			log.error("Error occurred during updatePosRiderStatus {}", e.getMessage());
			return null;
		}
	}

	@Override
	public boolean updateStock(PosStockRequest stockRequest) {
		long startTime = System.currentTimeMillis();
		try {
			final LocalDateTime autoTurnOnTime = (!stockRequest.isInStock()) ? parseAutoTurnOnTime(stockRequest) : null;
			long ttl = autoTurnOnTime != null ? calculateTTLInSeconds(autoTurnOnTime) : 0;

			if (stockRequest.getType().equalsIgnoreCase("item")) {
				long updateItemStock = System.currentTimeMillis();
				updateItemStock(stockRequest, autoTurnOnTime, ttl);
				log.info("Total updateItemStock execution time: {} ms", System.currentTimeMillis() - updateItemStock);
			} else {
				long addonItemStockUpdate = System.currentTimeMillis();
				updateAddonItemStock(stockRequest, autoTurnOnTime, ttl);
				log.info("Total updateAddonItemStock execution time: {} ms",
						System.currentTimeMillis() - addonItemStockUpdate);
			}
			return true;
		} catch (Exception e) {
			log.error("Exception occurred while updating stock: {}", e.getMessage(), e);
			PosException posException = new PosException("Error in updateStock API call: " + e.getMessage(), e);
			sendAlert(posException);
			return false;
		} finally {
			log.info("Total updateStock execution time: {} ms", System.currentTimeMillis() - startTime);
		}
	}

	public void updateItemStock(PosStockRequest stockRequest, LocalDateTime autoTurnOn, long ttl) {
		long findByIdsStart = System.currentTimeMillis();
		List<Item> items = itemService.findByIds(stockRequest.getItemId());
		log.info("Fetched items in {} ms", System.currentTimeMillis() - findByIdsStart);

		if (items.isEmpty()) {
			log.info("No items found. Checking variations...");
			updateVariationStock(stockRequest, autoTurnOn, ttl);
			return;
		}

		items.forEach(item -> {
			item.setActive(stockRequest.isInStock() ? "1" : "0");
			String redisKey = stockRequest.getType() + ":" + item.getId() + ":stock";
			if (!stockRequest.isInStock()) {
				item.setAutoTurnOnTime(autoTurnOn);
				if (ttl > 0) {
					redisService.setRedisData(redisKey, stockRequest, ttl);
				}
			} else {
				item.setAutoTurnOnTime(null);
				redisService.removeRedisData(redisKey);
			}
		});
		long bulkWriteStart = System.currentTimeMillis();
		itemService.bulkUpdate(items, Item.class);
		log.info("Bulk write items completed in {} ms", System.currentTimeMillis() - bulkWriteStart);
	}

	public void updateVariationStock(PosStockRequest stockRequest, LocalDateTime autoTurnOn, long ttl) {
		long findByVariationsStart = System.currentTimeMillis();
		List<Variation> variations = variationService.findByIds(stockRequest.getItemId());
		log.info("Fetched variations in {} ms", System.currentTimeMillis() - findByVariationsStart);

		if (variations.isEmpty()) {
			log.warn("No items or variations found for given IDs: {}", stockRequest.getItemId());
			return;
		}

		variations.forEach(variation -> {
			variation.setActive(stockRequest.isInStock() ? "1" : "0");
			String redisKey = "variation:" + variation.getId() + ":stock";
			if (!stockRequest.isInStock()) {
				variation.setAutoTurnOnTime(autoTurnOn);
				if (ttl > 0) {
					redisService.setRedisData(redisKey, stockRequest, ttl);
				}
			} else {
				variation.setAutoTurnOnTime(null);
				redisService.removeRedisData(redisKey);
			}
		});

		long bulkWriteStart = System.currentTimeMillis();
		variationService.bulkUpdate(variations, Variation.class);
		log.info("Bulk write for variations completed in {} ms", System.currentTimeMillis() - bulkWriteStart);
	}

	private void updateAddonItemStock(PosStockRequest stockRequest, LocalDateTime autoTurnOn, long ttl) {
		long findByIdsStart = System.currentTimeMillis();
		List<AddonItem> addonItems = addonItemService.findByIds(stockRequest.getItemId());
		log.info("Fetched addons in {} ms", System.currentTimeMillis() - findByIdsStart);

		addonItems.forEach(addonItem -> {
			addonItem.setActive(stockRequest.isInStock() ? "1" : "0");
			String redisKey = stockRequest.getType() + ":" + addonItem.getId() + ":stock";
			if (!stockRequest.isInStock()) {
				addonItem.setAutoTurnOnTime(autoTurnOn);
				if (ttl > 0) {
					redisService.setRedisData(redisKey, stockRequest, ttl);
				}
			} else {
				addonItem.setAutoTurnOnTime(null);
				redisService.removeRedisData(redisKey);
			}
		});
		long bulkWriteStart = System.currentTimeMillis();
		addonItemService.bulkUpdate(addonItems, AddonItem.class);
		log.info("Bulk write addons completed in {} ms", System.currentTimeMillis() - bulkWriteStart);
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
			log.error("Invalid TurnOnTime passed {}" ,turnOnTime);
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
			restaurant.setActive(updateStatus.getStoreStatus().equalsIgnoreCase("1"));
			if (!restaurant.isActive() && updateStatus.getTurnOnTime() != null
					&& !updateStatus.getTurnOnTime().isEmpty()) {
				restaurant.setTurnOnTime(LocalDateTime.parse(updateStatus.getTurnOnTime(),
						DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
			}
			restaurant.setStatusReason(updateStatus.getReason());
			restaurantService.update(restaurant);
			return true;
		} catch (Exception e) {
			log.error("Exception occurred in updateRestaurant {}", e.getMessage());
			return false;
		}
	}

	private void uploadImagesAsync(List<Item> items, String restaurantId) {
		List<CompletableFuture<Void>> uploadFutures = new ArrayList<>();

		items.forEach(item -> {
			if (item.getItemImageUrl() != null && !item.getItemImageUrl().isEmpty()) {
				CompletableFuture<Void> uploadFuture = CompletableFuture.runAsync(() -> {
					try {
						String uploadedImageUrl = bucketService.uploadItemImageFile(
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

//	private void sendNotification(String alertTemplate, PosStockRequest stockRequest) {
//		List<String> parameters = CommonUtils.buildStringList(stockRequest.getRestaurantId(), stockRequest.isInStock(),
//				stockRequest.getCustomTurnOnTime(), stockRequest.getMessage());
//		notificationService.sendInternalGroupNotification(alertTemplate, parameters);
//	}

	public void sendAlert(PosException e) {
		notificationService.sendInternalGroupNotification(Constants.META_GENERIC_ALERT_TEMPLATE,
				List.of(e.getAction(), e.getMessage(), "POS"));
	}

}
