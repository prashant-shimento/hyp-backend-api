package com.hyp.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@NoArgsConstructor
public class PosDataRequest {

	private String success;
	private List<RestaurantRequest> restaurants;
	private List<OrderTypeRequest> ordertypes;
	private List<CategoryRequest> categories;
	private List<ParentCategoryRequest> parentcategories;
	private List<ItemRequest> items;
	private List<VariationRequest> variations;
	private List<TaxRequest> taxes;
	private List<DiscountRequest> discounts;
	private List<AddonGroupRequest> addongroups;
	private List<AttributeRequest> attributes;
	private String serverdatetime;
	private String message;
	private String error;
	private String db_version;
	private String application_version;
	private int http_code;

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public class RestaurantRequest {
		private String restaurantid;
		private String active;
		private RestaurantDetails details;
	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public class RestaurantDetails {
		private String menusharingcode;
		private String currency_html;
		private String country;
		private List<String> images;
		private String restaurantname;
		private String address;
		private String contact;
		private String latitude;
		private String longitude;
		private String landmark;
		private String city;
		private String state;
		private String minimumorderamount;
		private String minimumdeliverytime;
		private String deliverycharge;
		private String deliveryhoursfrom1;
		private String deliveryhoursto1;
		private String deliveryhoursfrom2;
		private String deliveryhoursto2;
		private String sc_applicable_on;
		private String sc_type;
		private String sc_calculate_on;
		private String sc_value;
		private String tax_on_sc;
		private int calculatetaxonpacking;
		private String pc_taxes_id;
		private int calculatetaxondelivery;
		private String dc_taxes_id;
		private String packaging_applicable_on;
		private String packaging_charge;
		private String packaging_charge_type;
	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public class OrderTypeRequest {
		private int ordertypeid;
		private String ordertype;

	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public class CategoryRequest {
		private String categoryid;
		private String active;
		private String categoryrank;
		private String parent_category_id;
		private String categoryname;
		private String categorytimings;
		private String category_image_url;

	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public class ParentCategoryRequest {
		private String id;
		private String name;
		private String rank;
		private String imageUrl;
		private String active;
		private String status;

	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public class ItemRequest {
		private String itemid;
		private String itemallowvariation;
		private String itemrank;
		private String item_categoryid;
		private String item_ordertype;
		private String item_packingcharges;
		private String itemallowaddon;
		private String itemaddonbasedon;
		private String item_favorite;
		private String ignore_taxes;
		private String ignore_discounts;
		private String in_stock;
		private List<String> cuisine;
		private String variation_groupname;
		private List<VariationRequest> variation;
		private List<AddonGroupRequest> addon;
		private String itemname;
		private String item_attributeid;
		private String itemdescription;
		private String minimumpreparationtime;
		private String price;
		private String active;
		private String item_image_url;
		private String item_tax;
		private String gst_type;
		private String is_recommend;

	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public class VariationRequest {
		private String id;
		private String name;
		private String variationid;
		private String groupname;
		private String price;
		private String active;
		private String item_packingcharges;
		private String variationrank;
		private List<AddonGroupRequest> addon;
		private int variationallowaddon;
		private String status;

	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public class AddonGroupRequest {
		private String addongroupid;
		private String addongroup_rank;
		private String active;
		private String addongroup_name;
		private List<AddonItemRequest> addongroupitems;
		private String addon_group_id;
		private String addon_item_selection_max;
		private String addon_item_selection_min;

	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public class AddonItemRequest {
		private String addonitemid;
		private String addonitem_name;
		private String addonitem_price;
		private String active;
		private String attributes;
		private String addonitem_rank;

	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public class AttributeRequest {
		private String attributeid;
		private String attribute;
		private String active;

	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public class DiscountRequest {
		private String discountid;
		private String discountname;
		private String discounttype;
		private String bogobuyqty;
		private String bogogetqty;
		private String bogotype;
		private String discount;
		private String bogoapplicableonpurchase;
		private String bogoapplicableonpurchaseitemids;
		private String bogoapplicableon;
		private String bogoapplicableonitemids;
		private String bogoitemamountlimit;
		private String bogopurchasediscount;
		private String bogoapplicableonitem;
		private String bogoapplicableonpurchaseitem;
		private String discountordertype;
		private String discountapplicableon;
		private String discountdays;
		private String discountontotal;
		private String discountstarts;
		private String discountends;
		private String discounttimefrom;
		private String discounttimeto;
		private String discountminamount;
		private String discountmaxamount;
		private String discounthascoupon;
		private String discountcategoryitemids;
		private String discountmaxlimit;
		private String active;
		private String discountorder;

	}

	@Getter
	@Setter
	@AllArgsConstructor
	@ToString
	public class TaxRequest {
		private String taxid;
		private String taxname;
		private String tax;
		private String taxtype;
		private String tax_coreortotal;
		private String tax_taxtype;
		private String tax_ordertype;
		private String rank;
		private String description;
		private String consider_in_core_amount;
		private String active;

	}
}
