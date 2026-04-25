package com.hyp.adapter.urbanpiper;

import com.hyp.request.urbanpiper.UrbanPiperMenuRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VariantPathTransformer {

    private final AddonGroupTransformer addonGroupTransformer;

    public List<VariantPath> extractVariantPaths(List<UrbanPiperMenuRequest.VariantGroup> variantGroups) {
        List<VariantPath> allPaths = new ArrayList<>();
        extractVariantPathsRecursive(variantGroups, new ArrayList<>(), allPaths);
        return allPaths;
    }

    private void extractVariantPathsRecursive(List<UrbanPiperMenuRequest.VariantGroup> variantGroups, 
                                              List<VariantInfo> currentPath, 
                                              List<VariantPath> allPaths) {
        if (variantGroups == null || variantGroups.isEmpty()) {
            if (!currentPath.isEmpty()) {
                allPaths.add(new VariantPath(new ArrayList<>(currentPath)));
            }
            return;
        }
        
        for (UrbanPiperMenuRequest.VariantGroup variantGroup : variantGroups) {
            String groupName = variantGroup.getTitle();
            List<UrbanPiperMenuRequest.Variant> variants = variantGroup.getVariants();
            
            if (variants != null) {
                for (UrbanPiperMenuRequest.Variant variant : variants) {
                    VariantInfo variantInfo = createVariantInfo(variant, groupName);
                    
                    currentPath.add(variantInfo);
                    
                    if (variant.getVariantGroups() != null && !variant.getVariantGroups().isEmpty()) {
                        extractVariantPathsRecursive(variant.getVariantGroups(), currentPath, allPaths);
                    } else {
                        allPaths.add(new VariantPath(new ArrayList<>(currentPath)));
                    }
                    
                    currentPath.remove(currentPath.size() - 1);
                }
            }
        }
    }

    private VariantInfo createVariantInfo(UrbanPiperMenuRequest.Variant variant, String groupName) {
        VariantInfo variantInfo = new VariantInfo();
        variantInfo.setRefId(variant.getRefId());
        variantInfo.setTitle(variant.getTitle());
        variantInfo.setGroupName(groupName);
        variantInfo.setPrice(variant.getPrice() != null ? variant.getPrice().doubleValue() : 0);
        variantInfo.setInStock(variant.getInStock() != null && variant.getInStock());
        
        if (variant.getAddOnGroups() != null) {
            variantInfo.setAddonGroups(addonGroupTransformer.transform(variant.getAddOnGroups()));
        }
        
        return variantInfo;
    }
}
