package com.hyp.model;

import java.util.List;

import com.hyp.entity.AddonGroup;
import com.hyp.entity.AddonItem;
import com.hyp.entity.Attribute;
import com.hyp.entity.Category;
import com.hyp.entity.Discount;
import com.hyp.entity.Item;
import com.hyp.entity.OrderType;
import com.hyp.entity.ParentCategory;
import com.hyp.entity.Restaurant;
import com.hyp.entity.Tax;
import com.hyp.entity.Variation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PosData {

	private Restaurant restaurant;
	private List<OrderType> orderTypes;
	private List<Category> categories;
	private List<ParentCategory> parentCategories;
	private List<Item> items;
	private List<Variation> variations;
	private List<Tax> taxes;
	private List<Discount> discounts;
	private List<AddonGroup> addonGroups;
	private List<Attribute> attributes;
	private List<AddonItem> addonItems;

	private PosData(PosDataBuilder builder) {
		this.restaurant = builder.restaurant;
		this.orderTypes = builder.orderTypes;
		this.categories = builder.categories;
		this.parentCategories = builder.parentCategories;
		this.items = builder.items;
		this.variations = builder.variations;
		this.taxes = builder.taxes;
		this.discounts = builder.discounts;
		this.addonGroups = builder.addonGroups;
		this.attributes = builder.attributes;
		this.addonItems = builder.addonItems;
	}

	public static PosDataBuilder builder() {
		return new PosDataBuilder();
	}

	public static class PosDataBuilder {
		private Restaurant restaurant;
		private List<OrderType> orderTypes;
		private List<Category> categories;
		private List<ParentCategory> parentCategories;
		private List<Item> items;
		private List<Variation> variations;
		private List<Tax> taxes;
		private List<Discount> discounts;
		private List<AddonGroup> addonGroups;
		private List<Attribute> attributes;
		private List<AddonItem> addonItems;

		private PosDataBuilder() {
		}

		public PosDataBuilder restaurant(Restaurant restaurant) {
			this.restaurant = restaurant;
			return this;
		}

		public PosDataBuilder orderTypes(List<OrderType> orderTypes) {
			this.orderTypes = orderTypes;
			return this;
		}
		
		public PosDataBuilder categories(List<Category> categories) {
			this.categories = categories;
			return this;
		}

		public PosDataBuilder parentCategories(List<ParentCategory> parentCategories) {
			this.parentCategories = parentCategories;
			return this;
		}

		public PosDataBuilder items(List<Item> items) {
			this.items = items;
			return this;
		}

		public PosDataBuilder variations(List<Variation> variations) {
			this.variations = variations;
			return this;
		}

		public PosDataBuilder taxes(List<Tax> taxes) {
			this.taxes = taxes;
			return this;
		}

		public PosDataBuilder discounts(List<Discount> discounts) {
			this.discounts = discounts;
			return this;
		}

		public PosDataBuilder addonGroups(List<AddonGroup> addonGroups) {
			this.addonGroups = addonGroups;
			return this;
		}

		public PosDataBuilder attributes(List<Attribute> attributes) {
			this.attributes = attributes;
			return this;
		}

		public PosDataBuilder addonItems(List<AddonItem> addonItems) {
			this.addonItems = addonItems;
			return this;
		}

		public PosData build() {
			return new PosData(this);
		}
	}
}
