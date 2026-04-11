package com.hyp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.adapter.UrbanPiperAdapter;
import com.hyp.adapter.urbanpiper.order.OrderStatusTransformer;
import com.hyp.adapter.urbanpiper.order.UrbanPiperOrderStatusRequest;
import com.hyp.entity.Order;
import com.hyp.enums.OrderStatusType;
import com.hyp.request.OrderStatusUpdateRequest;
import com.hyp.request.urbanpiper.InventoryRequest;
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
            String orderId = request.getExternalOrderId();
            if (orderId == null || orderId.isEmpty()) {
                orderId = request.getOrderId();
            }
            
            if (orderId == null || orderId.isEmpty()) {
                return ResponseEntity.ok(PosResponse.builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .success("0")
                        .message("Order ID is required")
                        .status("failed")
                        .build());
            }
            
            Order order = orderService.findById(orderId);
            if (order == null) {
                return ResponseEntity.ok(PosResponse.builder()
                        .code(HttpStatus.NOT_FOUND.value())
                        .success("0")
                        .message("Order not found")
                        .status("failed")
                        .build());
            }
            
            OrderStatusType newStatus = orderStatusTransformer.mapUrbanPiperStatus(request.getStatus());
            OrderStatusType oldStatus = order.getStatus();
            
            orderService.updateOrderStatus(orderId, newStatus);
            
            if (newStatus == OrderStatusType.ACCEPTED && oldStatus != OrderStatusType.ACCEPTED) {
                log.info("Order {} acknowledged, triggering fulfillment workflow", orderId);
                orderService.startOrderFulfillmentWorkflow(orderId, 0);
            }
            
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
    public ResponseEntity<PosResponse> receiveStoreStatus(@RequestBody JsonNode payload) {
        log.info("UrbanPiper store status payload received: {}", payload);
        return ResponseEntity.ok(PosResponse.builder()
                .success("1")
                .message("Store status received successfully")
                .build());
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
            
            Order order = orderService.findById(request.getOrderNo());
            if (order == null) {
                return ResponseEntity.ok(PosResponse.builder()
                        .code(HttpStatus.NOT_FOUND.value())
                        .success("0")
                        .message("Order not found")
                        .status("failed")
                        .build());
            }
            
            // Map external status to internal enum and update
            OrderStatusType newStatus = orderStatusTransformer.mapUrbanPiperStatus(request.getNewStatus());
            OrderStatusType oldStatus = order.getStatus();
            
            // Log reason if provided
            if (request.getReason() != null && !request.getReason().isEmpty()) {
                log.info("Order {} status changing to {} with reason: {}", 
                        request.getOrderNo(), newStatus, request.getReason());
            }
            
            orderService.updateOrderStatus(request.getOrderNo(), newStatus);
            
            // Trigger fulfillment workflow if accepted
            if (newStatus == OrderStatusType.ACCEPTED && oldStatus != OrderStatusType.ACCEPTED) {
                log.info("Order {} acknowledged, triggering fulfillment workflow", request.getOrderNo());
                orderService.startOrderFulfillmentWorkflow(request.getOrderNo(), 0);
            }
            
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