package com.hyp.service;

import com.hyp.adapter.urbanpiper.order.UrbanPiperOrderRequest;
import com.hyp.adapter.urbanpiper.order.UrbanPiperOrderTransformer;
import com.hyp.client.UrbanPiperClient;
import com.hyp.entity.Customer;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.request.PosRiderUpdateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service("urbanPiper")
public class UrbanPiperPosServiceImpl extends PosServiceImpl {

    @Value("${pos.urbanpiper.url:https://api.urbanpiper.com/external/api/v1/}")
    private String upBaseUrl;

    @Value("${pos.urbanpiper.username:}")
    private String upUsername;

    @Value("${pos.urbanpiper.apikey:}")
    private String upApiKey;

    @Autowired
    private UrbanPiperOrderTransformer urbanPiperOrderTransformer;

    @Autowired
    private UrbanPiperClient urbanPiperClient;

    public UrbanPiperPosServiceImpl(
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
        super(attributeService, categoryService, taxService, orderTypeService, variationService,
                restaurantService, discountService, addonGroupService, addonItemService, itemService, bucketService);
    }

    @Override
    protected String posBaseUrl() {
        return upBaseUrl;
    }

    @Override
    public void processPosOrder(Order order) {
        Customer customer = customerService.findById(order.getCustomerId());
        Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
        try {
            log.info("=== UrbanPiper Order Processing ===");
            log.info("Order ID: {}", order.getId());
            log.info("Customer ID: {}, Customer Name: {}", customer.getId(), customer.getName());
            log.info("Restaurant ID: {}, Restaurant Name: {}", restaurant.getId(), restaurant.getRestaurantName());

            UrbanPiperOrderRequest urbanPiperRequest = urbanPiperOrderTransformer.transform(order, customer, restaurant);
            log.info("Transformed UrbanPiper Order Request: {}", objectMapper.writeValueAsString(urbanPiperRequest));

            urbanPiperClient.createOrder(urbanPiperRequest);

            log.info("UrbanPiper order created successfully for order: {}", order.getId());
            log.info("===================================");
        } catch (Exception e) {
            log.error("Error processing UrbanPiper order {}: {}", order.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to process UrbanPiper order: " + e.getMessage(), e);
        }
    }

    @Override
    public String updatePosRiderStatus(PosRiderUpdateRequest request) {
        try {
            String externalOrderId = request.getExternalOrderId();
            if (externalOrderId == null || externalOrderId.isEmpty() || "0".equals(externalOrderId)) {
                externalOrderId = request.getOrderId();
            }

            Map<String, Object> payload = new HashMap<>();
            if (request.getRiderData() != null) {
                payload.put("rider_name", request.getRiderData().getRiderName());
                payload.put("rider_phone", request.getRiderData().getRiderContact());
            }

            String status = request.getStatus();
            String upStatus = switch (status) {
                case "rider-assigned", "rider_assigned" -> "rider_assigned";
                case "rider-arrived", "rider_arrived" -> "rider_arrived";
                case "pickedup", "picked_up" -> "picked_up";
                case "delivered" -> "delivered";
                default -> status;
            };
            payload.put("status", upStatus);

            log.info("UrbanPiper Rider Status Update Payload: {}", objectMapper.writeValueAsString(payload));
            return urbanPiperClient.updateRiderStatus(externalOrderId, payload);
        } catch (Exception e) {
            log.error("Error updating UrbanPiper rider status: {}", e.getMessage());
            return null;
        }
    }
}
