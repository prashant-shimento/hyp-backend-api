package com.hyp.dto;

import java.util.List;

import lombok.Data;

@Data
public class AddonGroupDTO {
	private String addongroupId;
    private String addongroupRank;
    private String active;
    private String addongroupName;
    private List<String> addongroupitems;
}
