
package com.hyp.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class VariationDto extends BaseDto {
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
	private transient List<AddonGroupDto> addonGroups;

}
