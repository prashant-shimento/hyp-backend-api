package com.hyp.adapter.urbanpiper;

import com.hyp.request.PosDataRequest;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class VariantInfo {
    private String refId;
    private String title;
    private String groupName;
    private double price;
    private boolean inStock;
    private List<PosDataRequest.AddonGroupRequest> addonGroups = new ArrayList<>();
}
