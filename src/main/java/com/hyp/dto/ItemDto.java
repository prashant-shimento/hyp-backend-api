package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.hyp.entity.Tax;
import com.hyp.enums.OfferType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@JsonInclude(Include.NON_NULL)
public class ItemDto extends BaseDto {

    @NotBlank(message = "Item name cannot be empty")
    private String itemName;

    @NotBlank(message = "Item attribute ID cannot be empty")
    private String itemAttributeId;

    @NotBlank(message = "Item category ID cannot be empty")
    private String itemCategoryId;

    private List<Tax> taxes;

    private String itemDescription;
    private String itemRank;
    private String itemAllowAddon;
    private String variationGroupName;
    private List<String> addon;
    private String itemFavorite;

    @NotEmpty(message = "Taxes cannot be empty")
    private List<String> itemTax;

    private Boolean inStock;
    private String itemAllowVariation;
    private List<String> variation;
    private String itemPackingCharges;
    private String ignoreTaxes;

    @NotBlank(message = "Item price cannot be empty")
    private String price;

    private List<String> itemOrderType;
    private String minimumPreparationTime;
    private String itemAddonBasedOn;
    private String itemImageUrl;
    private List<String> cuisine;
    private String active = "1";
    private String ignoreDiscounts;
    private String isRecommend;
    private String gstType;
    private Boolean offerEnabled;
    private OfferType offerType;
    private Double offerValue;
}
