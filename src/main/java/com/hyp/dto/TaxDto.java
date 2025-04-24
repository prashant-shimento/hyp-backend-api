package com.hyp.dto;

import lombok.Data;

@Data
public class TaxDto extends BaseDto {
	private String taxName;
	private String tax;
	private String taxType;
	private String taxOrderType;
	private String active;
	private String taxCoreOrTotal;
	private String taxTaxType;
	private String rank;
	private String considerInCoreAmount;
	private String description;
}
