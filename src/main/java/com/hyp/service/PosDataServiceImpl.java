package com.hyp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hyp.entity.Restaurant;
import com.hyp.model.PosData;
import com.hyp.request.PosDataRequest;
import com.hyp.translation.RequestTranslation;

@Service
public class PosDataServiceImpl implements PosDataService {

	private final RestaurantService restaurantService;
	private final TaxService taxService;
	private final OrderTypeService orderTypeService;
	private final AttributeService attributeService;
	private final DiscountService discountService;
	private final VariationService variationService;
	private final AddonItemService addonItemService;
	private final AddonGroupService addonGroupService;
	private final CategoryService categoryService;
	private final ItemService itemService;

	public PosDataServiceImpl(AttributeService attributeService, CategoryService categoryService,
			TaxService taxService, OrderTypeService orderTypeService, VariationService variationService,
			RestaurantService restaurantService, DiscountService discountService,
			AddonGroupService addonGroupService, AddonItemService addonItemService, ItemService itemService) {
		this.attributeService = attributeService;
		this.categoryService = categoryService;
		this.taxService = taxService;
		this.orderTypeService = orderTypeService;
		this.variationService = variationService;
		this.restaurantService = restaurantService;
		this.discountService = discountService;
		this.addonGroupService = addonGroupService;
		this.addonItemService = addonItemService;
		this.itemService = itemService;
	}

	@Override
	@Transactional
	public boolean savePosData(PosDataRequest posDataRequest) {
		try {

			PosData posData = RequestTranslation.getPosData(posDataRequest);
			Restaurant restaurant = restaurantService.save(posData.getRestaurant());
			saveEntities(restaurant, posData);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private void saveEntities(Restaurant restaurant, PosData posData) {
		try {
			orderTypeService.saveAll(posData.getOrderTypes(), restaurant.getId());
			attributeService.saveAll(posData.getAttributes(), restaurant.getId());
			discountService.saveAll(posData.getDiscounts(), restaurant.getId());
			categoryService.saveAll(posData.getCategories(), restaurant.getId());
			taxService.saveAll(posData.getTaxes(), restaurant.getId());
			variationService.saveAll(posData.getVariations(), restaurant.getId());
			addonItemService.saveAll(posData.getAddonItems(), restaurant.getId());
			addonGroupService.saveAll(posData.getAddonGroups(), restaurant.getId());
			itemService.saveAll(posData.getItems(), restaurant.getId());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
