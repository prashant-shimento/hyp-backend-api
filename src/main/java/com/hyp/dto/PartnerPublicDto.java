package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hyp.enums.PartnerType;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class PartnerPublicDto {

    private String id;
    private String name;
    private PartnerType type;
    private String domain;
    private String logoUrl;
    private String webUrl;
    private String about;
    private String description;
    private String headerImageUrls;
    private List<String> galleryImageUrl;
    private String themeColor;
    private Map<String, String> socialMediaHandlesLink;
    private String officeAddress;

    // Only populated for /storefront endpoint — null in /discovery listing
    private List<RestaurantDto> restaurantDetails;
}
