package com.hyp.adapter.urbanpiper;

import com.hyp.request.PosDataRequest;
import com.hyp.request.urbanpiper.UrbanPiperMenuRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AddonGroupTransformer {

    private final Set<String> processedAddonGroupIds = new HashSet<>();
    private final List<PosDataRequest.AddonGroupRequest> allAddonGroups = new ArrayList<>();

    public List<PosDataRequest.AddonGroupRequest> transform(List<UrbanPiperMenuRequest.AddOnGroup> addonGroups) {
        List<PosDataRequest.AddonGroupRequest> result = new ArrayList<>();

        if (addonGroups != null) {
            for (UrbanPiperMenuRequest.AddOnGroup addonGroup : addonGroups) {
                PosDataRequest.AddonGroupRequest transformed = processAddonGroup(addonGroup);
                if (transformed != null) {
                    result.add(transformed);
                }
            }
        }

        return result;
    }

    public List<PosDataRequest.AddonGroupRequest> getAllAddonGroups() {
        return new ArrayList<>(allAddonGroups);
    }

    public void reset() {
        processedAddonGroupIds.clear();
        allAddonGroups.clear();
    }

    private PosDataRequest.AddonGroupRequest processAddonGroup(UrbanPiperMenuRequest.AddOnGroup addonGroup) {
        String addonGroupRefId = addonGroup.getRefId();

        if (processedAddonGroupIds.contains(addonGroupRefId)) {
            for (PosDataRequest.AddonGroupRequest existing : allAddonGroups) {
                if (addonGroupRefId.equals(existing.getAddongroupid())) {
                    return existing;
                }
            }
        }

        PosDataRequest.AddonGroupRequest request = new PosDataRequest.AddonGroupRequest();
        request.setAddongroupid(addonGroupRefId);
        request.setAddon_group_id(addonGroupRefId);
        request.setAddongroup_name(addonGroup.getTitle());
        request.setAddon_item_selection_min(
                addonGroup.getMinimumNeeded() != null ? String.valueOf(addonGroup.getMinimumNeeded()) : null);
        request.setAddon_item_selection_max(
                addonGroup.getMaximumAllowed() != null ? String.valueOf(addonGroup.getMaximumAllowed()) : null);
        request.setActive("1");
        request.setAddongroup_rank("0");
        request.setSourceId("urbanpiper");

        List<PosDataRequest.AddonItemRequest> addonItems = new ArrayList<>();
        if (addonGroup.getAddons() != null) {
            for (UrbanPiperMenuRequest.AddOn addon : addonGroup.getAddons()) {
                PosDataRequest.AddonItemRequest addonItem = new PosDataRequest.AddonItemRequest();
                addonItem.setAddonitemid(addon.getRefId());
                addonItem.setAddonitem_name(addon.getTitle());
                addonItem.setAddonitem_price(addon.getPrice() != null ? String.valueOf(addon.getPrice()) : "0");
                addonItem.setActive(addon.getInStock() != null && addon.getInStock() ? "1" : "0");
                addonItem.setAddonitem_rank("0");
                addonItem.setSourceId("urbanpiper");

                addonItems.add(addonItem);
            }
        }

        request.setAddongroupitems(addonItems);

        processedAddonGroupIds.add(addonGroupRefId);
        allAddonGroups.add(request);

        return request;
    }
}
