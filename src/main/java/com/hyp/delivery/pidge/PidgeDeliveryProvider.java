package com.hyp.delivery.pidge;

import com.hyp.client.PidgeClient;
import com.hyp.delivery.DeliveryProvider;
import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.DeliveryOrderStatusType;
import com.hyp.enums.DeliveryPartner;
import com.hyp.exception.DeliveryException;
import com.hyp.model.DeliveryEstimate;
import com.hyp.model.DeliveryOrderStatus;
import com.hyp.model.DeliveryQuote;
import com.hyp.model.DeliveryQuote.DeliveryNetworks;
import com.hyp.request.DeliveryOrderRequest;
import com.hyp.request.DeliveryQuoteRequest;
import com.hyp.translation.DeliveryRequestTranslation;
import java.time.Duration;
import java.util.Comparator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PidgeDeliveryProvider implements DeliveryProvider {

    @Autowired
    private PidgeClient pidgeClient;

    @Autowired
    private DeliveryRequestTranslation deliveryRequestTranslation;

    @Override
    public DeliveryPartner getType() {
        return DeliveryPartner.PIDGE;
    }

    @Override
    public DeliveryEstimate getEstimate(Restaurant restaurant, Address address) {
        try {
            DeliveryQuoteRequest quoteRequest = deliveryRequestTranslation.getQuoteRequest(restaurant, address);
            DeliveryQuote quote = pidgeClient.getDeliveryQuote(quoteRequest);

            if (quote == null || quote.getData() == null || quote.getData().getItems() == null) {
                return unavailable();
            }

            DeliveryNetworks selected = quote.getData().getItems().stream()
                    .filter(DeliveryNetworks::isPickupNow)
                    .filter(n -> !n.getService().equalsIgnoreCase("loadshare"))
                    .filter(n -> n.getQuote() != null)
                    .min(Comparator.comparingDouble(n -> n.getQuote().getPrice()))
                    .orElse(null);

            if (selected == null) {
                return unavailable();
            }

            Integer etaMin = null;
            if (selected.getQuote().getEta() != null) {
                etaMin = Integer.valueOf(selected.getQuote().getEta().getDropMin());
            }

            return DeliveryEstimate.builder()
                    .provider(DeliveryPartner.PIDGE)
                    .available(true)
                    .price(selected.getQuote().getPrice())
                    .etaMinutes(etaMin)
                    .rawData(selected)
                    .build();
        } catch (Exception e) {
            log.warn("Pidge estimate failed: {}", e.getMessage());
            return unavailable();
        }
    }

    @Override
    public CreateOrderResult createOrder(Restaurant restaurant, Address address, Customer customer, Order order)
            throws DeliveryException {
        DeliveryOrderRequest request =
                deliveryRequestTranslation.getDeliveryOrderRequest(restaurant, address, customer, order);
        String deliveryOrderId = pidgeClient.createDeliveryOrder(request).block(Duration.ofSeconds(10));
        if (deliveryOrderId == null || deliveryOrderId.isBlank()) {
            throw new DeliveryException("pidge.createOrder", "Empty deliveryOrderId from Pidge");
        }
        log.info("Pidge createOrder deliveryOrderId={} orderId={}", deliveryOrderId, order.getId());
        // Pidge fee comes from the quote estimate, not the create response
        return new CreateOrderResult(deliveryOrderId, null);
    }

    @Override
    public void cancelOrder(Delivery delivery) throws DeliveryException {
        DeliveryFulfillStatusType fulfillStatus =
                delivery.getFulfillment() != null ? delivery.getFulfillment().getStatus() : null;

        if (isPostPickup(fulfillStatus)) {
            throw new DeliveryException(
                    "pidge.cancelOrUnallocate", "Cannot cancel Pidge order in status: " + fulfillStatus);
        }

        DeliveryOrderStatusType orderStatus = delivery.getStatus();
        String deliveryOrderId = delivery.getDeliveryOrderId();

        if (orderStatus == DeliveryOrderStatusType.FULFILLED && !isPostPickup(fulfillStatus)) {
            // FULFILLED but not picked up → unallocate first, then cancel
            log.info("Unallocating Pidge order before cancel deliveryOrderId={}", deliveryOrderId);
            pidgeClient.unallocateDeliveryOrder(deliveryOrderId).block(Duration.ofSeconds(10));
        }

        pidgeClient.cancelDeliveryOrder(deliveryOrderId).block(Duration.ofSeconds(10));
        log.info("Pidge cancelOrUnallocate successful deliveryOrderId={}", deliveryOrderId);
    }

    @Override
    public DeliveryFulfillStatusType mapStatus(String rawStatus) {
        // Pidge uses the existing DeliveryFulfillStatusType names directly
        return DeliveryFulfillStatusType.valueOf(rawStatus);
    }

    @Override
    public boolean isSwitchable(DeliveryFulfillStatusType currentStatus) {
        return !isPostPickup(currentStatus);
    }

    private boolean isPostPickup(DeliveryFulfillStatusType status) {
        if (status == null) return false;
        return switch (status) {
            case PICKED_UP,
                    IN_TRANSIT,
                    OUT_FOR_DELIVERY,
                    REACHED_DELIVERY,
                    DELIVERED,
                    RTO_OUT_FOR_DELIVERY,
                    RTO_DELIVERED,
                    RTO_UNDELIVERED,
                    UNDELIVERED,
                    LOST,
                    DAMAGED -> true;
            default -> false;
        };
    }

    @Override
    public DeliveryOrderStatus getOrderStatus(Delivery delivery) throws DeliveryException {
        return pidgeClient.getDeliveryOrderStatus(delivery.getDeliveryOrderId());
    }

    private DeliveryEstimate unavailable() {
        return DeliveryEstimate.builder()
                .provider(DeliveryPartner.PIDGE)
                .available(false)
                .build();
    }
}
