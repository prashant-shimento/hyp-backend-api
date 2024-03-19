package com.hyp.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PosOrderRequest extends PosBaseRequest {

	@JsonProperty("orderinfo")
	private OrderInfo orderInfo;

	@Data
	@NoArgsConstructor
	public static class OrderInfo {
		@JsonProperty("OrderInfo")
		private OrderInfoDetails orderInfoDetails;

		@JsonProperty("udid")
		private String udid;

		@JsonProperty("device_type")
		private String deviceType;

	}

	@Data
	@NoArgsConstructor
	public static class OrderInfoDetails {
		@JsonProperty("Restaurant")
		private RestaurantOrderRequest restaurant;

		@JsonProperty("Customer")
		private CustomerOrderRequest customer;

		@JsonProperty("Order")
		private OrderRequest order;

		@JsonProperty("OrderItem")
		private OrderItemRequest orderItem;

		@JsonProperty("Tax")
		private TaxOrderRequest tax;

		@JsonProperty("Discount")
		private DiscountOrderRequest discount;

	}

	@Data
	@NoArgsConstructor
	public static class RestaurantOrderRequest {
		@JsonProperty("details")
		private RestaurantDetails restaurantDetails;

	}

	@Data
	@NoArgsConstructor
	public static class CustomerOrderRequest {
		@JsonProperty("details")
		private CustomerDetails customerDetails;

	}

	@Data
	@NoArgsConstructor
	public static class CustomerDetails {
		private String email;
		private String name;
		private String phone;
		private String latitude;
		private String longitude;
	}

	@Data
	@NoArgsConstructor
	public static class OrderRequest {
		@JsonProperty("details")
		private OrderDetails orderDetails;

	}

	@Data
	@NoArgsConstructor
	public static class OrderDetails {
		@JsonProperty("orderID")
		private String orderId;
		@JsonProperty("preorder_date")
		private String preOrderDate;
		@JsonProperty("preorder_time")
		private String preOrderTime;
		@JsonProperty("service_charge")
		private String serviceCharge;
		@JsonProperty("sc_tax_amount")
		private String scTaxAmount;
		@JsonProperty("delivery_charges")
		private String deliveryCharges;
		@JsonProperty("dc_tax_amount")
		private String dcTaxAmount;
		@JsonProperty("dc_gst_details")
		private List<GstDetails> dcGstDetails;
		@JsonProperty("packing_charges")
		private String packingCharges;
		@JsonProperty("pc_tax_amount")
		private String pcTaxAmount;
		@JsonProperty("pc_gst_details")
		private List<GstDetails> pcGstDetails;
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
		@JsonProperty("tax_total")
		private String taxTotal;
		@JsonProperty("discount_type")
		private String discountType;
		private String total;
		private String description;
		@JsonProperty("created_on")
		private String createdOn;
		@JsonProperty("enable_delivery")
		private int enableDelivery;
		@JsonProperty("min_prep_time")
		private int minPrepTime;
		@JsonProperty("callback_url")
		private String callbackUrl;
		@JsonProperty("collect_cash")
		private String collectCash;
		private String otp;

	}

	@Data
	@NoArgsConstructor
	public static class OrderItemRequest {
		private List<OrderItemDetails> details;

	}

	@Data
	@NoArgsConstructor
	public static class TaxOrderRequest {
		private List<TaxDetails> details;

	}

	@Data
	@NoArgsConstructor
	public static class DiscountOrderRequest {
		private List<DiscountDetails> details;

	}

	@Data
	@NoArgsConstructor
	public static class RestaurantDetails {
		@JsonProperty("res_name")
		private String resName;

		private String address;

		@JsonProperty("contact_information")
		private String contactInformation;

		@JsonProperty("restID")
		private String restId;
	}

	@Data
	@NoArgsConstructor
	public static class OrderItemDetails {

		private String id;
		private String name;
		@JsonProperty("gst_liability")
		private String gstLiability;
		@JsonProperty("item_tax")
		private List<ItemTax> itemTax;
		private String itemDiscount;
		private String price;
		private String finalPrice;
		private String quantity;
		private String variationName;
		private String variationId;
		@JsonProperty("AddonItem")
		private AddonItemOrderRequest addonItems;

	}

	@Data
	@NoArgsConstructor
	public static class ItemTax {
		private String id;
		private String name;
		private String amount;
	}

	@Data
	@NoArgsConstructor
	public static class TaxDetails {
		private String id;
		private String price;
		private String title;
		private String type;
		private String tax;
		@JsonProperty("restaurant_liable_amt")
		private String restaurantLiableAmt;
	}

	@Data
	@NoArgsConstructor
	public static class DiscountDetails {
		private String id;
		private String price;
		private String title;
		private String type;
	}

	@Data
	@NoArgsConstructor
	public static class AddonItemOrderRequest {
		private List<AddonItemDetails> details;
	}

	@Data
	@NoArgsConstructor
	public static class AddonItemDetails {
		private String id;
		private String name;
		@JsonProperty("group_name")
		private String groupName;
		private String price;
		@JsonProperty("group_id")
		private int groupId;
		private String quantity;
	}

	@Data
	@NoArgsConstructor
	public static class GstDetails {
		@JsonProperty("gst_liable")
		private String gstLiable;
		private String amount;

	}
}
