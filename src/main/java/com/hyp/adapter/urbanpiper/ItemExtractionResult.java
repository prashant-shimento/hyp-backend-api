package com.hyp.adapter.urbanpiper;

import com.hyp.request.PosDataRequest;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ItemExtractionResult {
    private List<PosDataRequest.ItemRequest> items;
    private List<PosDataRequest.VariationRequest> variations;
    private List<PosDataRequest.AddonGroupRequest> addonGroups;
}
