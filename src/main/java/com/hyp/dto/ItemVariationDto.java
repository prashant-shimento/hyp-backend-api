package com.hyp.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class ItemVariationDto {
    private String id;
    private String variationid;
    private String name;
    private String groupname;
    private String price;
    private String active;
    private String itemPackingCharges;
    private String variationrank;
    private List<ItemAddonDto> addon;
    private Integer variationallowaddon;
}
