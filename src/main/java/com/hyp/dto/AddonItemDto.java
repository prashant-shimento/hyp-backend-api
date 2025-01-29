package com.hyp.dto;

import java.time.LocalDateTime;

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
