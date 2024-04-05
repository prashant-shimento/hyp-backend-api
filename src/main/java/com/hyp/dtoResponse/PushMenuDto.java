package com.hyp.dtoResponse;

import java.util.List;

import com.hyp.dto.AddonGroupDto;
import com.hyp.dto.AttributeDto;
import com.hyp.dto.CategoryDto;
import com.hyp.dto.DiscountDto;
import com.hyp.dto.MenuItemDto;
import com.hyp.dto.OrderType;
import com.hyp.dto.ParentCategoryDto;
import com.hyp.dto.RestaurantDto;
import com.hyp.dto.TaxDto;
import com.hyp.dto.VariationsDto;

import lombok.Data;

@Data
public class PushMenuDto {

	private String success;
	private List<AttributeDto> attributes;
	private List<CategoryDto> categories;
	private List<DiscountDto> discounts;
	private List<MenuItemDto> items;
	private List<OrderType> ordertypes;
	private List<ParentCategoryDto> parentcategories;
	private List<RestaurantDto> restaurants;
	private List<TaxDto> taxes;
	private List<VariationsDto> variations;
	private List<AddonGroupDto> addongroups;
	private String serverdatetime;
	private String db_version;
	private String application_version;
	private int http_code;
}
