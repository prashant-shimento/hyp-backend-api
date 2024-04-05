package com.hyp.dto;

import java.time.LocalDateTime;

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
public class AddonItemDto extends BaseDto {
	private String addonItemName;
	private String addonItemPrice;
	private String active;
	private String attributes;
	private String addonItemRank;
	private String addonItemSelectionMin;
	private String addonItemSelectionMax;
	private LocalDateTime autoTurnOnTime;
}
