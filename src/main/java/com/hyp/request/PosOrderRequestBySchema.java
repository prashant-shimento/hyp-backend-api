package com.hyp.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PosOrderRequestBySchema {
	@JsonProperty("app_key")
	private String appKey;

	@JsonProperty("app_secret")
	private String appSecret;

	@JsonProperty("access_token")
	private String accessToken;

	@JsonProperty("res_name")
	private String resName;

	private String address;

	@JsonProperty("Contact_information")
	private String contactInformation;

	@JsonProperty("restID")
	private String restId;

	@JsonProperty("OrderInfo")
	private OrderInfo orderInfo;

	private String udid;

	@JsonProperty("device_type")
	private String deviceType;

	@Data
	@NoArgsConstructor
	public class OrderInfo {
		@JsonProperty("Customer")
		private OrderInfoCustomer orderInfoCustomer;

		@JsonProperty("Order")
		private OrderInfoOrder orderInfoOrder;

		@JsonProperty("OrderItem")
		private OrderInfoOrderItem orderInfoOrderItem;

		@JsonProperty("Tax")
		private OrderInfoTax orderInfoTax;

		@JsonProperty("Discount")
		private OrderInfoDiscount orderInfoDiscount;

	}

	@Data
	@NoArgsConstructor
	public class OrderInfoCustomer {
		private String email;
		private String name;
		private String address;
		private String phone;
		private String latitude;
		private String longitude;

	}

	@Data
	@NoArgsConstructor
	public class OrderInfoOrder {
		@JsonProperty("orderID")
		private String orderId;

		@JsonProperty("preorder_date")
		private String preorderDate;

		@JsonProperty("preorder_time")
		private String preorderTime;

		@JsonProperty("delivery_charges")
		private String deliveryCharges;

		@JsonProperty("order_type")
		private String orderType;

		@JsonProperty("ondc_bap")
		private String ondcBap;

		@JsonProperty("advanced_order")
		private String advancedOrder;

		@JsonProperty("payment_type")
		private String paymentType;

		@JsonProperty("table_no")
		private String tableNo;

		@JsonProperty("no_of_persons")
		private String noOfPersons;

		@JsonProperty("discount_total")
		private String discountTotal;

		private String discount;

		@JsonProperty("discount_type")
		private String discountType;

		private String total;

		@JsonProperty("tax_total")
		private String taxTotal;

		private String description;

		@JsonProperty("created_on")
		private String createdOn;

		@JsonProperty("packing_charges")
		private String packingCharges;

		@JsonProperty("min_prep_time")
		private int minPrepTime;

		@JsonProperty("callback_url")
		private String callbackUrl;

		@JsonProperty("collect_cash")
		private String collectCash;

		private String otp;

		@JsonProperty("enable_delivery")
		private int enableDelivery;

		@JsonProperty("service_charge")
		private String serviceCharge;

		@JsonProperty("sc_tax_amount")
		private String scTaxAmount;

		@JsonProperty("dc_tax_amount")
		private String dcTaxAmount;

		@JsonProperty("dc_gst_details")
		private GstDetails dcGstDetails;

		@JsonProperty("pc_tax_amount")
		private String pcTaxAmount;

		@JsonProperty("pc_gst_details")
		private GstDetails pcGstDetails;

	}

	@Data
	@NoArgsConstructor
	public class OrderInfoOrderItem {
		private String id;
		private String name;
		@JsonProperty("gst_liability")
		private String gstLiability;
		@JsonProperty("item_tax")
		private ItemTax itemTax;
		@JsonProperty("item_discount")
		private String itemDiscount;
		@JsonProperty("final_price")
		private String finalPrice;
		private String price;
		private String quantity;
		private String description;
		@JsonProperty("variation_name")
		private String variationName;
		@JsonProperty("variation_id")
		private String variationId;
		@JsonProperty("AddonItem")
		private OrderInfoOrderItemAddonItem orderInfoOrderItemAddonItem;

	}

	@Data
	@NoArgsConstructor
	public class OrderInfoOrderItemAddonItem {
		private String id;
		private String name;
		@JsonProperty("group_name")
		private String groupName;
		private String price;
		@JsonProperty("group_id")
		private String groupId;
		private String quantity;

	}

	@Data
	@NoArgsConstructor
	public class OrderInfoTax {
		private String id;
		private String title;
		private String type;
		private String price;
		private String tax;
		@JsonProperty("restaurant_liable_amt")
		private String restaurantLiableAmt;

	}

	@Data
	@NoArgsConstructor
	public class OrderInfoDiscount {
		private String id;
		private String title;
		private String type;
		private String price;

	}

	@Data
	@NoArgsConstructor
	public class ItemTax {
		private String id;
		private String name;
		private String amount;

	}

	@Data
	@NoArgsConstructor
	public class GstDetails {
		@JsonProperty("gst_liable")
		private String gstLiable;
		private String amount;

	}

}
