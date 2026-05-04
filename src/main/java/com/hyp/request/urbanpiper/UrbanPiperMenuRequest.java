package com.hyp.request.urbanpiper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UrbanPiperMenuRequest {

    @JsonProperty("order_bill_components")
    private OrderBillComponents orderBillComponents;

    private Menu menu;

    private List<Timing> timings;

    private Location location;

    @JsonProperty("bill_components")
    private BillComponents billComponents;

    @JsonProperty("callback_url")
    private String callbackUrl;

    // -------------------------

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderBillComponents {
        private List<String> charges;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Menu {
        private List<Category> categories;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Category {

        private List<SubCategory> subcategories;
        private String description;
        private String title;
        private List<Translation> translations;
        private List<Item> items;
        private String timings;

        @JsonProperty("ref_id")
        private String refId;

        @JsonProperty("sort_order")
        private Integer sortOrder;

        @JsonProperty("image_url")
        private String imageUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubCategory {

        private String description;
        private String title;
        private List<Translation> translations;
        private List<Item> items;

        @JsonProperty("ref_id")
        private String refId;

        @JsonProperty("sort_order")
        private Integer sortOrder;

        @JsonProperty("image_url")
        private String imageUrl;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {

        private String description;
        private String title;
        private List<Translation> translations;
        private List<Object> discounts;

        @JsonProperty("fulfillment_modes")
        private List<Object> fulfillmentModes;

        private List<String> tags;

        @JsonProperty("nutritional_info")
        private NutritionalInfo nutritionalInfo;

        private Boolean recommended;

        @JsonProperty("ref_id")
        private String refId;

        @JsonProperty("variant_groups")
        private List<VariantGroup> variantGroups;

        @JsonProperty("category_ref_id")
        private String categoryRefId;

        @JsonProperty("canonical_id")
        private String canonicalId;

        @JsonProperty("image_url")
        private String imageUrl;

        @JsonProperty("bill_components")
        private BillComponentRef billComponents;

        @JsonProperty("food_type")
        private Integer foodType;

        @JsonProperty("in_stock")
        private Boolean inStock;

        private Object price;

        @JsonProperty("sub_category_ref_id")
        private String subCategoryRefId;

        @JsonProperty("add_on_groups")
        private List<AddOnGroup> addOnGroups;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VariantGroup {

        private List<Variant> variants;
        private String title;
        private List<Translation> translations;

        @JsonProperty("ref_id")
        private String refId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Variant {

        private Integer price;
        private String title;
        private List<Translation> translations;

        @JsonProperty("in_stock")
        private Boolean inStock;

        @JsonProperty("food_type")
        private Integer foodType;

        @JsonProperty("ref_id")
        private String refId;

        @JsonProperty("nutritional_info")
        private NutritionalInfo nutritionalInfo;

        @JsonProperty("variant_groups")
        private List<VariantGroup> variantGroups;

        @JsonProperty("add_on_groups")
        private List<AddOnGroup> addOnGroups;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddOnGroup {

        @JsonProperty("maximum_allowed")
        private Integer maximumAllowed;

        private List<AddOn> addons;
        private String title;
        private List<Translation> translations;

        @JsonProperty("minimum_needed")
        private Integer minimumNeeded;

        @JsonProperty("ref_id")
        private String refId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AddOn {

        private Integer price;
        private String title;

        @JsonProperty("nutritional_info")
        private NutritionalInfo nutritionalInfo;

        private List<Translation> translations;

        @JsonProperty("in_stock")
        private Boolean inStock;

        @JsonProperty("food_type")
        private Integer foodType;

        @JsonProperty("ref_id")
        private String refId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Translation {

        private String language;
        private String title;
        private String description;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NutritionalInfo {

        private Nutrient carbohydrate;
        private Nutrient fiber;
        private Nutrient fat;
        private Nutrient protein;
        private Nutrient calorie;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Nutrient {

        private String unit;
        private Double value;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BillComponentRef {

        private List<String> charges;
        private List<String> taxes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Timing {

        private List<Day> days;

        @JsonProperty("ref_id")
        private String refId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Day {

        private List<Slot> slots;
        private String day;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Slot {

        @JsonProperty("start_time")
        private String startTime;

        @JsonProperty("end_time")
        private String endTime;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {

        @JsonProperty("min_delivery_time")
        private String minDeliveryTime;

        @JsonProperty("min_pickup_time")
        private String minPickupTime;

        @JsonProperty("ref_id")
        private String refId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BillComponents {

        private List<Charge> charges;
        private List<Tax> taxes;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Charge {

        private String description;
        private String title;

        @JsonProperty("fulfillment_modes")
        private List<String> fulfillmentModes;

        private Integer value;

        @JsonProperty("ref_id")
        private String refId;

        private List<String> taxes;
        private String type;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tax {

        private String title;
        private String description;
        private Double value;

        @JsonProperty("ref_id")
        private String refId;
    }
}
