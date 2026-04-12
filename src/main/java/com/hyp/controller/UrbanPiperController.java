package com.hyp.controller;

import com.hyp.adapter.UrbanPiperAdapter;
import com.hyp.adapter.urbanpiper.order.OrderStatusTransformer;
import com.hyp.adapter.urbanpiper.order.UrbanPiperOrderStatusRequest;
import com.hyp.entity.Order;
import com.hyp.enums.OrderStatusType;
import com.hyp.request.OrderStatusUpdateRequest;
import com.hyp.request.PosStatusRequest;
import com.hyp.request.urbanpiper.InventoryRequest;
import com.hyp.request.urbanpiper.StoreStatusRequest;
import com.hyp.request.urbanpiper.UrbanPiperMenuRequest;
import com.hyp.adapter.urbanpiper.inventory.InventoryTransformer;
import com.hyp.entity.Restaurant;
import com.hyp.request.PosDataRequest;
import com.hyp.request.PosStockRequest;
import com.hyp.response.PosResponse;
import com.hyp.service.OrderService;
import com.hyp.service.PosService;
import com.hyp.service.RestaurantService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Hidden
@RestController
@RequestMapping("/api/v3/pos/urbanpiper")
public class UrbanPiperController {

    @Autowired
    private UrbanPiperAdapter urbanPiperAdapter;

    @Autowired
    private InventoryTransformer inventoryTransformer;

    @Autowired
    private OrderStatusTransformer orderStatusTransformer;

    @Autowired
    private com.hyp.adapter.urbanpiper.order.UrbanPiperCallbackTranslator urbanPiperCallbackTranslator;

    @Autowired
    private PosService posService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private SimpMessagingTemplate messageTemplate;

    @PostMapping("/menu")
    public ResponseEntity<PosResponse> receiveMenu(@RequestBody UrbanPiperMenuRequest request) {
        log.info("UrbanPiper menu request received");
        
        try {
            PosDataRequest posDataRequest = urbanPiperAdapter.transformMenuPayload(request);

            boolean success = posService.savePosData(posDataRequest);
            
            return ResponseEntity.ok(PosResponse.builder()
                    .success(success ? "1" : "0")
                    .message(success ? "Menu received and saved successfully" : "Failed to save menu")
                    .build());
        } catch (Exception e) {
            log.error("Error processing UrbanPiper menu request", e);
            return ResponseEntity.ok(PosResponse.builder()
                    .success("0")
                    .error(e.getMessage())
                    .message("Error processing menu")
                    .build());
        }
    }

    @PostMapping("/inventory")
    public ResponseEntity<PosResponse> receiveInventory(@RequestBody InventoryRequest request) {
        log.info("UrbanPiper inventory payload received: {}", request);
        
        try {
            PosStockRequest stockRequest = inventoryTransformer.transform(request);
            Restaurant restaurant = restaurantService.findByMenuSharingCode(stockRequest.getRestaurantId());

            if (restaurant == null) {
                return ResponseEntity.ok(PosResponse.builder()
                        .code(HttpStatus.NOT_FOUND.value())
                        .message("Restaurant Not Found")
                        .status("failed")
                        .success("0")
                        .build());
            }
            
            boolean result = posService.updateStock(stockRequest);
            messageTemplate.convertAndSend("/topic/item-status", stockRequest);
            
            return ResponseEntity.ok(PosResponse.builder()
                    .code(result ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message(result ? "Stock Updated Successfully" : "Something Went Wrong")
                    .status(result ? "success" : "failed")
                    .success(result ? "1" : "0")
                    .build());
        } catch (Exception e) {
            log.error("Error processing UrbanPiper inventory payload", e);
            return ResponseEntity.ok(PosResponse.builder()
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success("0")
                    .error(e.getMessage())
                    .message("Error processing inventory")
                    .status("failed")
                    .build());
        }
    }

    @PostMapping("/order/status")
    public ResponseEntity<PosResponse> receiveOrderStatus(@RequestBody UrbanPiperOrderStatusRequest request) {
        log.info("UrbanPiper order status update received for order: {}, status: {}", 
                request.getExternalOrderId(), request.getStatus());
        
        try {
            // Translate UrbanPiper request to PosCallbackRequest
            com.hyp.request.PosCallbackRequest posCallbackRequest = 
                    urbanPiperCallbackTranslator.translateToPosCallback(request, request.getLocationRefId());
            
            // Use the same processOrderCallback method as PetPooja
            orderService.processOrderCallback(posCallbackRequest);
            
            return ResponseEntity.ok(PosResponse.builder()
                    .code(HttpStatus.OK.value())
                    .success("1")
                    .message("Order status updated successfully")
                    .status("success")
                    .build());
                    
        } catch (Exception e) {
            log.error("Error processing UrbanPiper order status update", e);
            return ResponseEntity.ok(PosResponse.builder()
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success("0")
                    .error(e.getMessage())
                    .message("Error processing order status")
                    .status("failed")
                    .build());
        }
    }

    @PostMapping("/store/status")
    public ResponseEntity<PosResponse> receiveStoreStatus(@RequestBody StoreStatusRequest request) {
        log.info("UrbanPiper store status update received: location_ref_id={}, ordering_enabled={}", 
                request.getLocationRefId(), request.getOrderingEnabled());
        
        try {
            if (request.getLocationRefId() == null || request.getLocationRefId().isEmpty()) {
                return ResponseEntity.ok(PosResponse.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .success("0")
                        .message("location_ref_id is required")
                        .status("failed")
                        .build());
            }

            if (request.getOrderingEnabled() == null) {
                return ResponseEntity.ok(PosResponse.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .success("0")
                        .message("ordering_enabled is required")
                        .status("failed")
                        .build());
            }

            PosStatusRequest posStatusRequest = new PosStatusRequest();
            posStatusRequest.setMenuSharingCode(request.getLocationRefId());
            posStatusRequest.setStoreStatus(request.getOrderingEnabled() ? "1" : "0");
            posStatusRequest.setReason(request.getOrderingEnabled() ? null : "Store disabled via UrbanPiper");

            posService.updateRestaurant(posStatusRequest);

            messageTemplate.convertAndSend("/topic/restaurant-status", posStatusRequest);
            
            return ResponseEntity.ok(PosResponse.builder()
                    .code(HttpStatus.OK.value())
                    .success("1")
                    .message("Store Toggle Details Successfully Updated")
                    .status("success")
                    .build());
                    
        } catch (Exception e) {
            log.error("Error processing store status update", e);
            return ResponseEntity.ok(PosResponse.builder()
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success("0")
                    .error(e.getMessage())
                    .message("Error processing store status")
                    .status("failed")
                    .build());
        }
    }

    @PostMapping("/order/status/exchange")
    public ResponseEntity<PosResponse> updateOrderStatusExchange(@RequestBody OrderStatusUpdateRequest request) {
        log.info("Order status exchange request received for order: {}, status: {}", 
                request.getOrderNo(), request.getNewStatus());
        
        try {
            // Validate the request
            if (request.getOrderNo() == null || request.getOrderNo().isEmpty()) {
                return ResponseEntity.ok(PosResponse.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .success("0")
                        .message("order_no is required")
                        .status("failed")
                        .build());
            }
            
            if (request.getNewStatus() == null || request.getNewStatus().isEmpty()) {
                return ResponseEntity.ok(PosResponse.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .success("0")
                        .message("new_status is required")
                        .status("failed")
                        .build());
            }
            
            // Validate reason is mandatory for cancelled or rejected
            String status = request.getNewStatus().toLowerCase();
            if ((status.equals("cancelled") || status.equals("rejected")) 
                    && (request.getReason() == null || request.getReason().isEmpty())) {
                return ResponseEntity.ok(PosResponse.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .success("0")
                        .message("reason is mandatory when new_status is cancelled or rejected")
                        .status("failed")
                        .build());
            }
            
            // Log reason if provided
            if (request.getReason() != null && !request.getReason().isEmpty()) {
                log.info("Order {} status changing to {} with reason: {}", 
                        request.getOrderNo(), request.getNewStatus(), request.getReason());
            }
            
            // Create PosCallbackRequest for translation
            UrbanPiperOrderStatusRequest urbanPiperStatusRequest = new UrbanPiperOrderStatusRequest();
            urbanPiperStatusRequest.setExternalOrderId(request.getOrderNo());
            urbanPiperStatusRequest.setStatus(request.getNewStatus());
            
            // Translate and use processOrderCallback
            com.hyp.request.PosCallbackRequest posCallbackRequest = 
                    urbanPiperCallbackTranslator.translateToPosCallback(urbanPiperStatusRequest, null);
            
            orderService.processOrderCallback(posCallbackRequest);
            
            return ResponseEntity.ok(PosResponse.builder()
                    .code(HttpStatus.OK.value())
                    .success("1")
                    .message("Order status updated successfully")
                    .status("success")
                    .build());
                    
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", e.getMessage());
            return ResponseEntity.ok(PosResponse.builder()
                    .code(HttpStatus.BAD_REQUEST.value())
                    .success("0")
                    .error(e.getMessage())
                    .message("Invalid status value")
                    .status("failed")
                    .build());
        } catch (Exception e) {
            log.error("Error processing order status exchange", e);
            return ResponseEntity.ok(PosResponse.builder()
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .success("0")
                    .error(e.getMessage())
                    .message("Error processing order status")
                    .status("failed")
                    .build());
        }
    }
}