package com.hyp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hyp.constants.Constants;
import com.hyp.constants.ErrorConstants;
import com.hyp.delivery.adloggs.AdloggsWebhookPayload;
import com.hyp.dto.DeliveryDto;
import com.hyp.entity.*;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.DeliveryOrderStatusType;
import com.hyp.enums.DeliveryPartner;
import com.hyp.enums.OrderStatusType;
import com.hyp.event.OrderEventPublisher;
import com.hyp.exception.BadRequestException;
import com.hyp.exception.DeliveryException;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.model.DeliveryOrderStatus;
import com.hyp.model.DeliveryOrderStatus.DeliveryFulfillment;
import com.hyp.model.DeliveryOrderStatus.DeliveryOrderData;
import com.hyp.model.DeliveryQuote;
import com.hyp.model.DeliveryRiderLocation;
import com.hyp.model.RiderLocation;
import com.hyp.response.Response;
import com.hyp.service.AddressService;
import com.hyp.service.DeliveryService;
import com.hyp.service.LocationService;
import com.hyp.service.OrderService;
import com.hyp.service.RestaurantService;
import com.hyp.translation.DeliveryRequestTranslation;
import com.hyp.translation.DeliveryTranslation;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/delivery")
public class DeliveryController extends BaseController<DeliveryDto, Delivery, String> {

    @Autowired
    DeliveryTranslation deliveryTranslation;

    @Autowired
    RestaurantService restaurantService;

    @Autowired
    DeliveryService deliveryService;

    @Autowired
    AddressService addressService;

    @Autowired
    OrderService orderService;

    @Autowired
    LocationService locationService;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private DeliveryRequestTranslation deliveryRequestTranslation;

    @PostMapping("/callback")
    public ResponseEntity<Response> updateDeliveryOrderStatus(@RequestBody DeliveryOrderData deliveryOrderData)
            throws EntityNotFoundException, DeliveryException {
        Delivery delivery;

        if (deliveryOrderData.getId() != null && !deliveryOrderData.getId().isBlank()) {
            delivery = deliveryService.findByDeliveryOrderIdIncludingDeleted(deliveryOrderData.getId());
            if (delivery == null) {
                throw new EntityNotFoundException("Delivery", deliveryOrderData.getId());
            }
        } else if (deliveryOrderData.getReferenceId() != null
                && !deliveryOrderData.getReferenceId().isBlank()) {
            delivery = deliveryService.findByOrderIdIncludingDeleted(deliveryOrderData.getReferenceId());
            if (delivery == null) {
                throw new EntityNotFoundException("Delivery", deliveryOrderData.getReferenceId());
            }
        } else {
            throw new EntityNotFoundException("Delivery", "no identifiers in callback payload");
        }

        deliveryService.processDeliveryCallback(delivery, deliveryOrderData);
        return ResponseEntity.ok(new Response(null, false, "Success"));
    }

    @GetMapping("/quote/{restaurantId}")
    public ResponseEntity<Response> getDeliveryQuote(@PathVariable String restaurantId, @RequestParam String addressId)
            throws EntityNotFoundException, BadRequestException, DeliveryException {

        Restaurant restaurant = Optional.ofNullable(restaurantService.findById(restaurantId))
                .orElseThrow(() -> new EntityNotFoundException(Restaurant.class.getSimpleName(), restaurantId));

        if (restaurant.getPincode() == null || !restaurant.getPincode().matches("\\d{6}")) {
            throw new BadRequestException("Pin code", "Invalid pin code " + restaurant.getPincode());
        }

        Address address = Optional.ofNullable(addressService.findById(addressId))
                .orElseThrow(() -> new EntityNotFoundException(Address.class.getSimpleName(), addressId));

        if (address.getPincode() == null || !address.getPincode().matches("\\d{6}")) {
            throw new BadRequestException("Pin code", "Invalid pin code " + address.getPincode());
        }

        if (!locationService.isLocationDeliverable(
                address.getLocation().getLatitude(),
                address.getLocation().getLongitude(),
                restaurant.getLocation().getLatitude(),
                restaurant.getLocation().getLongitude(),
                restaurant.getDeliveryRadius())) {
            throw new BadRequestException("Location", "The location is not deliverable.");
        }

        log.info("Requesting Delivery quote for addressId {} and restaurantId {}", addressId, restaurantId);
        DeliveryQuote deliveryQuote =
                deliveryService.getDeliveryQuote(deliveryRequestTranslation.getQuoteRequest(restaurant, address));

        if (deliveryQuote.getData() == null
                || deliveryQuote.getData().getItems() == null
                || deliveryQuote.getData().getItems().isEmpty()) {
            throw new EntityNotFoundException("Delivery", ErrorConstants.DELIVERY_OPTION_NOT_FOUND);
        }

        DeliveryQuote.DeliveryNetworks selectedQuote = deliveryQuote.getData().getItems().stream()
                .filter(DeliveryQuote.DeliveryNetworks::isPickupNow)
                .filter(item -> !item.getService().equalsIgnoreCase("loadshare"))
                .min(Comparator.comparingDouble(item -> item.getQuote().getPrice()))
                .orElseThrow(() -> new EntityNotFoundException("Delivery", ErrorConstants.DELIVERY_OPTION_NOT_FOUND));

        DeliveryQuoteRecord quoteRecord = deliveryService.saveDeliveryQuote(restaurantId, addressId, selectedQuote);
        selectedQuote.setDeliveryQuoteId(quoteRecord.getId());

        return ResponseEntity.ok(
                new Response(Collections.singletonList(selectedQuote), false, "Delivery Quotes Fetched"));
    }

    /** Adloggs webhook callback. */
    @PostMapping("/adloggs/callback")
    public ResponseEntity<Response> adloggsCallback(@RequestBody AdloggsWebhookPayload payload)
            throws EntityNotFoundException, DeliveryException, JsonProcessingException {
        log.info("Received Adloggs callback: {}", payload);
        log.info(
                "Adloggs callback received orderId={} orderUuid={} statusId={}",
                payload.getPartnerOrderId(),
                payload.getOrderUuid(),
                payload.getOrderStatusId());
        Delivery delivery = Optional.ofNullable(deliveryService.findByDeliveryOrderId(payload.getOrderUuid()))
                .orElseThrow(() -> new EntityNotFoundException("Delivery", payload.getOrderUuid()));
        deliveryService.processAdloggsCallback(delivery, payload);
        return ResponseEntity.ok(new Response(null, false, "Success"));
    }

    /** Manually switch the active delivery to the alternate provider (ops endpoint). */
    @PostMapping("/switch/{orderId}")
    public ResponseEntity<Response> switchDeliveryProvider(@PathVariable String orderId)
            throws EntityNotFoundException, DeliveryException {
        Optional.ofNullable(orderService.findById(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
        // forceOverride=true — ops explicitly switching, bypasses FIXED strategy restriction
        deliveryService.switchDeliveryProvider(orderId, null, true);
        return ResponseEntity.ok(new Response(null, false, "Provider switch initiated"));
    }

    @Hidden
    @GetMapping("/rider-detail/{orderId}")
    public ResponseEntity<Response> getRiderDetails(@PathVariable String orderId)
            throws EntityNotFoundException, DeliveryException {
        Order order = Optional.ofNullable(orderService.findById(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
        Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));
        DeliveryRiderLocation deliveryRiderLocation =
                deliveryService.getDeliveryRiderLocation(delivery.getDeliveryOrderId());

        Response response =
                new Response(Collections.singletonList(deliveryRiderLocation), false, "Rider Details Fetched");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rider-location/{orderId}")
    public ResponseEntity<Response> getRiderLocation(@PathVariable String orderId)
            throws EntityNotFoundException, DeliveryException {
        RiderLocation riderLocation = null;
        Order order = Optional.ofNullable(orderService.findById(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
        Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));
        if (delivery.getFulfillment().getChannel().getName().equalsIgnoreCase("porter")
                || delivery.getService().equalsIgnoreCase("porter")) {
            log.info("Getting Porter Rider location of the order {}", orderId);
            riderLocation = deliveryService.getPorterRiderLocation(delivery);
            if (riderLocation == null) {
                return ResponseEntity.status(503)
                        .body(new Response(
                                null, true, "Rider location temporarily unavailable — please retry shortly"));
            }
        } else {
            log.info("Getting Rider location of the order {}", orderId);
            riderLocation = deliveryService.getRiderLocation(delivery.getDeliveryOrderId());
        }
        Response response = new Response(Collections.singletonList(riderLocation), false, "Rider Location Fetched");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create/{orderId}")
    public ResponseEntity<Response> createDeliveryOrder(
            @PathVariable String orderId, @RequestParam(required = false) String partner)
            throws EntityNotFoundException, DeliveryException, BadRequestException {

        Order order = Optional.ofNullable(orderService.findById(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        DeliveryPartner requestedPartner = null;
        if (partner != null && !partner.isBlank()) {
            try {
                requestedPartner = DeliveryPartner.valueOf(partner.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(
                        "mode",
                        "Unknown delivery partner: " + partner + ". Valid values: "
                                + java.util.Arrays.toString(DeliveryPartner.values()));
            }
        }

        Delivery existingDelivery = deliveryService.findByOrderId(orderId);
        if (existingDelivery != null) {
            DeliveryFulfillment existingFulfillment = existingDelivery.getFulfillment();
            boolean hasActiveRider = existingFulfillment != null
                    && existingFulfillment.getStatus() != null
                    && isActiveRiderStatus(existingFulfillment.getStatus());
            if (hasActiveRider) {
                throw new DeliveryException(
                        "Active rider already assigned — cancel the existing delivery before switching");
            }
            if (existingDelivery.getStatus() == DeliveryOrderStatusType.COMPLETED) {
                throw new DeliveryException("Delivery already completed for this order");
            }
            deliveryService.cancelDeliveryOrder(existingDelivery);
        }

        // MANUAL — ops tracks manually (Ola, Uber, Rapido etc.)
        if (requestedPartner == DeliveryPartner.MANUAL) {
            Delivery delivery = deliveryService.createManualDelivery(order, DeliveryPartner.MANUAL);
            orderService.updateOrderStatus(orderId, OrderStatusType.MANUAL_DELIVERY_BOOKED);
            return ResponseEntity.ok(new Response(
                    Collections.singletonList(deliveryTranslation.getDto(delivery)), false, "Manual delivery created"));
        }

        // Default — external API partner (Pidge/Adloggs) via DeliveryRouter
        orderEventPublisher.publishCreateDeliveryOrderEvent(order);
        return ResponseEntity.ok(new Response(null, false, "Delivery order created"));
    }

    @PostMapping("/fulfill/{orderId}")
    public ResponseEntity<Response> fulfillOrder(
            @PathVariable String orderId, @RequestParam(defaultValue = "", required = false) String fulfillType)
            throws EntityNotFoundException, DeliveryException {
        Order order = Optional.ofNullable(orderService.findById(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));
        deliveryService.processDeliveryOrderFulfill(delivery, Constants.API, fulfillType);

        Response response = new Response(
                Collections.singletonList(deliveryTranslation.getDto(delivery)), false, "Delivery Fullfilled");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/consume/{orderId}")
    public ResponseEntity<Response> consumeDeliveryCallback(@PathVariable String orderId)
            throws EntityNotFoundException, DeliveryException {
        Order order = Optional.ofNullable(orderService.findById(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));

        DeliveryOrderStatus deliverOrderStatus = deliveryService.getDeliveryOrderStatus(delivery);
        deliveryService.processDeliveryCallback(delivery, deliverOrderStatus.getData());
        Response response = new Response(
                Collections.singletonList(deliveryTranslation.getDto(delivery)), false, "Delivery Processed Consumed");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/unallocate/{orderId}")
    public ResponseEntity<Response> unallocate(@PathVariable String orderId) throws EntityNotFoundException {
        Order order = Optional.ofNullable(orderService.findById(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));

        deliveryService.unallocateDeliveryOrder(delivery);
        Response response = new Response(null, false, "Order Unallocated Successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel/{orderId}")
    public ResponseEntity<Response> cancelDeliveryOrder(@PathVariable String orderId)
            throws DeliveryException, EntityNotFoundException {
        Response response;
        Order order = Optional.ofNullable(orderService.findById(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));

        deliveryService.cancelDeliveryOrder(delivery);
        response = new Response(null, false, "Delivery Order Cancelled");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<Response> getDeliveryOrderStatus(@PathVariable String orderId)
            throws DeliveryException, EntityNotFoundException {
        Order order = Optional.ofNullable(orderService.findById(orderId))
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        Delivery delivery = Optional.ofNullable(deliveryService.findByOrderId(order.getId()))
                .orElseThrow(() -> new EntityNotFoundException("Delivery", orderId));

        DeliveryOrderStatus deliveryOrderStatus = deliveryService.getDeliveryOrderStatus(delivery);
        Response response = new Response(
                Collections.singletonList(deliveryOrderStatus.getData()), false, "Delivery Order Status Fetched");
        return ResponseEntity.ok(response);
    }

    @Override
    @PatchMapping("/{id}")
    public ResponseEntity<Response> update(@PathVariable String id, @RequestBody DeliveryDto dto) {
        Delivery delivery = Optional.ofNullable(deliveryService.findById(id))
                .orElseThrow(() -> new RuntimeException(new EntityNotFoundException("Delivery", id)));

        deliveryTranslation.updateEntityFromDto(dto, delivery);

        if (DeliveryPartner.MANUAL.name().equalsIgnoreCase(delivery.getChannel())
                && delivery.getFulfillment() != null
                && delivery.getFulfillment().getStatus() != null) {
            Order order = Optional.ofNullable(orderService.findById(delivery.getOrderId()))
                    .orElseThrow(
                            () -> new RuntimeException(new EntityNotFoundException("Order", delivery.getOrderId())));
            Delivery updated = deliveryService.updateManualDeliveryStatus(delivery, order);
            return ResponseEntity.ok(new Response(
                    Collections.singletonList(deliveryTranslation.getDto(updated)), false, "Delivery Updated"));
        }

        Delivery updated = deliveryService.save(delivery);
        return ResponseEntity.ok(new Response(
                Collections.singletonList(deliveryTranslation.getDto(updated)), false, "Delivery Updated"));
    }

    private boolean isActiveRiderStatus(DeliveryFulfillStatusType status) {
        return switch (status) {
            case OUT_FOR_PICKUP, REACHED_PICKUP, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY, REACHED_DELIVERY -> true;
            default -> false;
        };
    }
}
