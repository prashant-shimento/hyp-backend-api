package com.hyp.mapper;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.hyp.entity.Attribute;
import com.hyp.entity.Category;
import com.hyp.entity.Discount;
import com.hyp.entity.OrderType;
import com.hyp.entity.Restaurant;
import com.hyp.entity.Tax;
import com.hyp.entity.Variation;
import com.hyp.request.MenuRequest.AttributeRequest;
import com.hyp.request.MenuRequest.CategoryRequest;
import com.hyp.request.MenuRequest.DiscountRequest;
import com.hyp.request.MenuRequest.OrderTypeRequest;
import com.hyp.request.MenuRequest.RestaurantRequest;
import com.hyp.request.MenuRequest.TaxRequest;
import com.hyp.request.MenuRequest.VariationRequest;

@Component
public class RequestTranslation {

	public Attribute translateAttribute(AttributeRequest attributeRequest) {
		Attribute attribute = new Attribute();
		attribute.setId(attributeRequest.getAttributeid());
		attribute.setAttribute(attributeRequest.getAttribute());
		attribute.setActive(attributeRequest.getActive());
		return attribute;
	}

	public List<Attribute> translateAttributeList(List<AttributeRequest> attributeRequestList) {
		List<Attribute> attributeList = new ArrayList<>();
		for (AttributeRequest attributeRequest : attributeRequestList) {
			Attribute attribute = translateAttribute(attributeRequest);
			attributeList.add(attribute);
		}
		return attributeList;
	}

	public OrderType translateOrderType(OrderTypeRequest orderTypeRequest) {
		OrderType orderType = new OrderType();
		orderType.setId(String.valueOf(orderTypeRequest.getOrdertypeid()));
		orderType.setOrderType(orderTypeRequest.getOrdertype());
		return orderType;
	}

	public List<OrderType> translateOrderTypeList(List<OrderTypeRequest> orderTypeRequestList) {
		List<OrderType> orderTypeList = new ArrayList<>();
		for (OrderTypeRequest orderTypeRequest : orderTypeRequestList) {
			OrderType orderType = translateOrderType(orderTypeRequest);
			orderTypeList.add(orderType);
		}
		return orderTypeList;
	}

	public Tax translateTax(TaxRequest taxRequest) {
		Tax tax = new Tax();
		tax.setId(taxRequest.getTaxid());
		tax.setTaxName(taxRequest.getTaxname());
		tax.setTax(taxRequest.getTax());
		tax.setTaxType(taxRequest.getTaxtype());
		tax.setTaxCoreOrTotal(taxRequest.getTax_coreortotal());
		tax.setTaxTaxType(taxRequest.getTax_taxtype());
		tax.setTaxOrderType(taxRequest.getTax_ordertype());
		tax.setRank(taxRequest.getRank());
		tax.setDescription(taxRequest.getDescription());
		tax.setConsiderInCoreAmount(taxRequest.getConsider_in_core_amount());
		tax.setActive(taxRequest.getActive());
		return tax;
	}

	public List<Tax> translateTaxList(List<TaxRequest> taxRequestList) {
		List<Tax> taxList = new ArrayList<>();
		for (TaxRequest taxRequest : taxRequestList) {
			Tax tax = translateTax(taxRequest);
			taxList.add(tax);
		}
		return taxList;
	}

	public Variation translateVariation(VariationRequest variationRequest) {
		Variation variation = new Variation();
		variation.setId(variationRequest.getVariationid());
		variation.setName(variationRequest.getName());
		variation.setGroupName(variationRequest.getGroupname());
		variation.setStatus(variationRequest.getStatus());
		variation.setVariationId(variationRequest.getVariationid());
		variation.setPrice(variationRequest.getPrice());
		variation.setActive(variationRequest.getActive());
		variation.setItemPackingCharges(variationRequest.getItem_packingcharges());
		variation.setVariationRank(variationRequest.getVariationrank());
		variation.setVariationAllowAddon(variationRequest.getVariationallowaddon());
		return variation;
	}

	public List<Variation> translateVariationList(List<VariationRequest> variationRequestList) {
		List<Variation> variationList = new ArrayList<>();
		for (VariationRequest variationRequest : variationRequestList) {
			Variation variation = translateVariation(variationRequest);
			variationList.add(variation);
		}
		return variationList;
	}

	public Restaurant translateRestaurant(RestaurantRequest restaurantRequest) {
		Restaurant restaurant = new Restaurant();
		restaurant.setId(restaurantRequest.getRestaurantid());
		restaurant.setActive(restaurantRequest.getActive());
		restaurant.setCurrencyHtml(restaurantRequest.getDetails().getCurrency_html());
		restaurant.setCountry(restaurantRequest.getDetails().getCountry());
		restaurant.setMinimumOrderAmount(restaurantRequest.getDetails().getMinimumorderamount());
		restaurant.setRestaurantName(restaurantRequest.getDetails().getRestaurantname());
		restaurant.setPackagingApplicableOn(restaurantRequest.getDetails().getPackaging_applicable_on());
		restaurant.setCity(restaurantRequest.getDetails().getCity());
		restaurant.setLatitude(restaurantRequest.getDetails().getLatitude());
		restaurant.setPackagingCharge(restaurantRequest.getDetails().getPackaging_charge());
		restaurant.setCalculateTaxOnDelivery(restaurantRequest.getDetails().getCalculatetaxondelivery());
		restaurant.setPackagingChargeType(restaurantRequest.getDetails().getPackaging_charge_type());
		restaurant.setContact(restaurantRequest.getDetails().getContact());
		restaurant.setState(restaurantRequest.getDetails().getState());
		restaurant.setLandmark(restaurantRequest.getDetails().getLandmark());
		restaurant.setLongitude(restaurantRequest.getDetails().getLongitude());
		restaurant.setImages(restaurantRequest.getDetails().getImages());
		restaurant.setAddress(restaurantRequest.getDetails().getAddress());
		restaurant.setPcTaxesId(restaurantRequest.getDetails().getPc_taxes_id());
		restaurant.setDeliveryHoursFrom2(restaurantRequest.getDetails().getDeliveryhoursfrom2());
		restaurant.setMenuSharingCode(restaurantRequest.getDetails().getMenusharingcode());
		restaurant.setDeliveryHoursFrom1(restaurantRequest.getDetails().getDeliveryhoursfrom1());
		restaurant.setDeliveryHoursTo2(restaurantRequest.getDetails().getDeliveryhoursto2());
		restaurant.setDeliveryHoursTo1(restaurantRequest.getDetails().getDeliveryhoursto1());
		restaurant.setCalculateTaxOnPacking(restaurantRequest.getDetails().getCalculatetaxondelivery());
		restaurant.setDcTaxesId(restaurantRequest.getDetails().getDc_taxes_id());
		restaurant.setDeliveryCharge(restaurantRequest.getDetails().getDeliverycharge());
		restaurant.setMinimumDeliveryTime(restaurantRequest.getDetails().getMinimumdeliverytime());
		return restaurant;
	}

	public List<Restaurant> translateRestaurantList(List<RestaurantRequest> restaurantRequestList) {
		List<Restaurant> restaurantList = new ArrayList<>();
		for (RestaurantRequest restaurantRequest : restaurantRequestList) {
			Restaurant restaurant = translateRestaurant(restaurantRequest);
			restaurantList.add(restaurant);
		}
		return restaurantList;
	}

	public Category translateCategory(CategoryRequest categoryRequest) {
		Category category = new Category();
		category.setId(categoryRequest.getCategoryid());
		category.setParentCategoryId(categoryRequest.getParent_category_id());
		category.setCategoryImageUrl(categoryRequest.getCategory_image_url());
		category.setCategoryTimings(categoryRequest.getCategorytimings());
		category.setActive(categoryRequest.getActive());
		category.setCategoryName(categoryRequest.getCategoryname());
		category.setCategoryRank(categoryRequest.getCategoryrank());
		return category;
	}

	public List<Category> translateCategoryList(List<CategoryRequest> categoryRequestList) {
		List<Category> categoryList = new ArrayList<>();
		for (CategoryRequest categoryRequest : categoryRequestList) {
			Category category = translateCategory(categoryRequest);
			categoryList.add(category);
		}
		return categoryList;
	}

	public Discount translateDiscount(DiscountRequest discountRequest) {
		Discount discount = new Discount();
		discount.setId(discountRequest.getDiscountid());
		discount.setDiscountName(discountRequest.getDiscountname());
		discount.setDiscountType(discountRequest.getDiscounttype());
		discount.setDiscount(discountRequest.getDiscount());
		discount.setDiscountOrderType(discountRequest.getDiscountordertype());
		discount.setDiscountApplicableOn(discountRequest.getDiscountapplicableon());
		discount.setDiscountDays(discountRequest.getDiscountdays());
		discount.setActive(discountRequest.getActive());
		discount.setDiscountOnTotal(discountRequest.getDiscountontotal());
		discount.setDiscountStarts(discountRequest.getDiscountstarts());
		discount.setDiscountEnds(discountRequest.getDiscountends());
		discount.setDiscountTimeFrom(discountRequest.getDiscounttimefrom());
		discount.setDiscountTimeTo(discountRequest.getDiscounttimeto());
		discount.setDiscountMinAmount(discountRequest.getDiscountminamount());
		discount.setDiscountMaxAmount(discountRequest.getDiscountmaxamount());
		discount.setDiscountHasCoupon(discountRequest.getDiscounthascoupon());
		discount.setDiscountCategoryItemIds(discountRequest.getDiscountcategoryitemids());
		discount.setDiscountMaxLimit(discountRequest.getDiscountmaxlimit());
		return discount;
	}

	public List<Discount> translateDiscountList(List<DiscountRequest> discountRequestList) {
		List<Discount> discountList = new ArrayList<>();
		for (DiscountRequest discountRequest : discountRequestList) {
			Discount discount = translateDiscount(discountRequest);
			discountList.add(discount);
		}
		return discountList;
	}

}
