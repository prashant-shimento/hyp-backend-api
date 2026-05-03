package com.hyp.controller;

import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.event.OrderEventPublisher;
import com.hyp.request.PosCallbackRequest;
import com.hyp.request.PosDataRequest;
import com.hyp.request.PosStatusRequest;
import com.hyp.request.PosStockRequest;
import com.hyp.response.PosResponse;
import com.hyp.service.OrderService;
import com.hyp.service.PosService;
import com.hyp.service.RestaurantService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Hidden
@RestController
@RequestMapping("/pos")
public class PosController {

    @Autowired
    @Qualifier("petPooja")
    PosService posDataService;

    @Autowired
    RestaurantService restaurantService;

    @Autowired
    OrderService orderService;

    @Autowired
    SimpMessagingTemplate messageTemplate;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @PostMapping("/menu")
    public ResponseEntity<PosResponse> saveMenuData(@RequestBody PosDataRequest posDataRequest) {
        boolean result = posDataService.savePosData(posDataRequest);
        return ResponseEntity.ok(PosResponse.builder()
                .success(result ? "1" : "0")
                .message(result ? "Menu items are successfully listed." : "Something Went Wrong")
                .build());
    }

    @PostMapping("/status/get")
    public ResponseEntity<PosResponse> getStatus(@RequestBody PosStatusRequest getStatus) {
        Restaurant restaurant = restaurantService.findByMenuSharingCode(getStatus.getMenuSharingCode());
        PosResponse response = PosResponse.builder()
                .httpCode(restaurant != null ? HttpStatus.OK.value() : HttpStatus.NOT_FOUND.value())
                .status(restaurant != null ? "success" : "failed")
                .storeStatus(restaurant != null && restaurant.isActive() ? "1" : "0")
                .message(restaurant != null ? "Store Delivery Status fetched successfully" : "Restaurant Not Found")
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/status/update")
    public ResponseEntity<PosResponse> updateStatus(@RequestBody PosStatusRequest updateStatus) {
        posDataService.updateRestaurant(updateStatus);
        messageTemplate.convertAndSend("/topic/restaurant-status", updateStatus);
        return ResponseEntity.ok(PosResponse.builder()
                .httpCode(HttpStatus.OK.value())
                .message("Restaurant Updated Successfully")
                .status("success")
                .build());
    }

    @PostMapping("/stock")
    public ResponseEntity<PosResponse> updateStock(@RequestBody PosStockRequest stockRequest) {
        PosResponse response = null;
        Restaurant restaurant = restaurantService.findByMenuSharingCode(stockRequest.getRestaurantId());
        if (restaurant == null) {
            response = PosResponse.builder()
                    .code(HttpStatus.NOT_FOUND.value())
                    .message("Restaurant Not Found")
                    .status("failed")
                    .build();
            return ResponseEntity.ok(response);
        }
        boolean result = posDataService.updateStock(stockRequest);
        messageTemplate.convertAndSend("/topic/item-status", stockRequest);
        response = PosResponse.builder()
                .code(result ? HttpStatus.OK.value() : HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message(result ? "Stock Updated Successfully" : "Something Went Wrong")
                .status(result ? "success" : "failed")
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/order/callback")
    public ResponseEntity<PosResponse> orderCallBack(@RequestBody PosCallbackRequest posCallbackRequest) {
        PosResponse response = null;
        try {
            orderService.processOrderCallback(posCallbackRequest);
            response = PosResponse.builder()
                    .httpCode(HttpStatus.OK.value())
                    .message("Order Updated Successfully")
                    .build();
            return new ResponseEntity<PosResponse>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Exception occurred on orderCallBack {}", e.getMessage());
            response = PosResponse.builder()
                    .httpCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Error Occurred")
                    .error(e.getMessage())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @PostMapping("/order/{orderId}")
    public ResponseEntity<PosResponse> createOrder(@PathVariable String orderId) {
        PosResponse response = null;
        try {
            Order order = orderService.findById(orderId);
            orderEventPublisher.publishProcessOrderEvent(order);
            return new ResponseEntity<PosResponse>(response, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Exception occurred on POS createOrder {}", e.getMessage());
            response = PosResponse.builder()
                    .httpCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Error Occurred in createOrder")
                    .error(e.getMessage())
                    .build();
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }
}
