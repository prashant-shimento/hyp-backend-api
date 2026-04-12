package com.hyp.translation;

import com.hyp.constants.Constants;
import com.hyp.entity.AddonGroup;
import com.hyp.entity.AddonItem;
import com.hyp.entity.Attribute;
import com.hyp.entity.Category;
import com.hyp.entity.Discount;
import com.hyp.entity.Item;
import com.hyp.entity.OrderType;
import com.hyp.entity.Restaurant;
import com.hyp.entity.Restaurant.DeliveryHours;
import com.hyp.entity.Restaurant.RestaurantTax;
import com.hyp.entity.Tax;
import com.hyp.entity.Variation;
import com.hyp.enums.DeliveryModel;
import com.hyp.model.Location;
import com.hyp.model.PosData;
import com.hyp.request.PosDataRequest;
import com.hyp.request.PosDataRequest.AddonGroupRequest;
import com.hyp.request.PosDataRequest.AddonItemRequest;
import com.hyp.request.PosDataRequest.AttributeRequest;
import com.hyp.request.PosDataRequest.CategoryRequest;
import com.hyp.request.PosDataRequest.DiscountRequest;
import com.hyp.request.PosDataRequest.ItemRequest;
import com.hyp.request.PosDataRequest.OrderTypeRequest;
import com.hyp.request.PosDataRequest.RestaurantRequest;
import com.hyp.request.PosDataRequest.TaxRequest;
import com.hyp.request.PosDataRequest.VariationRequest;
import com.hyp.util.CommonUtils;
import com.hyp.util.ValidationUtils;
import io.micrometer.common.util.StringUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PosDataRequestTranslation {

    public static PosData getPosData(PosDataRequest posDataRequest, List<Item> existingItem, List<Tax> existingTax) {
        return PosData.builder()
                .orderTypes(PosDataRequestTranslation.translateToOrderTypeList(posDataRequest.getOrdertypes()))
                .attributes(PosDataRequestTranslation.translateToAttributeList(posDataRequest.getAttributes()))
                .discounts(PosDataRequestTranslation.translateToDiscountList(posDataRequest.getDiscounts()))
                .categories(PosDataRequestTranslation.translateToCategoryList(posDataRequest.getCategories()))
                .taxes(PosDataRequestTranslation.translateToTaxList(posDataRequest.getTaxes(), existingTax))
                .addonItems(PosDataRequestTranslation.getUniqueAddonItemList(posDataRequest.getAddongroups()))
                .addonGroups(PosDataRequestTranslation.translateToAddonGroupByItemList(
                        posDataRequest.getAddongroups(), posDataRequest.getItems()))
                .variations(PosDataRequestTranslation.translateToVariationList(
                        posDataRequest.getVariations(), posDataRequest.getItems()))
                .items(PosDataRequestTranslation.translateToItemList(posDataRequest.getItems(), existingItem))
                .build();
    }

    public static Attribute translateToAttribute(AttributeRequest attributeRequest) {
        if (attributeRequest == null) {
            return null;
        }
        Attribute attribute = new Attribute();
        attribute.setId(attributeRequest.getAttributeid());
        attribute.setAttribute(attributeRequest.getAttribute());
        attribute.setActive(attributeRequest.getActive());
        return attribute;
    }

    public static List<Attribute> translateToAttributeList(List<AttributeRequest> attributeRequestList) {
        return attributeRequestList == null
                ? Collections.emptyList()
                : attributeRequestList.stream()
                        .map(PosDataRequestTranslation::translateToAttribute)
                        .collect(Collectors.toList());
    }

    public static OrderType translateToOrderType(OrderTypeRequest orderTypeRequest) {
        if (orderTypeRequest == null) {
            return null;
        }
        OrderType orderType = new OrderType();
        orderType.setId(CommonUtils.genId());
        orderType.setOrderTypeId(orderTypeRequest.getOrdertypeid());
        orderType.setOrderType(orderTypeRequest.getOrdertype());
        return orderType;
    }

    public static List<OrderType> translateToOrderTypeList(List<OrderTypeRequest> orderTypeRequestList) {
        return orderTypeRequestList == null
                ? Collections.emptyList()
                : orderTypeRequestList.stream()
                        .map(PosDataRequestTranslation::translateToOrderType)
                        .collect(Collectors.toList());
    }

    public static Tax translateToTax(TaxRequest taxRequest, Tax existingTax) {
        if (taxRequest == null) {
            return null;
        }
        Tax tax = new Tax();
        tax.setId(taxRequest.getTaxid());

        if (existingTax != null && existingTax.getTaxName() != null) {
            tax.setTaxName(existingTax.getTaxName());
            tax.setTax(resolveTax(taxRequest.getTax()));
        } else {
            String incomingTaxName = taxRequest.getTaxname();
            boolean isValidTax = "CGST".equalsIgnoreCase(incomingTaxName) || "SGST".equalsIgnoreCase(incomingTaxName);
            tax.setTaxName(isValidTax ? incomingTaxName : "CGST");
            tax.setTax(resolveTax(taxRequest.getTax()));
        }

        tax.setTaxType(taxRequest.getTaxtype());
        tax.setTaxCoreOrTotal(taxRequest.getTax_coreortotal());
        tax.setTaxTaxType(taxRequest.getTax_taxtype());
        tax.setTaxOrderType(taxRequest.getTax_ordertype());
        tax.setRank(taxRequest.getRank());
        tax.setDescription(taxRequest.getDescription());
        tax.setConsiderInCoreAmount(taxRequest.getConsider_in_core_amount());
        tax.setActive(taxRequest.getActive());
        return tax;
    }

    private static String resolveTax(String taxValue) {
        try {
            if (taxValue != null && !taxValue.trim().isEmpty()) {
                BigDecimal value = new BigDecimal(taxValue.trim());
                if (value.compareTo(BigDecimal.ZERO) > 0) {
                    return taxValue.trim();
                }
            }
        } catch (NumberFormatException e) {
            // non-numeric → fallback
        }
        return "5.00";
    }

    public static List<Tax> translateToTaxList(List<TaxRequest> taxRequestList, List<Tax> existingTaxes) {
        if (taxRequestList == null) {
            return Collections.emptyList();
        }

        List<Tax> mergedTaxes = new ArrayList<>();
        for (TaxRequest request : taxRequestList) {
            Tax existingTax = null;
            if (existingTaxes != null) {
                existingTax = existingTaxes.stream()
                        .filter(tax -> tax.getId().equals(request.getTaxid()))
                        .findFirst()
                        .orElse(null);
            }
            mergedTaxes.add(translateToTax(request, existingTax));
        }
        return mergedTaxes;
    }

    public static Variation translateToVariation(VariationRequest variationRequest) {
        if (variationRequest == null) {
            return null;
        }
        Variation variation = new Variation();
        variation.setId(variationRequest.getId());
        variation.setName(variationRequest.getName());
        variation.setGroupName(variationRequest.getGroupname());
        variation.setStatus(variationRequest.getStatus());
        variation.setPrice(variationRequest.getPrice());
        variation.setActive(variationRequest.getActive());
        variation.setItemPackingCharges(variationRequest.getItem_packingcharges());
        variation.setVariationRank(variationRequest.getVariationrank());
        variation.setVariationAllowAddon(variationRequest.getVariationallowaddon());
        variation.setVariationId(variationRequest.getVariationid());
        List<String> addonGroupIds = new ArrayList<>();
        if (variationRequest.getVariationallowaddon() == 1) {
            addonGroupIds = variationRequest.getAddon() == null
                    ? Collections.emptyList()
                    : variationRequest.getAddon().stream()
                            .map(AddonGroupRequest::getAddon_group_id)
                            .collect(Collectors.toList());
        }
        variation.setAddonGroupId(addonGroupIds);
        variation.setSourceId(
                variationRequest.getSourceId() != null ? variationRequest.getSourceId() : variationRequest.getId());
        return variation;
    }

    public static List<Variation> translateToVariationList(
            List<VariationRequest> variationRequestList, List<ItemRequest> itemList) {

        List<VariationRequest> itemVariationRequestList = itemList.stream()
                .filter(item -> "1".equalsIgnoreCase(item.getItemallowvariation()) && item.getVariation() != null)
                .flatMap(item -> item.getVariation().stream())
                .sorted(Comparator.comparing(VariationRequest::getVariationid))
                .toList();

        variationRequestList.sort(Comparator.comparing(VariationRequest::getVariationid));

        for (VariationRequest itemVariationRequest : itemVariationRequestList) {
            for (VariationRequest variationRequest : variationRequestList) {
                if (variationRequest.getVariationid().equals(itemVariationRequest.getVariationid())) {
                    itemVariationRequest.setStatus(variationRequest.getStatus());
                    itemVariationRequest.setActive(variationRequest.getStatus());
                    break;
                }
            }
        }

        return itemVariationRequestList.stream()
                .map(PosDataRequestTranslation::translateToVariation)
                .collect(Collectors.toList());
    }

    public static List<AddonGroup> translateToAddonGroupByItemList(
            List<AddonGroupRequest> addonGroupRequestList, List<ItemRequest> itemList) {

        Set<AddonGroupRequest> itemAddonGroupRequestSet = itemList.stream()
                .filter(item -> "1".equalsIgnoreCase(item.getItemallowaddon()) && item.getAddon() != null)
                .flatMap(item -> item.getAddon().stream())
                .sorted(Comparator.comparing(AddonGroupRequest::getAddon_group_id))
                .collect(Collectors.toSet());

        Set<AddonGroupRequest> itemVariationAddonGroupRequestSet = itemList.stream()
                .filter(item -> "1".equalsIgnoreCase(item.getItemallowvariation()) && item.getVariation() != null)
                .flatMap(item -> item.getVariation().stream())
                .flatMap(variation -> variation.getAddon().stream())
                .sorted(Comparator.comparing(AddonGroupRequest::getAddon_group_id))
                .collect(Collectors.toSet());

        Set<AddonGroupRequest> mergedItemAddonGroupRequestSet = new HashSet<>();
        mergedItemAddonGroupRequestSet.addAll(itemAddonGroupRequestSet);
        mergedItemAddonGroupRequestSet.addAll(itemVariationAddonGroupRequestSet);

        addonGroupRequestList.sort(Comparator.comparing(AddonGroupRequest::getAddongroupid));

        for (AddonGroupRequest itemAddonGroupRequest : mergedItemAddonGroupRequestSet) {
            for (AddonGroupRequest addonGroupRequest : addonGroupRequestList) {
                if (addonGroupRequest.getAddongroupid().equals(itemAddonGroupRequest.getAddon_group_id())) {
                    addonGroupRequest.setAddon_item_selection_max(itemAddonGroupRequest.getAddon_item_selection_max());
                    addonGroupRequest.setAddon_item_selection_min(itemAddonGroupRequest.getAddon_item_selection_min());
                    break;
                }
            }
        }
        return addonGroupRequestList.stream()
                .map(PosDataRequestTranslation::translateToAddonGroup)
                .collect(Collectors.toList());
    }

    public static List<Variation> getUniqueVariationList(List<VariationRequest> variationRequestList) {
        return variationRequestList == null
                ? Collections.emptyList()
                : new ArrayList<>(variationRequestList.stream()
                        .map(PosDataRequestTranslation::translateToVariation)
                        .collect(Collectors.toSet()));
    }

    public static Restaurant translateToRestaurant(RestaurantRequest restaurantRequest, Restaurant existingRestaurant) {
        if (restaurantRequest == null) {
            return null;
        }
        Restaurant restaurant = existingRestaurant != null ? existingRestaurant : new Restaurant();
        restaurant.setId(restaurantRequest.getRestaurantid());
        restaurant.setActive(restaurantRequest.getActive().equalsIgnoreCase("1"));
        restaurant.setCurrencyHtml(restaurantRequest.getDetails().getCurrency_html());
        restaurant.setCountry(restaurantRequest.getDetails().getCountry());
        restaurant.setMinimumOrderAmount(restaurantRequest.getDetails().getMinimumorderamount());
        restaurant.setPackagingApplicableOn(restaurantRequest.getDetails().getPackaging_applicable_on());
        restaurant.setCity(restaurantRequest.getDetails().getCity());
        restaurant.setCalculateTaxOnDelivery(restaurantRequest.getDetails().getCalculatetaxondelivery());
        restaurant.setPackagingChargeType(restaurantRequest.getDetails().getPackaging_charge_type());
        restaurant.setState(restaurantRequest.getDetails().getState());
        restaurant.setLandmark(restaurantRequest.getDetails().getLandmark());
        restaurant.setImages(restaurantRequest.getDetails().getImages());
        restaurant.setMenuSharingCode(restaurantRequest.getDetails().getMenusharingcode());
        restaurant.setCalculateTaxOnPacking(restaurantRequest.getDetails().getCalculatetaxondelivery());
        restaurant.setDeliveryCharge(restaurantRequest.getDetails().getDeliverycharge());
        restaurant.setMinimumDeliveryTime(restaurantRequest.getDetails().getMinimumdeliverytime());
        restaurant.setTax(new RestaurantTax(
                restaurantRequest.getDetails().getDc_taxes_id(),
                restaurantRequest.getDetails().getPc_taxes_id()));

        if (existingRestaurant != null && existingRestaurant.getRestaurantName() != null) {
            restaurant.setRestaurantName(existingRestaurant.getRestaurantName());
        } else {
            restaurant.setRestaurantName(restaurantRequest.getDetails().getRestaurantname());
        }
        if (existingRestaurant != null && existingRestaurant.getFssai() != null) {
            restaurant.setFssai(existingRestaurant.getFssai());
        } else {
            restaurant.setFssai("");
        }
        if (existingRestaurant != null && existingRestaurant.getAddress() != null) {
            restaurant.setAddress(existingRestaurant.getAddress());
        } else {
            restaurant.setAddress(restaurantRequest.getDetails().getAddress());
        }

        if (existingRestaurant != null && existingRestaurant.getLocation() != null) {
            restaurant.setLocation(existingRestaurant.getLocation());
        } else {
            if (restaurantRequest.getDetails().getLatitude() != null
                    && restaurantRequest.getDetails().getLongitude() != null) {
                restaurant.setLocation(new Location(
                        Double.valueOf(restaurantRequest.getDetails().getLatitude()),
                        Double.valueOf(restaurantRequest.getDetails().getLongitude())));
            }
        }
        if (existingRestaurant != null && existingRestaurant.getDeliveryHours() != null) {
            restaurant.setDeliveryHours(existingRestaurant.getDeliveryHours());
        } else {
            restaurant.setDeliveryHours(getDeliveryHours(restaurantRequest));
        }
        if (existingRestaurant != null && existingRestaurant.getDeliveryRadius() != 0) {
            restaurant.setDeliveryRadius(existingRestaurant.getDeliveryRadius());
        } else {
            restaurant.setDeliveryRadius(10.0);
        }
        if (existingRestaurant != null && !StringUtils.isEmpty(existingRestaurant.getPincode())) {
            restaurant.setPincode(existingRestaurant.getPincode());
        } else {
            restaurant.setPincode(
                    CommonUtils.extractPincode(restaurantRequest.getDetails().getAddress()));
        }
        if (existingRestaurant != null && existingRestaurant.getDeliveryModel() != null) {
            restaurant.setDeliveryModel(existingRestaurant.getDeliveryModel());
        } else {
            restaurant.setDeliveryModel(DeliveryModel.PARTNER);
        }
        if (existingRestaurant != null && existingRestaurant.getContact() != null) {
            restaurant.setContact(existingRestaurant.getContact());
        } else {
            restaurant.setContact(restaurantRequest.getDetails().getContact());
        }
        if (existingRestaurant != null && existingRestaurant.getLogoUrl() != null) {
            restaurant.setLogoUrl(existingRestaurant.getLogoUrl());
        } else {
            restaurant.setLogoUrl("https://storage.googleapis.com/hyp-app-bucket/default-logo.png");
        }
        if (existingRestaurant != null && existingRestaurant.getScreens() != null) {
            restaurant.setScreens(existingRestaurant.getScreens());
        } else {
            restaurant.setScreens(null);
        }
        if (existingRestaurant != null && existingRestaurant.getWebsiteUrl() != null) {
            restaurant.setWebsiteUrl(existingRestaurant.getWebsiteUrl());
        } else {
            restaurant.setWebsiteUrl(null);
        }

        if (existingRestaurant != null && existingRestaurant.getSourceId() != null) {
            restaurant.setSourceId(existingRestaurant.getSourceId());
        } else {
            restaurant.setSourceId(
                    restaurantRequest.getSourceId() != null ? restaurantRequest.getSourceId() : restaurant.getId());
        }

        if (existingRestaurant != null && existingRestaurant.getIngestionSource() != null) {
            restaurant.setIngestionSource(existingRestaurant.getIngestionSource());
        } else {
            restaurant.setIngestionSource(
                    restaurantRequest.getIngestionSource() != null
                            ? restaurantRequest.getIngestionSource()
                            : Constants.PET_POOJA);
        }
        
        // Set posPartner based on ingestionSource
        if (existingRestaurant != null && existingRestaurant.getPosPartner() != null) {
            restaurant.setPosPartner(existingRestaurant.getPosPartner());
        } else {
            if (restaurantRequest.getPosPartner() != null) {
                restaurant.setPosPartner(restaurantRequest.getPosPartner());
            } else {
                String ingestionSource = restaurant.getIngestionSource();
                if ("urbanpiper".equalsIgnoreCase(ingestionSource)) {
                    restaurant.setPosPartner(com.hyp.enums.PosPartner.URBAN_PIPER.name());
                } else if (Constants.PET_POOJA.equalsIgnoreCase(ingestionSource)) {
                    restaurant.setPosPartner(com.hyp.enums.PosPartner.PET_POOJA.name());
                } else {
                    restaurant.setPosPartner(com.hyp.enums.PosPartner.SELF.name());
                }
            }
        }
        
        String existingCharge = existingRestaurant != null ? existingRestaurant.getPackagingCharge() : null;
        if (existingCharge != null && !existingCharge.isEmpty()) {
            restaurant.setPackagingCharge(existingCharge);
        } else {
            restaurant.setPackagingCharge(restaurantRequest.getDetails().getPackaging_charge());
        }
        return restaurant;
    }

    public static List<DeliveryHours> getDeliveryHours(RestaurantRequest restaurantRequest) {
        List<DeliveryHours> deliveryHoursList = new ArrayList<>();
        String fromTime1 = restaurantRequest.getDetails().getDeliveryhoursfrom1();
        String toTime1 = restaurantRequest.getDetails().getDeliveryhoursto1();
        String fromTime2 = restaurantRequest.getDetails().getDeliveryhoursfrom2();
        String toTime2 = restaurantRequest.getDetails().getDeliveryhoursto2();

        deliveryHoursList.add(new DeliveryHours(
                ValidationUtils.validateTimeString(fromTime1), ValidationUtils.validateTimeString(toTime1)));
        deliveryHoursList.add(new DeliveryHours(
                ValidationUtils.validateTimeString(fromTime2), ValidationUtils.validateTimeString(toTime2)));

        return deliveryHoursList;
    }

    public static Category translateToCategory(CategoryRequest categoryRequest) {
        if (categoryRequest == null) {
            return null;
        }
        Category category = new Category();
        category.setId(categoryRequest.getCategoryid());
        category.setParentCategoryId(categoryRequest.getParent_category_id());
        category.setCategoryImageUrl(categoryRequest.getCategory_image_url());
        category.setCategoryTimings(categoryRequest.getCategorytimings());
        category.setActive(categoryRequest.getActive());
        category.setCategoryName(categoryRequest.getCategoryname());
        category.setCategoryRank(categoryRequest.getCategoryrank());
        category.setSourceId(
                category.getSourceId() != null ? categoryRequest.getSourceId() : categoryRequest.getCategoryid());
        return category;
    }

    public static List<Category> translateToCategoryList(List<CategoryRequest> categoryRequestList) {
        return categoryRequestList == null
                ? Collections.emptyList()
                : categoryRequestList.stream()
                        .map(PosDataRequestTranslation::translateToCategory)
                        .collect(Collectors.toList());
    }

    public static Discount translateToDiscount(DiscountRequest discountRequest) {
        if (discountRequest == null) {
            return null;
        }
        Discount discount = new Discount();
        discount.setId(discountRequest.getDiscountid());
        discount.setDiscountName(discountRequest.getDiscountname());
        discount.setDiscountType(discountRequest.getDiscounttype());
        discount.setDiscount(discountRequest.getDiscount());
        discount.setDiscountOrder(discountRequest.getDiscountorder());
        discount.setBogoApplicableOnPurchase(discountRequest.getBogoapplicableonpurchase());
        discount.setBogoApplicableOnPurchaseItemIds(discountRequest.getBogoapplicableonpurchaseitemids());
        discount.setBogoApplicableOn(discountRequest.getBogoapplicableon());
        discount.setBogoApplicableOnItemIds(discountRequest.getBogoapplicableonitemids());
        discount.setBogoItemAmountLimit(discountRequest.getBogoitemamountlimit());
        discount.setBogoPurchaseDiscount(discountRequest.getBogopurchasediscount());
        discount.setBogoApplicableOnItem(discountRequest.getBogoapplicableonitem());
        discount.setBogoApplicableOnPurchaseItem(discountRequest.getBogoapplicableonpurchaseitem());
        discount.setBogoBuyQty(discountRequest.getBogobuyqty());
        discount.setBogoGetQty(discountRequest.getBogogetqty());
        discount.setBogoType(discountRequest.getBogotype());
        discount.setDiscountOrderType(discountRequest.getDiscountordertype());
        discount.setDiscountApplicableOn(discountRequest.getDiscountapplicableon());
        discount.setDiscountDays(discountRequest.getDiscountdays());
        discount.setActive(discountRequest.getActive());
        discount.setDiscountOnTotal(discountRequest.getDiscountontotal());
        discount.setDiscountStarts(discountRequest.getDiscountstarts());
        discount.setDiscountEnds(discountRequest.getDiscountends());
        discount.setDiscountTimeFrom(discountRequest.getDiscounttimefrom());
        discount.setDiscountTimeTo(discountRequest.getDiscounttimeto());
        discount.setDiscountMinAmount(discountRequest.getDiscountminamount());
        discount.setDiscountMaxAmount(discountRequest.getDiscountmaxamount());
        discount.setDiscountHasCoupon(discountRequest.getDiscounthascoupon());
        discount.setDiscountCategoryItemIds(discountRequest.getDiscountcategoryitemids());
        discount.setDiscountMaxLimit(discountRequest.getDiscountmaxlimit());
        return discount;
    }

    public static List<Discount> translateToDiscountList(List<DiscountRequest> discountRequestList) {
        return discountRequestList == null
                ? Collections.emptyList()
                : discountRequestList.stream()
                        .map(PosDataRequestTranslation::translateToDiscount)
                        .collect(Collectors.toList());
    }

    public static AddonGroup translateToAddonGroup(AddonGroupRequest addonGroupRequest) {
        if (addonGroupRequest == null) {
            return null;
        }
        AddonGroup addonGroup = new AddonGroup();
        addonGroup.setId(addonGroupRequest.getAddongroupid());
        addonGroup.setAddonGroupName(addonGroupRequest.getAddongroup_name());
        addonGroup.setActive(addonGroupRequest.getActive());
        addonGroup.setAddonGroupRank(addonGroupRequest.getAddongroup_rank());
        addonGroup.setAddonGroupItems(getAddonItemIdList(addonGroupRequest.getAddongroupitems()));
        addonGroup.setAddonItemSelectionMax(addonGroupRequest.getAddon_item_selection_max());
        addonGroup.setAddonItemSelectionMin(addonGroupRequest.getAddon_item_selection_min());
        addonGroup.setSourceId(
                addonGroupRequest.getSourceId() != null
                        ? addonGroupRequest.getSourceId()
                        : addonGroupRequest.getAddongroupid());
        return addonGroup;
    }

    public static List<AddonGroup> translateToAddonGroupList(List<AddonGroupRequest> addonGroupRequestList) {
        return addonGroupRequestList == null
                ? Collections.emptyList()
                : addonGroupRequestList.stream()
                        .map(PosDataRequestTranslation::translateToAddonGroup)
                        .collect(Collectors.toList());
    }

    public static List<AddonGroup> getUniqueAddonGroupList(List<AddonGroupRequest> addonGroupRequestList) {
        return addonGroupRequestList == null
                ? Collections.emptyList()
                : new ArrayList<>(addonGroupRequestList.stream()
                        .map(PosDataRequestTranslation::translateToAddonGroup)
                        .collect(Collectors.toSet()));
    }

    public static AddonItem translateToAddonItem(AddonItemRequest addonItemRequest) {
        if (addonItemRequest == null) {
            return null;
        }
        AddonItem addonItem = new AddonItem();
        addonItem.setId(addonItemRequest.getAddonitemid());
        addonItem.setAddonItemName(addonItemRequest.getAddonitem_name());
        addonItem.setAddonItemPrice(addonItemRequest.getAddonitem_price());
        addonItem.setActive(addonItemRequest.getActive());
        addonItem.setAddonItemRank(addonItemRequest.getAddonitem_rank());
        addonItem.setAttributes(addonItemRequest.getAttributes());
        addonItem.setSourceId(
                addonItemRequest.getSourceId() != null
                        ? addonItemRequest.getSourceId()
                        : addonItemRequest.getAddonitemid());
        return addonItem;
    }

    public static List<String> getAddonItemIdList(List<AddonItemRequest> addonItemRequestList) {
        return addonItemRequestList == null
                ? Collections.emptyList()
                : addonItemRequestList.stream()
                        .map(AddonItemRequest::getAddonitemid)
                        .collect(Collectors.toList());
    }

    public static List<AddonItem> translateToAddonItemList(List<AddonItemRequest> addonItemRequestList) {
        return addonItemRequestList == null
                ? Collections.emptyList()
                : addonItemRequestList.stream()
                        .map(PosDataRequestTranslation::translateToAddonItem)
                        .collect(Collectors.toList());
    }

    public static List<AddonItem> getAddonItemList(List<AddonGroupRequest> addonGroupRequestList) {
        return addonGroupRequestList == null
                ? Collections.emptyList()
                : addonGroupRequestList.stream()
                        .flatMap(addonGroupRequest ->
                                translateToAddonItemList(addonGroupRequest.getAddongroupitems()).stream())
                        .collect(Collectors.toList());
    }

    public static List<AddonItem> getUniqueAddonItemList(List<AddonGroupRequest> addonGroupRequestList) {
        return addonGroupRequestList == null
                ? Collections.emptyList()
                : addonGroupRequestList.stream()
                        .flatMap(addonGroupRequest ->
                                translateToAddonItemList(addonGroupRequest.getAddongroupitems()).stream())
                        .distinct()
                        .collect(Collectors.toList());
    }

    public static Item translateToItem(ItemRequest itemRequest, Item existingItem) {
        if (itemRequest == null) {
            return null;
        }

        Item item = new Item();
        item.setId(itemRequest.getItemid());
        item.setItemDescription(itemRequest.getItemdescription());
        item.setItemRank(itemRequest.getItemrank());
        item.setItemAllowAddon(itemRequest.getItemallowaddon());
        item.setVariationGroupName(itemRequest.getVariation_groupname());
        item.setItemFavorite(itemRequest.getItem_favorite());
        item.setInStock(itemRequest.getIn_stock().equalsIgnoreCase("1"));
        item.setItemAllowVariation(itemRequest.getItemallowvariation());

        item.setVariation(getVariationIdList(itemRequest.getVariation()));

        item.setItemPackingCharges(itemRequest.getItem_packingcharges());
        item.setIgnoreTaxes(itemRequest.getIgnore_taxes());
        item.setPrice(getPrice(itemRequest));
        item.setMinimumPreparationTime(itemRequest.getMinimumpreparationtime());
        item.setItemAddonBasedOn(itemRequest.getItemaddonbasedon());
        item.setItemImageUrl(itemRequest.getItem_image_url());
        item.setItemName(itemRequest.getItemname());
        item.setCuisine(itemRequest.getCuisine());
        item.setActive(itemRequest.getActive());
        item.setIgnoreDiscounts(itemRequest.getIgnore_discounts());
        item.setItemAttributeId(itemRequest.getItem_attributeid());
        item.setIsRecommend(itemRequest.getIs_recommend());
        item.setGstType(itemRequest.getGst_type());

        item.setItemCategoryId(itemRequest.getItem_categoryid());
        List<String> taxIds = Arrays.asList(itemRequest.getItem_tax().split(","));
        item.setItemTax(taxIds);

        List<String> orderTypeIds =
                Arrays.asList(itemRequest.getItem_ordertype().split(","));
        item.setItemOrderType(orderTypeIds);

        item.setAddon(getAddonIdList(itemRequest.getAddon()));
        if (existingItem != null) {
            item.setOfferEnabled(Boolean.TRUE.equals(existingItem.getOfferEnabled()));
            item.setOfferType(existingItem.getOfferType());
            item.setOfferValue(existingItem.getOfferValue());
        } else {
            item.setOfferEnabled(false);
        }

        if (existingItem != null) {
            item.setSourceId(existingItem.getSourceId());
        } else {
            item.setSourceId(itemRequest.getSourceId() != null ? itemRequest.getSourceId() : itemRequest.getItemid());
        }
        return item;
    }

    public static List<String> getVariationIdList(List<VariationRequest> variationRequestLst) {
        return variationRequestLst == null
                ? Collections.emptyList()
                : variationRequestLst.stream().map(VariationRequest::getId).collect(Collectors.toList());
    }

    public static List<String> getAddonIdList(List<AddonGroupRequest> addonGroupList) {
        return addonGroupList == null
                ? Collections.emptyList()
                : addonGroupList.stream()
                        .map(AddonGroupRequest::getAddon_group_id)
                        .collect(Collectors.toList());
    }

    public static List<Item> translateToItemList(List<ItemRequest> itemRequestList, List<Item> existingItems) {
        List<Item> mergedItems = new ArrayList<>();
        for (ItemRequest req : itemRequestList) {
            Item existingItem = null;
            if (existingItems != null) {
                existingItem = existingItems.stream()
                        .filter(item -> item.getId().equals(req.getItemid()))
                        .findFirst()
                        .orElse(null);
            }
            mergedItems.add(translateToItem(req, existingItem));
        }
        return mergedItems;
    }

    private static String getPrice(ItemRequest itemRequest) {
        if ("0".equals(itemRequest.getPrice())
                || "0.0".equals(itemRequest.getPrice())
                || "1".equalsIgnoreCase(itemRequest.getItemallowvariation())) {
            return getLeastVariationPrice(itemRequest.getVariation(), itemRequest.getPrice());
        }
        return itemRequest.getPrice();
    }

    private static String getLeastVariationPrice(List<VariationRequest> variations, String itemPrice) {
        if (variations == null || variations.isEmpty()) {
            return itemPrice;
        }
        return String.valueOf(variations.stream()
                .filter(variation -> "1".equals(variation.getActive()))
                .mapToDouble(variation -> Double.parseDouble(variation.getPrice()))
                .min()
                .orElse(Double.parseDouble(itemPrice)));
    }
}
