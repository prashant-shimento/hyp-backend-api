package com.hyp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.constants.Constants;
import com.hyp.entity.*;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.PartnerType;
import com.hyp.enums.RiderStatusType;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.exception.PosException;
import com.hyp.exception.RequestTranslationException;
import com.hyp.model.DeliveryOrderStatus.Rider;
import com.hyp.model.PosData;
import com.hyp.observability.ApplicationMetrics;
import com.hyp.observability.MetricTag;
import com.hyp.observability.MetricsEvent;
import com.hyp.request.FileUploadRequest;
import com.hyp.request.PosDataRequest;
import com.hyp.request.PosDataRequest.ItemRequest;
import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest.RiderDetails;
import com.hyp.request.PosStatusRequest;
import com.hyp.request.PosStockRequest;
import com.hyp.temporal.service.RestaurantWorkflowService;
import com.hyp.temporal.service.StockWorkflowService;
import com.hyp.translation.PosDataRequestTranslation;
import com.hyp.translation.PosOrderRequestTranslation;
import com.hyp.util.CommonUtils;
import io.micrometer.core.instrument.Timer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
public abstract class PosServiceImpl implements PosService {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RedisService redisService;

    @Autowired
    CustomerService customerService;

    @Autowired
    NotificationService notificationService;

    @Autowired
    PartnerService partnerService;

    @Autowired
    StockWorkflowService stockWorkflowService;

    @Autowired
    RestaurantWorkflowService restaurantWorkflowService;

    @Autowired
    OneSignalAlertService oneSignalAlertService;

    private final ExecutorService executorService = Executors.newFixedThreadPool(8);

    protected final RestaurantService restaurantService;
    protected final TaxService taxService;
    protected final OrderTypeService orderTypeService;
    protected final AttributeService attributeService;
    protected final DiscountService discountService;
    protected final VariationService variationService;
    protected final AddonItemService addonItemService;
    protected final AddonGroupService addonGroupService;
    protected final CategoryService categoryService;
    protected final ItemService itemService;
    protected final BucketService bucketService;

    @Autowired
    PosOrderRequestTranslation posOrderRequestTranslation;

    @Autowired
    ApplicationMetrics metrics;

    public PosServiceImpl(
            AttributeService attributeService,
            CategoryService categoryService,
            TaxService taxService,
            OrderTypeService orderTypeService,
            VariationService variationService,
            RestaurantService restaurantService,
            DiscountService discountService,
            AddonGroupService addonGroupService,
            AddonItemService addonItemService,
            ItemService itemService,
            BucketService bucketService) {
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
    public abstract void processPosOrder(Order order);

    @Override
    public abstract String updatePosRiderStatus(PosRiderUpdateRequest posRiderUpdateRequest);

    protected abstract String posBaseUrl();

    @Override
    @Transactional
    public boolean savePosData(PosDataRequest posDataRequest) {
        Timer.Sample timerSample = metrics.startTimer();
        String restaurantId = posDataRequest.getRestaurants().get(0).getRestaurantid();
        try {
            long existingRestQuery = System.currentTimeMillis();
            Restaurant existingRestaurant = restaurantService.findById(restaurantId);
            log.info("Time taken for existingRestQuery: {} ms", System.currentTimeMillis() - existingRestQuery);
            if (existingRestaurant != null) {
                deletePosData(existingRestaurant.getId());
            }
            long restaurantTranslation = System.currentTimeMillis();
            Restaurant restaurant = PosDataRequestTranslation.translateToRestaurant(
                    posDataRequest.getRestaurants().get(0), existingRestaurant);
            log.info("Time taken for restaurantTranslation: {} ms", System.currentTimeMillis() - restaurantTranslation);
            long restaurantSave = System.currentTimeMillis();
            restaurantService.save(restaurant);
            log.info("Time taken for restaurantSave: {} ms", System.currentTimeMillis() - restaurantSave);
            List<String> requestItemIds = posDataRequest.getItems().stream()
                    .map(ItemRequest::getItemid)
                    .collect(Collectors.toList());
            List<Item> existingItems = itemService.findAllByIdIn(requestItemIds);
            List<String> requestTaxIds = posDataRequest.getTaxes().stream()
                    .map(PosDataRequest.TaxRequest::getTaxid)
                    .toList();
            List<Tax> existingTax = taxService.findAllByIdIn(requestTaxIds);
            long posDataTranslation = System.currentTimeMillis();
            PosData posData = PosDataRequestTranslation.getPosData(posDataRequest, existingItems, existingTax);
            log.info("Time taken for posDataTranslation: {} ms", System.currentTimeMillis() - posDataTranslation);
            saveEntities(restaurant, posData);

            List<String> parameters = CommonUtils.buildStringList(
                    posDataRequest.getRestaurants().get(0).getDetails().getRestaurantname(),
                    posDataRequest.getRestaurants().get(0).getRestaurantid(),
                    posDataRequest.getRestaurants().get(0).getDetails().getMenusharingcode());

            notificationService.sendInternalGroupNotification(Constants.META_MENU_PUSH_ALERT_TEMPLATE, parameters);
            oneSignalAlertService.notifyMenuPush(
                    posDataRequest.getRestaurants().get(0).getDetails().getRestaurantname(),
                    posDataRequest.getRestaurants().get(0).getRestaurantid(),
                    posDataRequest.getRestaurants().get(0).getDetails().getMenusharingcode());

            metrics.count(
                    MetricsEvent.POS,
                    MetricTag.PARTNER,
                    Constants.PET_POOJA,
                    MetricTag.ACTION,
                    "menu_push",
                    MetricTag.RESULT,
                    "success");

            metrics.stopTimer(
                    timerSample,
                    MetricsEvent.POS,
                    MetricTag.PARTNER,
                    Constants.PET_POOJA,
                    MetricTag.ACTION,
                    "menu_push");
            return true;
        } catch (Exception e) {
            log.error("Exception occurred while saving POS data: {}", e.getMessage(), e);
            metrics.count(
                    MetricsEvent.POS,
                    MetricTag.PARTNER,
                    Constants.PET_POOJA,
                    MetricTag.ACTION,
                    "menu_push",
                    MetricTag.RESULT,
                    "failed");
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

            CompletableFuture<Void> attributeFuture = logEntityInsert(
                    "attributes", () -> attributeService.saveAll(posData.getAttributes(), restaurant.getId()));

            CompletableFuture<Void> discountFuture = logEntityInsert(
                    "discounts", () -> discountService.saveAll(posData.getDiscounts(), restaurant.getId()));

            CompletableFuture<Void> categoryFuture = logEntityInsert(
                    "categories", () -> categoryService.saveAll(posData.getCategories(), restaurant.getId()));

            CompletableFuture<Void> taxFuture =
                    logEntityInsert("taxes", () -> taxService.saveAll(posData.getTaxes(), restaurant.getId()));

            CompletableFuture<Void> variationFuture = logEntityInsert(
                    "variations", () -> variationService.saveAll(posData.getVariations(), restaurant.getId()));

            CompletableFuture<Void> addonItemFuture = logEntityInsert(
                    "addon items", () -> addonItemService.saveAll(posData.getAddonItems(), restaurant.getId()));

            CompletableFuture<Void> addonGroupFuture = logEntityInsert(
                    "addon groups", () -> addonGroupService.saveAll(posData.getAddonGroups(), restaurant.getId()));

            CompletableFuture<List<Item>> itemsFuture =
                    logItemInsert("items", () -> itemService.saveAll(posData.getItems(), restaurant.getId()));

            itemsFuture.thenAccept(items -> {
                try {
                    uploadImagesAsync(items, restaurant.getId());
                } catch (Exception e) {
                    log.error("Error occurred during image upload: {}", e.getMessage(), e);
                }
            });

            CompletableFuture.allOf(
                            orderTypeFuture,
                            attributeFuture,
                            discountFuture,
                            categoryFuture,
                            taxFuture,
                            variationFuture,
                            addonItemFuture,
                            addonGroupFuture)
                    .thenRun(() ->
                            log.info("Total time for saveEntities: {} ms", System.currentTimeMillis() - startTime));

        } catch (Exception e) {
            log.error("Error occurred during saveEntities for restaurant {}: {}", restaurant.getId(), e.getMessage());
        }
    }

    @Override
    public void createPosOrder(PosOrderRequest posOrderRequest) throws PosException {
        try {
            log.info("createPosOrder Request {}", objectMapper.writeValueAsString(posOrderRequest));
            WebClient webClient = WebClient.builder().baseUrl(posBaseUrl()).build();
            String endpoint = "/save_order";
            String response = webClient
                    .post()
                    .uri(endpoint)
                    .body(BodyInserters.fromValue(posOrderRequest))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("createPosOrder Response {}", objectMapper.writeValueAsString(response));

            metrics.count(
                    MetricsEvent.POS,
                    MetricTag.PARTNER,
                    Constants.PET_POOJA,
                    MetricTag.ACTION,
                    "create",
                    MetricTag.RESULT,
                    "success");

        } catch (Exception e) {
            log.error("Error occurred during createPosOrder {}", e.getMessage());
            metrics.count(
                    MetricsEvent.POS,
                    MetricTag.PARTNER,
                    Constants.PET_POOJA,
                    MetricTag.ACTION,
                    "create",
                    MetricTag.RESULT,
                    "failed");
            throw new PosException("POS Order Creation failed " + e.getMessage());
        }
    }

    @Override
    public void updatePosOrder(PosOrderUpdateRequest posOrderUpdateRequest) throws PosException {
        try {
            log.info("updatePosOrder Request {}", objectMapper.writeValueAsString(posOrderUpdateRequest));
            WebClient webClient = WebClient.builder().baseUrl(posBaseUrl()).build();
            String endpoint = "/update_order_status";
            String updateOrderResponse = webClient
                    .post()
                    .uri(endpoint)
                    .body(BodyInserters.fromValue(posOrderUpdateRequest))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("updatePosOrder Response {}", objectMapper.writeValueAsString(updateOrderResponse));
        } catch (Exception e) {
            log.error("Error occurred during updatePosOrder {}", e.getMessage());
            throw new PosException("POS Order Update failed " + e.getMessage());
        }
    }

    @Override
    public boolean updateStock(PosStockRequest stockRequest) {
        long startTime = System.currentTimeMillis();
        try {
            LocalDateTime autoTurnOnTime = null;
            if (!stockRequest.isInStock()) {
                String turnOnTime = stockRequest.getAutoTurnOnTime().equalsIgnoreCase("custom")
                        ? stockRequest.getCustomTurnOnTime()
                        : stockRequest.getAutoTurnOnTime();
                autoTurnOnTime = CommonUtils.parseAutoTurnOnTime(turnOnTime);
                autoTurnOnTime = CommonUtils.convertISTtoUTC(autoTurnOnTime);
            }
            long ttl = autoTurnOnTime != null ? CommonUtils.calculateTTLInSeconds(autoTurnOnTime) : 0;

            boolean isWorkflowEnabled = redisService
                    .getRedisData(Constants.STOCK_WORKFLOW_ENABLED)
                    .map(Boolean::parseBoolean)
                    .orElse(false);
            log.info("Stock workflow enabled status: {} and ttl {}", isWorkflowEnabled, ttl);
            if (isWorkflowEnabled && ttl > 0) {
                stockWorkflowService.startStockUpdateWorkflow(stockRequest, ttl);
            }

            String type = Optional.ofNullable(stockRequest.getType()).orElse("").toLowerCase();
            if ("item".equals(type)) {
                long updateItemStock = System.currentTimeMillis();
                updateItemStock(stockRequest, autoTurnOnTime);
                log.info("Total updateItemStock execution time: {} ms", System.currentTimeMillis() - updateItemStock);
            } else if ("addon".equals(type)) {
                long addonItemStockUpdate = System.currentTimeMillis();
                updateAddonItemStock(stockRequest, autoTurnOnTime);
                log.info(
                        "Total updateAddonItemStock execution time: {} ms",
                        System.currentTimeMillis() - addonItemStockUpdate);
            } else {
                log.warn("Unknown stockRequest type '{}'. Skipping stock update.", stockRequest.getType());
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

    public void updateItemStock(PosStockRequest stockRequest, LocalDateTime autoTurnOn) {
        long findByIdsStart = System.currentTimeMillis();
        List<Item> items = itemService.findByIds(stockRequest.getItemId());
        log.info("Fetched items in {} ms", System.currentTimeMillis() - findByIdsStart);

        if (items.isEmpty()) {
            log.info("No items found. Checking variations...");
            updateVariationStock(stockRequest, autoTurnOn);
            return;
        }

        items.forEach(item -> {
            item.setActive(stockRequest.isInStock() ? "1" : "0");
            item.setAutoTurnOnTime(stockRequest.isInStock() ? null : autoTurnOn);
        });
        long bulkWriteStart = System.currentTimeMillis();
        itemService.bulkUpdate(items, Item.class);
        log.info("Bulk write items completed in {} ms", System.currentTimeMillis() - bulkWriteStart);
    }

    public void updateVariationStock(PosStockRequest stockRequest, LocalDateTime autoTurnOn) {
        long findByVariationsStart = System.currentTimeMillis();
        List<Variation> variations = variationService.findByIds(stockRequest.getItemId());
        log.info("Fetched variations in {} ms", System.currentTimeMillis() - findByVariationsStart);

        if (variations.isEmpty()) {
            log.warn("No items or variations found for given IDs: {}", stockRequest.getItemId());
            return;
        }

        variations.forEach(variation -> {
            variation.setActive(stockRequest.isInStock() ? "1" : "0");
            variation.setAutoTurnOnTime(stockRequest.isInStock() ? null : autoTurnOn);
        });

        long bulkWriteStart = System.currentTimeMillis();
        variationService.bulkUpdate(variations, Variation.class);
        log.info("Bulk write for variations completed in {} ms", System.currentTimeMillis() - bulkWriteStart);
    }

    private void updateAddonItemStock(PosStockRequest stockRequest, LocalDateTime autoTurnOn) {
        long findByIdsStart = System.currentTimeMillis();
        List<AddonItem> addonItems = addonItemService.findByIds(stockRequest.getItemId());
        log.info("Fetched addons in {} ms", System.currentTimeMillis() - findByIdsStart);

        addonItems.forEach(addonItem -> {
            addonItem.setActive(stockRequest.isInStock() ? "1" : "0");
            addonItem.setAutoTurnOnTime(stockRequest.isInStock() ? null : autoTurnOn);
        });
        long bulkWriteStart = System.currentTimeMillis();
        addonItemService.bulkUpdate(addonItems, AddonItem.class);
        log.info("Bulk write addons completed in {} ms", System.currentTimeMillis() - bulkWriteStart);
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
            riderStatus = RiderStatusType.getRiderStatusByDeliveryRider(
                    delivery.getFulfillment().getStatus());
        } else {
            riderStatus = RiderStatusType.getRiderStatusByOrderStatusType(order.getStatus());
        }
        PosRiderUpdateRequest posRiderUpdateRequest = posOrderRequestTranslation.getPosRiderStatusUpdateRequest(
                restaurant, order, riderDetails, riderStatus.getValue());

        this.updatePosRiderStatus(posRiderUpdateRequest);
    }

    @Override
    public void updateRestaurant(PosStatusRequest updateStatus) {
        try {
            Restaurant restaurant = null;
            if (updateStatus.getMenuSharingCode() != null) {
                restaurant = restaurantService.findByMenuSharingCode(updateStatus.getMenuSharingCode());
            }
            if (updateStatus.getRestaurantId() != null) {
                restaurant = restaurantService.findById(updateStatus.getRestaurantId());
            }

            Optional.ofNullable(restaurant)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Restaurant",
                            updateStatus.getRestaurantId() != null
                                    ? updateStatus.getRestaurantId()
                                    : updateStatus.getMenuSharingCode()));

            boolean status = "1".equalsIgnoreCase(updateStatus.getStoreStatus());
            restaurant.setActive(status);

            if (!status
                    && updateStatus.getTurnOnTime() != null
                    && !updateStatus.getTurnOnTime().isEmpty()) {
                LocalDateTime turnOnTime = CommonUtils.parseAutoTurnOnTime(updateStatus.getTurnOnTime());
                turnOnTime = CommonUtils.convertISTtoUTC(turnOnTime);
                long ttl = turnOnTime != null ? CommonUtils.calculateTTLInSeconds(turnOnTime) : 0;

                log.info(
                        "Scheduling restaurant {} to auto-turn-on in {} seconds at {}",
                        restaurant.getId(),
                        ttl,
                        turnOnTime);
                if (ttl > 0) {
                    restaurantWorkflowService.startRestaurantStatusWorkflow(restaurant.getId(), ttl);
                }
            }
            restaurant.setStatusReason(updateStatus.getReason());
            restaurantService.update(restaurant);

            log.info(
                    "Updated restaurant {} active={}, reason={}", restaurant.getId(), status, updateStatus.getReason());
        } catch (Exception e) {
            log.error(
                    "Exception occurred while updating restaurant {}: {}",
                    updateStatus.getRestaurantId(),
                    e.getMessage(),
                    e);
        }
    }

    private void uploadImagesAsync(List<Item> items, String restaurantId) {
        List<CompletableFuture<Void>> uploadFutures = new ArrayList<>();

        items.forEach(item -> {
            if (item.getItemImageUrl() != null && !item.getItemImageUrl().isEmpty()) {
                CompletableFuture<Void> uploadFuture = CompletableFuture.runAsync(() -> {
                    try {
                        String uploadedImageUrl = bucketService.uploadItemImageFile(FileUploadRequest.builder()
                                .fileName(String.join("-", item.getId(), item.getItemName()))
                                .folderName(restaurantId)
                                .fileUrl(item.getItemImageUrl())
                                .build());

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
        return CompletableFuture.runAsync(
                () -> {
                    long start = System.currentTimeMillis();
                    action.run();
                    log.info(
                            "Time taken for logEntityInsert {}: {} ms",
                            entityName,
                            (System.currentTimeMillis() - start));
                },
                executorService);
    }

    private <T> CompletableFuture<T> logItemInsert(String label, Supplier<T> supplier) {
        long startTime = System.currentTimeMillis();
        return CompletableFuture.supplyAsync(() -> {
            T result = supplier.get();
            log.info("Time taken for logItemInsert {}: {} ms", label, (System.currentTimeMillis() - startTime));
            return result;
        });
    }

    public void sendAlert(PosException e) {
        notificationService.sendInternalGroupNotification(
                Constants.META_GENERIC_ALERT_TEMPLATE, List.of(e.getAction(), e.getMessage(), "POS"));
        oneSignalAlertService.notifyErrorResponseAlert(e.getAction(), e.getMessage(), "POS");
    }
}
