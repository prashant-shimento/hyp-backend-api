package com.hyp.adapter.urbanpiper;

import com.hyp.request.PosDataRequest;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MenuExtractionResult {
    private List<PosDataRequest.CategoryRequest> categories;
    private List<PosDataRequest.ItemRequest> items;
    private List<PosDataRequest.VariationRequest> variations;
    private List<PosDataRequest.AddonGroupRequest> addonGroups;
}
