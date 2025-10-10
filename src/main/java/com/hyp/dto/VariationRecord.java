package com.hyp.dto;

import java.util.List;

public record VariationRecord(
        String name,
        String groupName,
        String status,
        String price,
        String active,
        String itemPackingCharges,
        String variationRank,
        List<String> addonGroupId,
        int variationAllowAddon,
        String variationId,
        List<AddonGroupDto> addonGroups) {}
