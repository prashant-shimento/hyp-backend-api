package com.hyp.translation;

import com.hyp.delivery.adloggs.AdloggsCreateOrderRequest;
import com.hyp.delivery.adloggs.AdloggsCreateOrderRequest.AddressDetails;
import com.hyp.delivery.adloggs.AdloggsCreateOrderRequest.OrderItem;
import com.hyp.delivery.adloggs.AdloggsServiceAvailabilityRequest;
import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.util.CommonUtils;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdloggsRequestTranslation {

    private static final DateTimeFormatter ADLOGGS_DT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static AdloggsServiceAvailabilityRequest getServiceAvailabilityRequest(
            Restaurant restaurant, Address address) {
        return AdloggsServiceAvailabilityRequest.builder()
                .partnerOrderId(CommonUtils.generateReferenceId("AQ"))
                .pickupLat(restaurant.getLocation().getLatitude())
                .pickupLong(restaurant.getLocation().getLongitude())
                .pickupPincode(restaurant.getPincode())
                .deliveryLat(address.getLocation().getLatitude())
                .deliveryLong(address.getLocation().getLongitude())
                .deliveryPincode(address.getPincode())
                .build();
    }

    public static AdloggsCreateOrderRequest getCreateOrderRequest(
            Restaurant restaurant, Address address, Customer customer, Order order) {

        AddressDetails pickupDetails = AddressDetails.builder()
                .cityName(restaurant.getCity())
                .stateName(restaurant.getState())
                .countryName(restaurant.getCountry())
                .pincode(restaurant.getPincode())
                .build();

        AddressDetails deliveryDetails = AddressDetails.builder()
                .streetName(address.getAddressOne())
                .cityName(address.getCity())
                .stateName(address.getState())
                .countryName(address.getCountry())
                .pincode(address.getPincode())
                .build();

        List<OrderItem> items = order.getOrderItems().stream()
                .map(i -> OrderItem.builder()
                        .name(i.getName())
                        .quantity(String.valueOf(i.getQuantity()))
                        .price(String.valueOf(i.getPrice()))
                        .build())
                .toList();

        return AdloggsCreateOrderRequest.builder()
                .partnerOrderId(order.getId())
                .pickupContactName(restaurant.getRestaurantName())
                .pickupContactNo(restaurant.getContact())
                .pickupAddress(restaurant.getAddress())
                .pickupAddressDetails(pickupDetails)
                .pickupDateTime(ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                        .plusMinutes(order.getMinPrepTime() != null ? Integer.parseInt(order.getMinPrepTime()) : 15)
                        .format(ADLOGGS_DT_FORMAT))
                .pickupLat(restaurant.getLocation().getLatitude())
                .pickupLong(restaurant.getLocation().getLongitude())
                .deliveryContactName(customer.getName())
                .deliveryContactNo(customer.getMobile())
                .deliveryAddress(address.getAddressOne())
                .deliveryAddressDetails(deliveryDetails)
                .deliveryLat(address.getLocation().getLatitude())
                .deliveryLong(address.getLocation().getLongitude())
                .orderTotalPrice(String.valueOf(order.getTotalAmount()))
                .items(items)
                .orderDescription("Order #" + order.getId())
                .build();
    }

    private AdloggsRequestTranslation() {}
}
