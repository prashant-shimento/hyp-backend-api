package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hyp.entity.Restaurant.DeliveryHours;
import com.hyp.entity.Restaurant.RestaurantTax;
import com.hyp.model.Location;
import com.hyp.model.PreOrder;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestaurantPublicDto {

    private String id;
    private String restaurantName;
    private boolean active;
    private String currencyHtml;
    private String country;
    private String city;
    private String state;
    private String pincode;
    private List<String> images;
    private String logoUrl;
    private String websiteUrl;
    private String address;
    private String landmark;
    private String contact;
    private Location location;
    private String embedMapUrl;

    // Ordering info
    private String minimumOrderAmount;
    private String minimumDeliveryTime;
    private String deliveryCharge;
    private List<DeliveryHours> deliveryHours;
    private Double deliveryRadius;
    private String packagingCharge;
    private String packagingChargeType;
    private String packagingApplicableOn;
    private RestaurantTax tax;
    private Integer calculateTaxOnDelivery;
    private Double discountPercentage;

    // Availability
    private Boolean serviceable;
    private String serviceableMessage;
    private Boolean showCustomMessage;
    private String customMessage;
    private PreOrder preOrderConfig;
    private Boolean discoverable;
    private boolean launched;
}
