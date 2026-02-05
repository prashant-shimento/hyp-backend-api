package com.hyp.controller;

import com.hyp.constants.ErrorConstants;
import com.hyp.dto.OrderDto;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.entity.Settlement;
import com.hyp.enums.*;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.exception.OrderNotFoundException;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest.RiderDetails;
import com.hyp.response.Response;
import com.hyp.service.DeliveryService;
import com.hyp.service.OrderService;
import com.hyp.service.PaymentService;
import com.hyp.service.PosService;
import com.hyp.service.RedisService;
import com.hyp.service.RestaurantService;
import com.hyp.service.SettlementService;
import com.hyp.translation.OrderTranslation;
import com.hyp.translation.PosOrderRequestTranslation;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/order")
@Tag(name = "Order", description = "Order management APIs")
public class OrderController extends BaseListController<OrderDto, Order, String> {

    @Autowired
    OrderService orderService;

    @Autowired
    OrderTranslation orderTranslation;

    @Autowired
    RestaurantService restaurantService;

    @Autowired
    PosService posService;

    @Autowired
    DeliveryService deliveryService;

    @Autowired
    PaymentService paymentService;

    @Autowired
    RedisService redisService;

    @Autowired
    PosOrderRequestTranslation posOrderRequestTranslation;

    @Autowired
    SettlementService settlementService;

    @Operation(
            summary = "Create a new order",
            description = "Creates an order with full price validation, offer application, and referral handling. "
                    + "All catalog lookups (items, variations, addons, taxes) run in parallel for sub-500ms response. "
                    + "After creation, call POST /payment/{orderId} to initiate payment.")
    @PostMapping()
    public ResponseEntity<Response> create(@RequestBody @Valid OrderDto orderDto) {
        Response response;
        try {
            Order createdOrder = orderService.create(orderDto);
            OrderDto createdOrderDto = orderTranslation.getDto(createdOrder);
            response = new Response(Collections.singletonList(createdOrderDto), false, "Order Created");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception occurred in create order {}", e.getMessage());
            response = new Response(null, true, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<Response> update(@PathVariable String orderId, @RequestBody OrderDto orderDto)
            throws OrderNotFoundException {
        Response response;
        Order order = orderService.update(orderId, orderDto);
        response = new Response(Collections.singletonList(orderTranslation.getDto(order)), false, "Order Updated");
        return ResponseEntity.ok(response);
    }

    @Hidden
    @PostMapping("/rider/{orderId}")
    public ResponseEntity<Response> orderRiderUpdate(@PathVariable String orderId) {
        Response response;
        try {
            Order order = orderService.findById(orderId);
            Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
            PosRiderUpdateRequest posRiderUpdateRequest = posOrderRequestTranslation.getPosRiderStatusUpdateRequest(
                    restaurant,
                    order,
                    new RiderDetails("rider", "9964552656"),
                    RiderStatusType.rider_assigned.getValue());
            String posResponse = posService.updatePosRiderStatus(posRiderUpdateRequest);
            response = new Response(Collections.singletonList(posResponse), false, "Rider Status Updated");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Exception occurred in orderRiderUpdate {}", e.getMessage());
            response = new Response(null, true, ErrorConstants.INTERNAL_SERVER_ERROR);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/track/{orderId}")
    public ResponseEntity<Void> orderTracking(@PathVariable String orderId) throws EntityNotFoundException {
        Order order = Optional.ofNullable(orderService.findById(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));

        String trackingUrl = "https://t.pidge.in?t=" + delivery.getFulfillment().getTrackCode();
        if (trackingUrl.isBlank()) {
            throw new EntityNotFoundException("Order Tracking Link", orderId);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(trackingUrl))
                .build();
    }

    @PostMapping("/{orderId}/settlement")
    public ResponseEntity<Response> orderSettlement(@PathVariable String orderId) throws EntityNotFoundException {
        Order order = Optional.ofNullable(orderService.findById(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
        log.info("Computing Settlement for orderId {} via API", orderId);
        Settlement settlement = settlementService.processSettlement(order);
        return ResponseEntity.ok(Response.builder()
                .data(Collections.singletonList(settlement))
                .error(false)
                .message("Order Settlement Consumed Successfully")
                .build());
    }
}
