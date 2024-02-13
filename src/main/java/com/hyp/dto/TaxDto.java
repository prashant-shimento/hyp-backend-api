package com.hyp.dto;

import lombok.Data;

@Data
public class TaxDto {
	private String taxid;
	private String taxname;
	private String tax;
	private String taxtype;
	private String tax_ordertype;
	private String active;
	private String tax_coreortotal;
	private String tax_taxtype;
	private String rank;
	private String consider_in_core_amount;
	private String description;
}
