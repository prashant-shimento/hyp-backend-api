package com.hyp.dto;

import java.util.List;

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
public class AddonGroupDto extends BaseDto {
	private String addonGroupName;
	private String addonGroupRank;
	private String active;
	private String addonItemSelectionMax;
	private String addonItemSelectionMin;
	private List<String> addonGroupItems;
	private List<AddonItemDto> addonItems;
}
