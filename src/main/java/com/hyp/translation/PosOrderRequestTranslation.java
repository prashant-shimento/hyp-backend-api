package com.hyp.translation;

import com.hyp.entity.AddonItem;
import com.hyp.entity.Item;
import com.hyp.entity.Restaurant;
import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosOrderRequest.CustomerOrderRequest;
import com.hyp.request.PosOrderRequest.CustomerDetails;
import com.hyp.request.PosOrderRequest.OrderInfo;
import com.hyp.request.PosOrderRequest.OrderInfoDetails;
import com.hyp.request.PosOrderRequest.OrderItemRequest;
import com.hyp.request.PosOrderRequest.OrderItemDetails;
import com.hyp.request.PosOrderRequest.RestaurantOrderRequest;
import com.hyp.request.PosOrderRequest.RestaurantDetails;
import com.hyp.request.PosOrderRequest.OrderDetails;
import com.hyp.request.PosOrderRequest.TaxOrderRequest;
import com.hyp.request.PosOrderRequest.TaxDetails;
import com.hyp.request.PosOrderRequest.OrderRequest;
import com.hyp.request.PosOrderRequest.DiscountOrderRequest;
import com.hyp.request.PosOrderRequest.DiscountDetails;
import com.hyp.request.PosOrderRequest.GstDetails;
import com.hyp.request.PosOrderRequest.ItemTax;
import com.hyp.request.PosOrderRequest.AddonItemOrderRequest;
import com.hyp.request.PosOrderRequest.AddonItemDetails;

import java.util.ArrayList;
import java.util.List;

public class PosOrderRequestTranslation {

	public static PosOrderRequest getPosOrderRequest(Restaurant restaurant,Item item, AddonItem addonItem) {
		PosOrderRequest posOrderRequest = new PosOrderRequest();
		posOrderRequest.setAccessToken("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
		posOrderRequest.setAppKey("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
		posOrderRequest.setAppSecret("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
		posOrderRequest.setOrderInfo(getOrderInfo(restaurant,item,addonItem));
		return posOrderRequest;
	}

	public static OrderInfo getOrderInfo(Restaurant restaurant,Item item, AddonItem addonItem) {
		OrderInfo orderInfo = new OrderInfo();
		orderInfo.setOrderInfoDetails(getOrderInfoDetails(restaurant,item,addonItem));
		orderInfo.setDeviceType("");
		orderInfo.setUdid("");
		return orderInfo;
	}

	public static OrderInfoDetails getOrderInfoDetails(Restaurant restaurant,Item item, AddonItem addonItem) {
		OrderInfoDetails orderInfoDetails = new OrderInfoDetails();
		orderInfoDetails.setRestaurant(getRestaurantDetails(restaurant));
		orderInfoDetails.setCustomer(getCustomerDetails());
		orderInfoDetails.setOrder(getOrderDetails());
		orderInfoDetails.setOrderItem(getOrderItems(item,addonItem));
		orderInfoDetails.setTax(getTax());
		orderInfoDetails.setDiscount(getDiscount());
		return orderInfoDetails;
	}

	public static CustomerOrderRequest getCustomerDetails() {
		CustomerDetails customerDetails = new CustomerDetails();
		customerDetails.setEmail("xxx@yahoo.com");
		customerDetails.setLatitude("34.11752681212772");
		customerDetails.setLongitude("74.72949172653219");
		customerDetails.setName("Advait");
		customerDetails.setPhone("9090909090");
		CustomerOrderRequest customer = new CustomerOrderRequest();
		customer.setCustomerDetails(customerDetails);
		return customer;
	}

	public static OrderRequest getOrderDetails() {
		OrderDetails orderDetails = new OrderDetails();
		orderDetails.setOrderId("A-1");
		orderDetails.setPreOrderDate(null);
		orderDetails.setPreOrderTime(null);
		orderDetails.setServiceCharge("0");
		orderDetails.setScTaxAmount("0");
		orderDetails.setDeliveryCharges("50");
		orderDetails.setDcTaxAmount("2.5");
		orderDetails.setPackingCharges("20");
		orderDetails.setPcTaxAmount("1");
		orderDetails.setOrderType("H");
		orderDetails.setOndcBap("buyerAppName");
		orderDetails.setAdvancedOrder("N");
		orderDetails.setPaymentType("COD");
		orderDetails.setTableNo("");
		orderDetails.setNoOfPersons("0");
		orderDetails.setDiscountTotal("45");
		orderDetails.setTaxTotal("65.52");
		orderDetails.setDiscountType("F");
		orderDetails.setTotal("560");
		orderDetails.setDescription("");
		orderDetails.setCreatedOn("2022-01-01 15:49:00");
		orderDetails.setEnableDelivery(1);
		orderDetails.setMinPrepTime(20);
		orderDetails.setCallbackUrl("https.xyz.abc");
		orderDetails.setCollectCash("480");
		orderDetails.setOtp("9876");
		orderDetails.setDcGstDetails(getGstDetails());
		orderDetails.setPcGstDetails(getGstDetails());
		OrderRequest order = new OrderRequest();
		order.setOrderDetails(orderDetails);
		return order;
	}

	public static OrderItemRequest getOrderItems(Item item, AddonItem addonItem) {
		OrderItemRequest orderItem = new OrderItemRequest();
		List<OrderItemDetails> orderItemsList = new ArrayList<>();
		OrderItemDetails orderItemDetails = new OrderItemDetails();
		orderItemDetails.setId(item.getId());
		orderItemDetails.setName(item.getItemName());
		orderItemDetails.setGstLiability(null);
		orderItemDetails.setItemTax(getItemTax());
		orderItemDetails.setItemDiscount(null);
		orderItemDetails.setPrice(item.getPrice());
		orderItemDetails.setFinalPrice(null);
		orderItemDetails.setQuantity(null);
		orderItemDetails.setVariationName(null);
		orderItemDetails.setVariationId(null);
		orderItemDetails.setAddonItems(getAddonItems(addonItem));
		orderItemsList.add(orderItemDetails);
		orderItem.setDetails(orderItemsList);
		return orderItem;
	}

	public static List<ItemTax> getItemTax() {
		List<ItemTax> itemTaxList = new ArrayList<>();
		ItemTax itemTax = new ItemTax();
		itemTax.setId(null);
		itemTax.setName(null);
		itemTax.setAmount(null);
		itemTaxList.add(itemTax);
		return itemTaxList;
	}

	public static AddonItemOrderRequest getAddonItems(AddonItem addonItem) {
		AddonItemOrderRequest addonItemRequest = new AddonItemOrderRequest();
		List<AddonItemDetails> addonItemDetailsList = new ArrayList<>();
		AddonItemDetails addonItemDetails = new AddonItemDetails();
		addonItemDetails.setId(null);
		addonItemDetails.setName(null);
		addonItemDetails.setGroupName(null);
		addonItemDetails.setPrice(null);
		addonItemDetails.setGroupId(0);
		addonItemDetails.setQuantity(null);
		addonItemDetailsList.add(addonItemDetails);
		addonItemRequest.setDetails(null);
		return addonItemRequest;
	}

	public static TaxOrderRequest getTax() {
		TaxOrderRequest tax = new TaxOrderRequest();
		TaxDetails taxDetails = new TaxDetails();
		taxDetails.setId(null);
		taxDetails.setPrice(null);
		taxDetails.setTax(null);
		taxDetails.setTitle(null);
		taxDetails.setType(null);
		taxDetails.setRestaurantLiableAmt(null);
		List<TaxDetails> taxDetailsList = new ArrayList<>();
		taxDetailsList.add(taxDetails);
		tax.setDetails(taxDetailsList);
		return tax;
	}

	public static DiscountOrderRequest getDiscount() {
		DiscountOrderRequest discount = new DiscountOrderRequest();
		List<DiscountDetails> discountDetailsList = new ArrayList<>();
		DiscountDetails discountDetails = new DiscountDetails();
		discountDetails.setId(null);
		discountDetails.setTitle(null);
		discountDetails.setType(null);
		discountDetails.setPrice(null);
		discountDetailsList.add(discountDetails);
		discount.setDetails(discountDetailsList);
		return discount;
	}

	public static RestaurantOrderRequest getRestaurantDetails(Restaurant restaurant) {
		RestaurantDetails restaurantDetails = new RestaurantDetails();
		restaurantDetails.setResName(restaurant.getRestaurantName());
		restaurantDetails.setAddress(restaurant.getAddress());
		restaurantDetails.setContactInformation(restaurant.getContact());
		restaurantDetails.setRestId(restaurant.getRestaurantId());
		RestaurantOrderRequest restaurantRequest = new RestaurantOrderRequest();
		restaurantRequest.setRestaurantDetails(restaurantDetails);
		return restaurantRequest;
	}

	public static List<GstDetails> getGstDetails() {
		List<GstDetails> gstDetailsList = new ArrayList<>();
		GstDetails gstdetails = new GstDetails();
		gstdetails.setGstLiable(null);
		gstdetails.setAmount(null);
		gstDetailsList.add(gstdetails);
		return gstDetailsList;
	}
}
