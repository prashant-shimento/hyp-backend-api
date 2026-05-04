package com.hyp.adapter.urbanpiper;

import com.hyp.request.PosDataRequest;
import com.hyp.request.urbanpiper.UrbanPiperMenuRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VariantTransformer {

    private final AddonGroupTransformer addonGroupTransformer;
    private final VariantPathTransformer variantPathTransformer;
    private int variationCounter = 0;

    public ItemExtractionResult processItemWithVariants(
            UrbanPiperMenuRequest.Item item,
            String itemRefId,
            String itemTitle,
            String itemDescription,
            String categoryRefId,
            String subCategoryRefId,
            List<PosDataRequest.AddonGroupRequest> itemAddonGroups) {

        variationCounter = 0;
        List<UrbanPiperMenuRequest.VariantGroup> variantGroups = item.getVariantGroups();

        List<VariantPath> variantPaths = variantPathTransformer.extractVariantPaths(variantGroups);

        List<PosDataRequest.ItemRequest> items = new ArrayList<>();
        List<PosDataRequest.VariationRequest> variations = new ArrayList<>();
        List<PosDataRequest.AddonGroupRequest> allAddonGroups = new ArrayList<>(itemAddonGroups);

        for (VariantPath path : variantPaths) {
            PosDataRequest.ItemRequest itemRequest = createItemFromVariantPath(
                    path,
                    item,
                    itemRefId,
                    itemTitle,
                    itemDescription,
                    categoryRefId,
                    subCategoryRefId,
                    itemAddonGroups,
                    variations);
            items.add(itemRequest);

            for (VariantInfo variant : path.getVariants()) {
                allAddonGroups.addAll(variant.getAddonGroups());
            }
        }

        return ItemExtractionResult.builder()
                .items(items)
                .variations(variations)
                .addonGroups(allAddonGroups)
                .build();
    }

    private PosDataRequest.ItemRequest createItemFromVariantPath(
            VariantPath path,
            UrbanPiperMenuRequest.Item item,
            String itemRefId,
            String itemTitle,
            String itemDescription,
            String categoryRefId,
            String subCategoryRefId,
            List<PosDataRequest.AddonGroupRequest> itemAddonGroups,
            List<PosDataRequest.VariationRequest> allVariations) {

        PosDataRequest.ItemRequest itemRequest = new PosDataRequest.ItemRequest();

        String lastVariantRefId =
                path.getVariants().get(path.getVariants().size() - 1).getRefId();
        itemRequest.setItemid(itemRefId + "_" + lastVariantRefId);
        itemRequest.setItemname(itemTitle);
        itemRequest.setItemdescription(itemDescription);

        double totalPrice = 0;
        for (VariantInfo variant : path.getVariants()) {
            totalPrice += variant.getPrice();
        }
        itemRequest.setPrice(String.valueOf((int) totalPrice));

        itemRequest.setItem_categoryid(subCategoryRefId != null ? subCategoryRefId : categoryRefId);

        VariantInfo lastVariant = path.getVariants().get(path.getVariants().size() - 1);
        itemRequest.setIn_stock(lastVariant.isInStock() ? "1" : "0");
        itemRequest.setActive("1");
        itemRequest.setItem_ordertype("1,2,3");
        itemRequest.setItemallowvariation("1");

        boolean hasAddons = !lastVariant.getAddonGroups().isEmpty() || !itemAddonGroups.isEmpty();
        itemRequest.setItemallowaddon(hasAddons ? "1" : "0");

        boolean recommended = item.getRecommended() != null && item.getRecommended();
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

        List<PosDataRequest.VariationRequest> itemVariations = new ArrayList<>();
        for (VariantInfo variant : path.getVariants()) {
            PosDataRequest.VariationRequest variation = new PosDataRequest.VariationRequest();
            variation.setVariationid(variant.getRefId());
            variation.setName(variant.getTitle());
            variation.setGroupname(variant.getGroupName());
            variation.setPrice(String.valueOf((int) variant.getPrice()));
            variation.setActive("1");
            variation.setStatus("1");
            variation.setVariationrank("0");
            variation.setItem_packingcharges("0");
            variation.setVariationallowaddon(variant.getAddonGroups().isEmpty() ? 0 : 1);
            variation.setAddon(variant.getAddonGroups());
            variation.setSourceId("urbanpiper");
            variation.setId(String.valueOf(variationCounter++));

            itemVariations.add(variation);
            allVariations.add(variation);
        }

        itemRequest.setVariation(itemVariations);

        List<PosDataRequest.AddonGroupRequest> allAddons = new ArrayList<>(itemAddonGroups);
        allAddons.addAll(lastVariant.getAddonGroups());
        itemRequest.setAddon(allAddons);

        return itemRequest;
    }
}
