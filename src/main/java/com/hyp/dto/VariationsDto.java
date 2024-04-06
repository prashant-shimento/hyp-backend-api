
package com.hyp.dto;

import java.util.List;

import lombok.Data;

@Data
public class VariationsDto {
	private String name;
    private String groupName;
    private String status;
    private String price;
    private String active;
    private String itemPackingCharges;
    private String variationRank;
    private List<String> addonGroupId;
    private int variationAllowAddon;
    private String variationId;
	
}
