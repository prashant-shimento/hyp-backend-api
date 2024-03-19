package com.hyp.translation;

import com.hyp.entity.Customer;
import com.hyp.entity.Order;
import com.hyp.entity.Order.OrderAddonItem;
import com.hyp.entity.Order.OrderItem;
import com.hyp.entity.Order.OrderItemTax;
import com.hyp.enums.PosOrderStatusType;
import com.hyp.enums.RiderStatusType;
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
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest.RiderDetails;
import com.hyp.request.PosOrderRequest.TaxDetails;
import com.hyp.request.PosOrderRequest.OrderRequest;
import com.hyp.request.PosOrderRequest.DiscountOrderRequest;
import com.hyp.request.PosOrderRequest.DiscountDetails;
import com.hyp.request.PosOrderRequest.GstDetails;
import com.hyp.request.PosOrderRequest.ItemTax;
import com.hyp.request.PosOrderRequest.AddonItemOrderRequest;
import com.hyp.request.PosOrderRequest.AddonItemDetails;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;

public class PosOrderRequestTranslation {

	//@Value("${pos.petpooja.token}")
	private static String accessToken = "1306d6f5f3332a3e3a35c77909f8cdf77831cef3";

	//@Value("${pos.petpooja.secret}")
	private static String appSecret = "154e9636cfb4a98a00933f6aeffbbd92c814e482";

	//@Value("${pos.petpooja.key}")
	private static String appKey = "13yrzmb9pivxscoh25gdj0t7fk48uaqw";

	//@Value("${myapp.domain}")
	private static String domain = "https://aardvark-notable-terminally.ngrok-free.app";

	public static PosOrderUpdateRequest getPosOrderUpdateRequest(Restaurant restaurant, Order order,String cancelReason) {
		PosOrderUpdateRequest posOrderUpdateRequest = new PosOrderUpdateRequest();
		try {
			posOrderUpdateRequest.setAccessToken(accessToken);
			posOrderUpdateRequest.setAppKey(appKey);
			posOrderUpdateRequest.setAppSecret(appSecret);
			posOrderUpdateRequest.setClientOrderId(order.getId());
			posOrderUpdateRequest.setRestaurantId(restaurant.getMenuSharingCode());
			posOrderUpdateRequest.setOrderId("");
			posOrderUpdateRequest.setCancelReason(cancelReason);
			posOrderUpdateRequest.setStatus("-1");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return posOrderUpdateRequest;
	}

	public static PosRiderUpdateRequest getPosRiderStatusUpdateRequest(Restaurant restaurant, Order order,
			RiderStatusType riderStatus) {
		PosRiderUpdateRequest posRiderUpdateRequest = new PosRiderUpdateRequest();
		try {
			posRiderUpdateRequest.setAccessToken(accessToken);
			posRiderUpdateRequest.setAppKey(appKey);
			posRiderUpdateRequest.setAppSecret(appSecret);
			posRiderUpdateRequest.setRestaurantId(restaurant.getMenuSharingCode());
			posRiderUpdateRequest.setOrderId(order.getId());
			posRiderUpdateRequest.setRiderData(new RiderDetails("rider", "9964552656"));
			posRiderUpdateRequest.setStatus(String.valueOf(riderStatus));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return posRiderUpdateRequest;
	}

	public static PosOrderRequest getPosOrderRequest(Restaurant restaurant, Order order, Customer customer) {
		PosOrderRequest posOrderRequest = new PosOrderRequest();
		try {
			posOrderRequest.setAccessToken(accessToken);
			posOrderRequest.setAppKey(appKey);
			posOrderRequest.setAppSecret(appSecret);
			posOrderRequest.setOrderInfo(getOrderInfo(restaurant, order, customer));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return posOrderRequest;
	}

	public static OrderInfo getOrderInfo(Restaurant restaurant, Order order, Customer customer) {
		OrderInfo orderInfo = new OrderInfo();
		orderInfo.setOrderInfoDetails(getOrderInfoDetails(restaurant, order, customer));
		orderInfo.setDeviceType("");
		orderInfo.setUdid("");
		return orderInfo;
	}

	public static OrderInfoDetails getOrderInfoDetails(Restaurant restaurant, Order order, Customer customer) {
		OrderInfoDetails orderInfoDetails = new OrderInfoDetails();
		orderInfoDetails.setRestaurant(getRestaurantDetails(restaurant));
		orderInfoDetails.setCustomer(getCustomerDetails(customer));
		orderInfoDetails.setOrder(getOrderDetails(order));
		orderInfoDetails.setOrderItem(getOrderItems(order));
		orderInfoDetails.setTax(getTax());
		orderInfoDetails.setDiscount(getDiscount());
		return orderInfoDetails;
	}

	public static CustomerOrderRequest getCustomerDetails(Customer customer) {
		CustomerDetails customerDetails = new CustomerDetails();
		customerDetails.setEmail(customer.getEmail());
		customerDetails.setLatitude("34.11752681212772");
		customerDetails.setLongitude("74.72949172653219");
		customerDetails.setName(customer.getName());
		;
		customerDetails.setPhone(customer.getMobile());
		CustomerOrderRequest customerRequest = new CustomerOrderRequest();
		customerRequest.setCustomerDetails(customerDetails);
		return customerRequest;
	}

	public static OrderRequest getOrderDetails(Order order) {
		OrderDetails orderDetails = new OrderDetails();
		orderDetails.setOrderId(order.getId());
		orderDetails.setPreOrderDate(null);
		orderDetails.setPreOrderTime(null);
		orderDetails.setServiceCharge(String.valueOf(order.getServiceCharge()));
		orderDetails.setScTaxAmount(String.valueOf(order.getScTaxAmount()));
		orderDetails.setDeliveryCharges(String.valueOf(order.getDeliveryCharge()));
		orderDetails.setDcTaxAmount(String.valueOf(order.getDcTaxAmount()));
		orderDetails.setPackingCharges(String.valueOf(order.getPackagingCharge()));
		orderDetails.setPcTaxAmount(String.valueOf(order.getPcTaxAmount()));
		orderDetails.setOrderType(order.getOrderType());
		orderDetails.setAdvancedOrder("N");
		orderDetails.setPaymentType(String.valueOf(order.getPaymentType()));
		orderDetails.setDiscountTotal(String.valueOf(order.getDiscountAmount()));
		orderDetails.setTaxTotal(String.valueOf(order.getTaxAmount()));
		orderDetails.setDiscountType(order.getDiscountType());
		orderDetails.setTotal(String.valueOf(order.getTotalAmount()));
		orderDetails.setDescription(order.getDescription());
		orderDetails.setCreatedOn(order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		orderDetails.setEnableDelivery(1);
		orderDetails.setCallbackUrl(domain + "/api/order/callback");
		orderDetails.setDcGstDetails(getGstDetails());
		orderDetails.setPcGstDetails(getGstDetails());
		OrderRequest orderRequest = new OrderRequest();
		orderRequest.setOrderDetails(orderDetails);
		return orderRequest;
	}

	public static OrderItemRequest getOrderItems(Order order) {

		OrderItemRequest orderItemRequest = new OrderItemRequest();
		List<OrderItemDetails> orderItemList = new ArrayList<>();
		for (OrderItem orderItem : order.getOrderItems()) {
			OrderItemDetails orderItemDetails = new OrderItemDetails();
			orderItemDetails.setId(orderItem.getId());
			orderItemDetails.setName(orderItem.getName());
			orderItemDetails.setItemDiscount(String.valueOf(orderItem.getItemDiscount()));
			orderItemDetails.setPrice(String.valueOf(orderItem.getPrice()));
			orderItemDetails.setFinalPrice(String.valueOf(orderItem.getFinalPrice()));
			orderItemDetails.setQuantity(String.valueOf(orderItem.getQuantity()));
			orderItemDetails.setVariationName(orderItem.getVariationName());
			orderItemDetails.setVariationId(orderItem.getVariationId());
			orderItemDetails.setAddonItems(getAddonItems(orderItem.getOrderAddonItems()));
			orderItemDetails.setItemTax(getItemTax(orderItem.getOrderItemTax()));

			orderItemList.add(orderItemDetails);
		}
		orderItemRequest.setDetails(orderItemList);
		return orderItemRequest;
	}

	public static List<ItemTax> getItemTax(List<OrderItemTax> orderItemTaxList) {
		List<ItemTax> itemTaxList = new ArrayList<>();
		for (OrderItemTax orderItemTax : orderItemTaxList) {
			ItemTax itemTax = new ItemTax();
			itemTax.setId(orderItemTax.getId());
			itemTax.setName(orderItemTax.getName());
			itemTax.setAmount(String.valueOf(orderItemTax.getAmount()));
			itemTaxList.add(itemTax);
		}
		return itemTaxList;
	}

	public static AddonItemOrderRequest getAddonItems(List<OrderAddonItem> orderAddonItems) {
		AddonItemOrderRequest addonItemOrderRequest = new AddonItemOrderRequest();
		List<AddonItemDetails> addonItemDetailsList = new ArrayList<>();
		for (OrderAddonItem orderAddonItem : orderAddonItems) {
			AddonItemDetails addonItemDetails = new AddonItemDetails();
			addonItemDetails.setId(orderAddonItem.getAddonItemId());
			addonItemDetails.setName(orderAddonItem.getAddonItemName());
			addonItemDetails.setGroupName(orderAddonItem.getAddonGroupName());
			addonItemDetails.setPrice(String.valueOf(orderAddonItem.getPrice()));
			addonItemDetails.setGroupId(Integer.parseInt(orderAddonItem.getAddonGroupId()));
			addonItemDetails.setQuantity(String.valueOf(orderAddonItem.getQuantity()));
			addonItemDetailsList.add(addonItemDetails);
		}
		addonItemOrderRequest.setDetails(addonItemDetailsList);
		return addonItemOrderRequest;
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
		restaurantDetails.setRestId(restaurant.getMenuSharingCode());
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
