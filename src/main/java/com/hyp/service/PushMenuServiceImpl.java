package com.hyp.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hyp.entity.Attribute;
import com.hyp.entity.Category;
import com.hyp.entity.Discount;
import com.hyp.entity.OrderType;
import com.hyp.entity.Restaurant;
import com.hyp.entity.Tax;
import com.hyp.entity.Variation;
import com.hyp.mapper.RequestTranslation;
import com.hyp.request.MenuRequest;

@Component
public class PushMenuServiceImpl implements PushMenuService {

	@Autowired
	RequestTranslation requestTranslation;

	@Autowired
	private AttributeService attributeService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private TaxService taxService;

	@Autowired
	private OrderTypeService orderTypeService;

	@Autowired
	private VariationService variationService;
	
	@Autowired
	private RestaurantService restaurantService;
	
	@Autowired
	private DiscountService discountService;

	@Override
	public boolean pushMenu(MenuRequest pushMenuRequest) {
		List<Attribute> attributeList = requestTranslation.translateAttributeList(pushMenuRequest.getAttributes());
		List<Restaurant> restaurantList = requestTranslation.translateRestaurantList(pushMenuRequest.getRestaurants());
		List<Discount> discountList = requestTranslation.translateDiscountList(pushMenuRequest.getDiscounts());
		List<Category> categoryList = requestTranslation.translateCategoryList(pushMenuRequest.getCategories());
		List<OrderType> orderTypeList = requestTranslation.translateOrderTypeList(pushMenuRequest.getOrdertypes());
		List<Variation> variationList = requestTranslation.translateVariationList(pushMenuRequest.getVariations());
		List<Tax> taxList = requestTranslation.translateTaxList(pushMenuRequest.getTaxes());

		attributeService.saveAll(attributeList);
		categoryService.saveAll(categoryList);
		taxService.saveAll(taxList);
		orderTypeService.saveAll(orderTypeList);
		variationService.saveAll(variationList);
		restaurantService.saveAll(restaurantList);
		discountService.saveAll(discountList);
		return true;
	}

}
