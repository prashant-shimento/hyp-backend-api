package com.hyp.adapter.urbanpiper;

import com.hyp.constants.Constants;
import com.hyp.request.PosDataRequest;
import com.hyp.request.urbanpiper.UrbanPiperMenuRequest;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryTransformer {

    private final ItemTransformer itemTransformer;

    public MenuExtractionResult transform(List<UrbanPiperMenuRequest.Category> categories) {
        List<PosDataRequest.CategoryRequest> categoryList = new ArrayList<>();
        List<PosDataRequest.ItemRequest> itemList = new ArrayList<>();
        List<PosDataRequest.VariationRequest> variationList = new ArrayList<>();
        List<PosDataRequest.AddonGroupRequest> addonGroupList = new ArrayList<>();

        int categoryRank = 0;

        for (UrbanPiperMenuRequest.Category categoryNode : categories) {
            String categoryRefId = categoryNode.getRefId();
            String categoryTitle = categoryNode.getTitle();

            PosDataRequest.CategoryRequest category = new PosDataRequest.CategoryRequest();
            category.setCategoryid(categoryRefId);
            category.setCategoryname(categoryTitle);
            category.setCategoryrank(String.valueOf(categoryRank++));
            category.setActive("1");
            category.setCategory_image_url(categoryNode.getImageUrl());
            category.setCategorytimings(categoryNode.getTimings());
            category.setSourceId("urbanpiper");
            categoryList.add(category);

            if (categoryNode.getItems() != null) {
                for (UrbanPiperMenuRequest.Item item : categoryNode.getItems()) {
                    ItemExtractionResult itemResult = itemTransformer.transform(item, categoryRefId, null);
                    itemList.addAll(itemResult.getItems());
                    variationList.addAll(itemResult.getVariations());
                    addonGroupList.addAll(itemResult.getAddonGroups());
                }
            }

            if (categoryNode.getSubcategories() != null) {
                for (UrbanPiperMenuRequest.SubCategory subCategory : categoryNode.getSubcategories()) {
                    String subCategoryRefId = subCategory.getRefId();
                    String subCategoryTitle = subCategory.getTitle();

                    PosDataRequest.CategoryRequest subCat = new PosDataRequest.CategoryRequest();
                    subCat.setCategoryid(subCategoryRefId);
                    subCat.setCategoryname(subCategoryTitle);
                    subCat.setCategoryrank(String.valueOf(categoryRank++));
                    subCat.setParent_category_id(categoryRefId);
                    subCat.setActive("1");
                    subCat.setCategory_image_url(subCategory.getImageUrl());
                    subCat.setSourceId(Constants.URBAN_PIPER);
                    categoryList.add(subCat);

                    if (subCategory.getItems() != null) {
                        for (UrbanPiperMenuRequest.Item item : subCategory.getItems()) {
                            ItemExtractionResult itemResult =
                                    itemTransformer.transform(item, categoryRefId, subCategoryRefId);
                            itemList.addAll(itemResult.getItems());
                            variationList.addAll(itemResult.getVariations());
                            addonGroupList.addAll(itemResult.getAddonGroups());
                        }
                    }
                }
            }
        }

        return MenuExtractionResult.builder()
                .categories(categoryList)
                .items(itemList)
                .variations(variationList)
                .addonGroups(addonGroupList)
                .build();
    }
}
