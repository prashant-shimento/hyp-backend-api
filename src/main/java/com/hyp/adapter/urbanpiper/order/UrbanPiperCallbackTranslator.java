package com.hyp.adapter.urbanpiper.order;

import com.hyp.enums.OrderStatusType;
import com.hyp.request.PosCallbackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrbanPiperCallbackTranslator {

    private final OrderStatusTransformer orderStatusTransformer;

    public PosCallbackRequest translateToPosCallback(
            UrbanPiperOrderStatusRequest urbanPiperRequest, String locationRefId) {
        log.info("Translating UrbanPiper order status request to PosCallbackRequest");

        PosCallbackRequest posCallbackRequest = new PosCallbackRequest();

        // Use external_order_id if available, otherwise use order_id
        String orderId = urbanPiperRequest.getExternalOrderId();
        if (orderId == null || orderId.isEmpty()) {
            orderId = urbanPiperRequest.getOrderId();
        }
        posCallbackRequest.setOrderId(orderId);

        // Set restaurant ID from location_ref_id
        posCallbackRequest.setRestaurantId(locationRefId);

        // Map UrbanPiper status to POS status code
        OrderStatusType orderStatus = orderStatusTransformer.mapUrbanPiperStatus(urbanPiperRequest.getStatus());
        posCallbackRequest.setStatus(mapOrderStatusToPosCode(orderStatus));

        // UrbanPiper doesn't provide prep/delivery times in status callback
        // These will be handled by the restaurant's default values
        posCallbackRequest.setMinPrepTime("0");
        posCallbackRequest.setMinDeliveryTime("0");

        log.info(
                "Translated to PosCallbackRequest: orderId={}, restaurantId={}, status={}",
                posCallbackRequest.getOrderId(),
                posCallbackRequest.getRestaurantId(),
                posCallbackRequest.getStatus());

        return posCallbackRequest;
    }

    /**
     * Maps internal OrderStatusType to PetPooja-style status codes
     * This ensures UrbanPiper callbacks use the same status mapping as PetPooja
     */
    private String mapOrderStatusToPosCode(OrderStatusType orderStatus) {
        return switch (orderStatus) {
            case ACCEPTED, ACKNOWLEDGED -> "1"; // Accepted
            case FOOD_READY, READY_FOR_DELIVERY -> "5"; // Ready for delivery
            case DISPATCHED, OUT_FOR_DELIVERY -> "4"; // Dispatched
            case DELIVERED -> "10"; // Delivered
            case CANCELLED, REJECTED -> "-1"; // Cancelled
            default -> "0"; // Default/Unknown
        };
    }
}
