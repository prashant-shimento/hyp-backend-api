package com.hyp.adapter.urbanpiper.order;

import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.service.AddressService;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrbanPiperOrderTransformer {

    private final AddressService addressService;

    public UrbanPiperOrderRequest transform(Order order, Customer customer, Restaurant restaurant) {
        return UrbanPiperOrderRequest.builder()
                .customer(buildCustomer(customer, order))
                .items(buildItems(order))
                .meta(buildMeta(order, restaurant))
                .discounts(buildOrderLevelDiscounts(order))
                .payment(buildPayment(order))
                .build();
    }

    private UrbanPiperOrderRequest.Customer buildCustomer(Customer customer, Order order) {
        // Fetch address from order's delivery details
        Address address = null;
        if (order.getDeliveryDetails() != null && order.getDeliveryDetails().getAddressId() != null) {
            try {
                address = addressService.findById(order.getDeliveryDetails().getAddressId());
            } catch (Exception e) {
                log.warn("Failed to fetch address for order {}: {}", order.getId(), e.getMessage());
            }
        }

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
                .line1(address.getAddressOne())
                .line2(address.getAddressTwo())
                .city(address.getCity())
                .pincode(address.getPincode())
                .country("India")
                .landmark(address.getLandmark())
                .latitude(address.getLocation() != null ? address.getLocation().getLatitude() : null)
                .longitude(address.getLocation() != null ? address.getLocation().getLongitude() : null)
                .instructions(null)
                .build();
    }

    private List<UrbanPiperOrderRequest.Item> buildItems(Order order) {
        if (order.getOrderItems() == null) {
            return new ArrayList<>();
        }

        return order.getOrderItems().stream()
                .map(orderItem -> UrbanPiperOrderRequest.Item.builder()
                        .refId(orderItem.getId())
                        .title(orderItem.getName())
                        .quantity(orderItem.getQuantity())
                        .pricePerUnit(orderItem.getPrice())
                        .subtotal(orderItem.getPrice() * orderItem.getQuantity())
                        .total(orderItem.getFinalPrice())
                        .discount(orderItem.getItemDiscount())
                        .instructions(null)
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
        if (orderItem.getOrderItemTax() == null) {
            return new ArrayList<>();
        }

        return orderItem.getOrderItemTax().stream()
                .map(tax -> UrbanPiperOrderRequest.Tax.builder()
                        .title(tax.getName())
                        .value(tax.getAmount())
                        .percentage(0.0)
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
                        .title(discount.getTitle())
                        .code(discount.getId())
                        .value(Double.parseDouble(discount.getPrice()))
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
        String paymentMode =
                order.getPaymentType() != null ? order.getPaymentType().name().toLowerCase() : "online";

        return UrbanPiperOrderRequest.Payment.builder()
                .amountPaid(order.getGrandTotalAmount())
                .amountBalance(0.0)
                .mode(paymentMode)
                .status("paid")
                .build();
    }
}
