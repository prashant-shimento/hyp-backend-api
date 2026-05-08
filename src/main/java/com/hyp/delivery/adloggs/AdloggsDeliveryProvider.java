package com.hyp.delivery.adloggs;

import com.hyp.client.AdloggsClient;
import com.hyp.delivery.DeliveryProvider;
import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.DeliveryPartner;
import com.hyp.exception.DeliveryException;
import com.hyp.model.DeliveryEstimate;
import com.hyp.model.DeliveryOrderStatus;
import com.hyp.translation.AdloggsRequestTranslation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdloggsDeliveryProvider implements DeliveryProvider {

    @Autowired
    private AdloggsClient adloggsClient;

    @Override
    public DeliveryPartner getType() {
        return DeliveryPartner.ADLOGGS;
    }

    @Override
    public DeliveryEstimate getEstimate(Restaurant restaurant, Address address) {
        try {
            AdloggsServiceAvailabilityRequest request =
                    AdloggsRequestTranslation.getServiceAvailabilityRequest(restaurant, address);
            AdloggsServiceAvailabilityResponse response = adloggsClient.checkServiceAvailability(request);

            if (response == null
                    || response.getData() == null
                    || !response.getData().isServiceAvailable()) {
                log.info("Adloggs not available for restaurant={}", restaurant.getId());
                return unavailable();
            }

            Integer eta = response.getData().getToPickup() != null
                    ? response.getData().getToPickup().getEtaMin()
                    : null;

            return DeliveryEstimate.builder()
                    .provider(DeliveryPartner.ADLOGGS)
                    .available(true)
                    .price(response.getData().getEstimatedPrice())
                    .distanceKm(response.getData().getDistance())
                    .etaMinutes(eta)
                    .rawData(response)
                    .build();
        } catch (Exception e) {
            log.warn("Adloggs estimate failed: {}", e.getMessage());
            return unavailable();
        }
    }

    @Override
    public CreateOrderResult createOrder(Restaurant restaurant, Address address, Customer customer, Order order)
            throws DeliveryException {
        AdloggsCreateOrderResponse response = adloggsClient.createOrder(
                AdloggsRequestTranslation.getCreateOrderRequest(restaurant, address, customer, order));
        String orderUuid = response.getData().getOrderUuid();
        if (response.getData().getTrackUrl() != null) {
            order.setDeliveryTrackingLink(response.getData().getTrackUrl());
        }
        log.info(
                "Adloggs createOrder orderUuid={} orderId={} fee={} trackUrl={}",
                orderUuid,
                order.getId(),
                response.getData().getFee(),
                response.getData().getTrackUrl());
        return new CreateOrderResult(orderUuid, response.getData().getFee());
    }

    @Override
    public void cancelOrder(Delivery delivery) throws DeliveryException {
        DeliveryFulfillStatusType fulfillStatus =
                delivery.getFulfillment() != null ? delivery.getFulfillment().getStatus() : null;

        if (isPostPickup(fulfillStatus)) {
            throw new DeliveryException(
                    "adloggs.cancelOrder", "Cannot cancel Adloggs order in status: " + fulfillStatus);
        }

        adloggsClient.cancelOrder(delivery.getDeliveryOrderId(), "Switching delivery partner");
    }

    @Override
    public DeliveryFulfillStatusType mapStatus(String rawStatus) {
        return AdloggsStatusMapper.map(Integer.parseInt(rawStatus));
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
        // Adloggs is webhook-only — no polling API. Build status from stored fulfillment.
        DeliveryOrderStatus status = new DeliveryOrderStatus();
        DeliveryOrderStatus.DeliveryOrderData data = new DeliveryOrderStatus.DeliveryOrderData();
        data.setId(delivery.getDeliveryOrderId());
        data.setReferenceId(delivery.getOrderId());
        data.setStatus(delivery.getStatus().name().toLowerCase());
        data.setFulfillment(delivery.getFulfillment());
        status.setData(data);
        return status;
    }

    private DeliveryEstimate unavailable() {
        return DeliveryEstimate.builder()
                .provider(DeliveryPartner.ADLOGGS)
                .available(false)
                .build();
    }
}
