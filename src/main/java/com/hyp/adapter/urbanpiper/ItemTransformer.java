package com.hyp.adapter.urbanpiper;

import com.hyp.request.PosDataRequest;
import com.hyp.request.urbanpiper.UrbanPiperMenuRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ItemTransformer {

    private final AddonGroupTransformer addonGroupTransformer;
    private final VariantTransformer variantTransformer;

    public ItemExtractionResult transform(
            UrbanPiperMenuRequest.Item item, String categoryRefId, String subCategoryRefId) {
        String itemRefId = item.getRefId();
        String itemTitle = item.getTitle();
        String itemDescription = item.getDescription();
        String itemPrice = item.getPrice() != null ? String.valueOf(item.getPrice()) : null;
        boolean inStock = item.getInStock() != null && item.getInStock();
        boolean recommended = item.getRecommended() != null && item.getRecommended();

        boolean hasVariants =
                item.getVariantGroups() != null && !item.getVariantGroups().isEmpty();

        List<PosDataRequest.AddonGroupRequest> itemAddonGroups = new ArrayList<>();
        if (item.getAddOnGroups() != null) {
            itemAddonGroups = addonGroupTransformer.transform(item.getAddOnGroups());
        }

        if (hasVariants) {
            return variantTransformer.processItemWithVariants(
                    item, itemRefId, itemTitle, itemDescription, categoryRefId, subCategoryRefId, itemAddonGroups);
        } else {
            return createSimpleItem(
                    item,
                    itemRefId,
                    itemTitle,
                    itemDescription,
                    itemPrice,
                    categoryRefId,
                    subCategoryRefId,
                    inStock,
                    recommended,
                    itemAddonGroups);
        }
    }

    private ItemExtractionResult createSimpleItem(
            UrbanPiperMenuRequest.Item item,
            String itemRefId,
            String itemTitle,
            String itemDescription,
            String itemPrice,
            String categoryRefId,
            String subCategoryRefId,
            boolean inStock,
            boolean recommended,
            List<PosDataRequest.AddonGroupRequest> itemAddonGroups) {

        PosDataRequest.ItemRequest itemRequest = new PosDataRequest.ItemRequest();
        itemRequest.setItemid(itemRefId);
        itemRequest.setItemname(itemTitle);
        itemRequest.setItemdescription(itemDescription);
        itemRequest.setPrice(itemPrice != null && !itemPrice.isEmpty() ? itemPrice : "0");
        itemRequest.setItem_categoryid(subCategoryRefId != null ? subCategoryRefId : categoryRefId);
        itemRequest.setIn_stock(inStock ? "1" : "0");
        itemRequest.setActive("1");
        itemRequest.setItem_ordertype("1,2,3");
        itemRequest.setItemallowvariation("0");
        itemRequest.setItemallowaddon(itemAddonGroups.isEmpty() ? "0" : "1");
        itemRequest.setItem_favorite(recommended ? "1" : "0");
        itemRequest.setIs_recommend(recommended ? "1" : "0");
        itemRequest.setItem_image_url(item.getImageUrl());
        itemRequest.setItemrank("0");
        itemRequest.setItem_packingcharges("0");
        itemRequest.setIgnore_taxes("0");
        itemRequest.setIgnore_discounts("0");
        itemRequest.setMinimumpreparationtime("0");
        itemRequest.setSourceId("urbanpiper");

        if (item.getBillComponents() != null && item.getBillComponents().getTaxes() != null) {
            itemRequest.setItem_tax(String.join(",", item.getBillComponents().getTaxes()));
        } else {
            itemRequest.setItem_tax("");
        }

        itemRequest.setVariation(new ArrayList<>());
        itemRequest.setAddon(itemAddonGroups);

        List<PosDataRequest.ItemRequest> items = new ArrayList<>();
        items.add(itemRequest);

        return ItemExtractionResult.builder()
                .items(items)
                .variations(new ArrayList<>())
                .addonGroups(itemAddonGroups)
                .build();
    }
}
