package com.hyp.adapter.urbanpiper.order;

import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UrbanPiperOrderTransformer {

    public UrbanPiperOrderRequest transform(Order order, Customer customer, Restaurant restaurant) {
        return UrbanPiperOrderRequest.builder()
                .customer(buildCustomer(customer))
                .items(buildItems(order))
                .meta(buildMeta(order, restaurant))
                .discounts(buildOrderLevelDiscounts(order))
                .payment(buildPayment(order))
                .build();
    }

    private UrbanPiperOrderRequest.Customer buildCustomer(Customer customer) {
        Address address = customer.getAddresses() != null && !customer.getAddresses().isEmpty()
                ? customer.getAddresses().get(0)
                : null;

        return UrbanPiperOrderRequest.Customer.builder()
                .name(customer.getName())
                .email(customer.getEmail())
                .phoneNumber(customer.getMobile())
                .address(buildAddress(address))
                .build();
    }

    private UrbanPiperOrderRequest.Address buildAddress(Address address) {
        if (address == null) {
            return UrbanPiperOrderRequest.Address.builder().build();
        }

        return UrbanPiperOrderRequest.Address.builder()
                .line1(address.getAddressLine1())
                .line2(address.getAddressLine2())
                .city(address.getCity())
                .pincode(address.getPincode())
                .country("India")
                .landmark(address.getLandmark())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .instructions(address.getDeliveryInstructions())
                .build();
    }

    private List<UrbanPiperOrderRequest.Item> buildItems(Order order) {
        if (order.getOrderItems() == null) {
            return new ArrayList<>();
        }

        return order.getOrderItems().stream()
                .map(orderItem -> UrbanPiperOrderRequest.Item.builder()
                        .refId(orderItem.getItemId())
                        .title(orderItem.getName())
                        .quantity(orderItem.getQuantity())
                        .pricePerUnit(orderItem.getPrice())
                        .subtotal(orderItem.getPrice() * orderItem.getQuantity())
                        .total(orderItem.getFinalPrice())
                        .discount(orderItem.getDiscountAmount())
                        .instructions(orderItem.getSpecialInstructions())
                        .addons(buildAddons(orderItem))
                        .taxes(buildItemTaxes(orderItem))
                        .discounts(new ArrayList<>())
                        .charges(new ArrayList<>())
                        .variants(new ArrayList<>())
                        .build())
                .collect(Collectors.toList());
    }

    private List<UrbanPiperOrderRequest.Addon> buildAddons(Order.OrderItem orderItem) {
        if (orderItem.getOrderAddonItems() == null) {
            return new ArrayList<>();
        }

        return orderItem.getOrderAddonItems().stream()
                .map(addon -> UrbanPiperOrderRequest.Addon.builder()
                        .refId(addon.getAddonItemId())
                        .title(addon.getAddonItemName())
                        .pricePerUnit(addon.getPrice())
                        .quantity(addon.getQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    private List<UrbanPiperOrderRequest.Tax> buildItemTaxes(Order.OrderItem orderItem) {
        if (orderItem.getOrderItemTaxes() == null) {
            return new ArrayList<>();
        }

        return orderItem.getOrderItemTaxes().stream()
                .map(tax -> UrbanPiperOrderRequest.Tax.builder()
                        .title(tax.getName())
                        .value(tax.getAmount())
                        .percentage(tax.getRate())
                        .liabilityOn("merchant")
                        .build())
                .collect(Collectors.toList());
    }

    private List<UrbanPiperOrderRequest.Discount> buildOrderLevelDiscounts(Order order) {
        if (order.getOrderDiscount() == null) {
            return new ArrayList<>();
        }

        return order.getOrderDiscount().stream()
                .map(discount -> UrbanPiperOrderRequest.Discount.builder()
                        .title(discount.getName())
                        .code(discount.getId())
                        .value(discount.getAmount())
                        .type("fixed")
                        .merchantSponsored(true)
                        .build())
                .collect(Collectors.toList());
    }

    private UrbanPiperOrderRequest.Meta buildMeta(Order order, Restaurant restaurant) {
        Long createdTimestamp = order.getCreatedAt() != null
                ? order.getCreatedAt().atZone(ZoneId.systemDefault()).toEpochSecond()
                : System.currentTimeMillis() / 1000;

        return UrbanPiperOrderRequest.Meta.builder()
                .orderNo(order.getId())
                .restaurantName(restaurant.getRestaurantName())
                .locationRefId(restaurant.getSourceId())
                .currentStatus("placed")
                .fulfillmentMode(getFulfillmentMode(order))
                .instructions(order.getSpecialInstructions())
                .created(createdTimestamp)
                .subTotal(order.getItemTotalAmount())
                .total(order.getGrandTotalAmount())
                .totalCharges(order.getDeliveryCharge() + order.getServiceCharge() + order.getPackagingCharge())
                .totalDiscount(order.getDiscountAmount())
                .totalTaxes(order.getTaxAmount())
                .itemLevelCharges(0.0)
                .itemLevelDiscount(order.getDiscountAmount())
                .itemLevelTaxes(order.getTaxAmount())
                .orderLevelCharges(order.getDeliveryCharge() + order.getServiceCharge() + order.getPackagingCharge())
                .orderLevelDiscount(0.0)
                .isEdit(false)
                .isInstantOrder(true)
                .discountCode("")
                .charges(buildCharges(order))
                .prepTimeDetails(buildPrepTimeDetails())
                .build();
    }

    private String getFulfillmentMode(Order order) {
        if (order.getOrderType() == null) {
            return "delivery";
        }
        return order.getOrderType().equalsIgnoreCase("H") ? "delivery" : "pickup";
    }

    private List<UrbanPiperOrderRequest.Charge> buildCharges(Order order) {
        List<UrbanPiperOrderRequest.Charge> charges = new ArrayList<>();

        if (order.getDeliveryCharge() > 0) {
            charges.add(UrbanPiperOrderRequest.Charge.builder()
                    .title("Delivery charge")
                    .value(order.getDeliveryCharge())
                    .taxes(new ArrayList<>())
                    .build());
        }

        if (order.getServiceCharge() > 0) {
            charges.add(UrbanPiperOrderRequest.Charge.builder()
                    .title("Service charge")
                    .value(order.getServiceCharge())
                    .taxes(new ArrayList<>())
                    .build());
        }

        if (order.getPackagingCharge() > 0) {
            charges.add(UrbanPiperOrderRequest.Charge.builder()
                    .title("Packaging charge")
                    .value(order.getPackagingCharge())
                    .taxes(new ArrayList<>())
                    .build());
        }

        return charges;
    }

    private UrbanPiperOrderRequest.PrepTimeDetails buildPrepTimeDetails() {
        return UrbanPiperOrderRequest.PrepTimeDetails.builder()
                .predictedPrepTime(15)
                .maxIncreaseThreshold(3)
                .maxDecreaseThreshold(5)
                .build();
    }

    private UrbanPiperOrderRequest.Payment buildPayment(Order order) {
        String paymentMode = order.getPaymentType() != null
                ? order.getPaymentType().name().toLowerCase()
                : "online";

        return UrbanPiperOrderRequest.Payment.builder()
                .amountPaid(order.getGrandTotalAmount())
                .amountBalance(0.0)
                .mode(paymentMode)
                .status("paid")
                .build();
    }
}
