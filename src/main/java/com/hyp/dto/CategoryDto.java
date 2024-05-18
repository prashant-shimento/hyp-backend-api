package com.hyp.dto;

import lombok.Data;

@Data
public class CategoryDto extends BaseDto {
	private String active;
	private String categoryRank;
	private String parentCategoryId;
	private String categoryName;
	private String categoryTimings;
	private String categoryImageUrl;

}
