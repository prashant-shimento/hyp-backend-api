package com.hyp.translation;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.hyp.constants.Constants;
import com.hyp.entity.Customer;
import com.hyp.entity.Order;
import com.hyp.entity.Order.OrderAddonItem;
import com.hyp.entity.Order.OrderDiscount;
import com.hyp.entity.Order.OrderItem;
import com.hyp.entity.Order.OrderItemTax;
import com.hyp.entity.Order.OrderTax;
import com.hyp.entity.Restaurant;
import com.hyp.enums.DiscountType;
import com.hyp.enums.OrderType;
import com.hyp.enums.RiderStatusType;
import com.hyp.enums.TaxType;
import com.hyp.exception.RequestTranslationException;
import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosOrderRequest.AddonItemDetails;
import com.hyp.request.PosOrderRequest.AddonItemOrderRequest;
import com.hyp.request.PosOrderRequest.CustomerDetails;
import com.hyp.request.PosOrderRequest.CustomerOrderRequest;
import com.hyp.request.PosOrderRequest.DiscountDetails;
import com.hyp.request.PosOrderRequest.DiscountOrderRequest;
import com.hyp.request.PosOrderRequest.GstDetails;
import com.hyp.request.PosOrderRequest.ItemTax;
import com.hyp.request.PosOrderRequest.OrderDetails;
import com.hyp.request.PosOrderRequest.OrderInfo;
import com.hyp.request.PosOrderRequest.OrderInfoDetails;
import com.hyp.request.PosOrderRequest.OrderItemDetails;
import com.hyp.request.PosOrderRequest.OrderItemRequest;
import com.hyp.request.PosOrderRequest.OrderRequest;
import com.hyp.request.PosOrderRequest.RestaurantDetails;
import com.hyp.request.PosOrderRequest.RestaurantOrderRequest;
import com.hyp.request.PosOrderRequest.TaxDetails;
import com.hyp.request.PosOrderRequest.TaxOrderRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest.RiderDetails;
import com.hyp.util.CommonUtils;

public class PosOrderRequestTranslation {

	// @Value("${pos.petpooja.token}")
	private static String accessToken = "1306d6f5f3332a3e3a35c77909f8cdf77831cef3";

	// @Value("${pos.petpooja.secret}")
	private static String appSecret = "154e9636cfb4a98a00933f6aeffbbd92c814e482";

	// @Value("${pos.petpooja.key}")
	private static String appKey = "13yrzmb9pivxscoh25gdj0t7fk48uaqw";

	// @Value("${myapp.domain}")
	private static String domain = "https://aardvark-notable-terminally.ngrok-free.app/hyp-backend-api";

	public static PosOrderUpdateRequest getPosOrderUpdateRequest(Restaurant restaurant, Order order,
			String cancelReason) throws RequestTranslationException {
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
			throw new RequestTranslationException(Constants.PET_POOJA, e.getMessage());
		}
		return posOrderUpdateRequest;
	}

	public static PosRiderUpdateRequest getPosRiderStatusUpdateRequest(Restaurant restaurant, Order order,
			RiderDetails riderDetails,RiderStatusType riderStatus) {
		PosRiderUpdateRequest posRiderUpdateRequest = new PosRiderUpdateRequest();
		try {
			posRiderUpdateRequest.setAccessToken(accessToken);
			posRiderUpdateRequest.setAppKey(appKey);
			posRiderUpdateRequest.setAppSecret(appSecret);
			posRiderUpdateRequest.setRestaurantId(restaurant.getMenuSharingCode());
			posRiderUpdateRequest.setOrderId(order.getId());
			posRiderUpdateRequest.setRiderData(riderDetails);
			posRiderUpdateRequest.setStatus(String.valueOf(riderStatus));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return posRiderUpdateRequest;
	}

	public static PosOrderRequest getPosOrderRequest(Restaurant restaurant, Order order, Customer customer) throws RequestTranslationException {
		PosOrderRequest posOrderRequest = new PosOrderRequest();
		try {
			posOrderRequest.setAccessToken(accessToken);
			posOrderRequest.setAppKey(appKey);
			posOrderRequest.setAppSecret(appSecret);
			posOrderRequest.setOrderInfo(getOrderInfo(restaurant, order, customer));
		} catch (Exception e) {
			e.printStackTrace();
			throw new RequestTranslationException(Constants.PET_POOJA, e.getMessage());
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
		orderInfoDetails.setTax(getTax(order.getOrderTax()));
		return orderInfoDetails;
	}

	public static CustomerOrderRequest getCustomerDetails(Customer customer) {
		CustomerDetails customerDetails = new CustomerDetails();
		customerDetails.setEmail(customer.getEmail());
		customerDetails.setLatitude(String.valueOf(customer.getAddress().getLocation().getLatitude()));
		customerDetails.setLongitude(String.valueOf(customer.getAddress().getLocation().getLongitude()));
		customerDetails.setName(customer.getName());
		customerDetails.setPhone(customer.getMobile());
		CustomerOrderRequest customerRequest = new CustomerOrderRequest();
		customerRequest.setCustomerDetails(customerDetails);
		return customerRequest;
	}

	public static OrderRequest getOrderDetails(Order order) {
		OrderDetails orderDetails = new OrderDetails();
		orderDetails.setOrderId(order.getId());
		orderDetails.setOtp(CommonUtils.emptyIfNullOrZeroToString(0));
		orderDetails.setPreOrderDate(order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
		orderDetails.setPreOrderTime(order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
		orderDetails.setServiceCharge(CommonUtils.emptyIfNullOrZeroToString(order.getServiceCharge()));
		orderDetails.setScTaxAmount(CommonUtils.emptyIfNullOrZeroToString(order.getScTaxAmount()));
		orderDetails.setDeliveryCharges(CommonUtils.emptyIfNullOrZeroToString(order.getDeliveryCharge()));
		orderDetails.setDcTaxAmount(CommonUtils.emptyIfNullOrZeroToString(order.getDcTaxAmount()));
		orderDetails.setPackingCharges(CommonUtils.emptyIfNullOrZeroToString(order.getPackagingCharge()));
		orderDetails.setPcTaxAmount(CommonUtils.emptyIfNullOrZeroToString(order.getPcTaxAmount()));
		orderDetails.setOrderType(OrderType.fromCode(order.getOrderType()).toString());
		orderDetails.setAdvancedOrder("N");
		orderDetails.setPaymentType(String.valueOf(order.getPaymentType()));
		orderDetails.setDiscountTotal(CommonUtils.emptyIfNullOrZeroToString(order.getDiscountAmount()));
		orderDetails.setTaxTotal(CommonUtils.emptyIfNullOrZeroToString(order.getTaxAmount()));
		if(order.getDiscountType() != null) {
			orderDetails.setDiscountType(DiscountType.fromCode(order.getDiscountType()));
		}		
		orderDetails.setTotal(CommonUtils.emptyIfNullOrZeroToString(order.getTotalAmount()));
		orderDetails.setDescription(order.getDescription());
		orderDetails.setCreatedOn(order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		orderDetails.setEnableDelivery(Constants.VENDOR_HANDLE_DELIVERY);
		orderDetails.setCallbackUrl(domain + "/pos/order/callback");
		orderDetails.setDcGstDetails(getGstDetails(null, null));
		orderDetails.setPcGstDetails(getGstDetails(null, null));
		orderDetails.setCollectCash(CommonUtils.emptyIfNullOrZeroToString(null));
		orderDetails.setMinPrepTime(CommonUtils.emptyIfNullOrZeroToString(order.getMinPrepTime()));
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
			orderItemDetails.setVariationName(CommonUtils.emptyIfNullOrZeroToString(orderItem.getVariationName()));
			orderItemDetails.setVariationId(CommonUtils.emptyIfNullOrZeroToString(orderItem.getVariationId()));
			orderItemDetails.setGstLiability(Constants.GST_VENDOR_LIABLE);
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
		AddonItemOrderRequest addonItemOrderRequest = null;
		if (orderAddonItems != null) {
			addonItemOrderRequest = new AddonItemOrderRequest();
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
		}
		return addonItemOrderRequest;
	}

	public static TaxOrderRequest getTax(List<OrderTax> taxList) {
		TaxOrderRequest taxOrderRequest = null;
		if (taxList != null) {
			taxOrderRequest = new TaxOrderRequest();
			List<TaxDetails> taxDetailsList = new ArrayList<>();
			for (OrderTax tax : taxList) {
				TaxDetails taxDetails = new TaxDetails();
				taxDetails.setId(tax.getId());
				taxDetails.setPrice(CommonUtils.emptyIfNullOrZeroToString(tax.getPrice()));
				taxDetails.setTax(CommonUtils.emptyIfNullOrZeroToString(tax.getTax()));
				taxDetails.setTitle(tax.getTitle());
				taxDetails.setType(TaxType.fromCode(tax.getType()));
				taxDetails.setRestaurantLiableAmt(CommonUtils.emptyIfNullOrZeroToString(null));
				taxDetailsList.add(taxDetails);
			}
			taxOrderRequest.setDetails(taxDetailsList);
		}
		return taxOrderRequest;
	}

	public static DiscountOrderRequest getDiscount(List<OrderDiscount> orderDiscountList) {
		DiscountOrderRequest discountOrderRequest = null;
		if (orderDiscountList != null) {
			discountOrderRequest = new DiscountOrderRequest();
			List<DiscountDetails> discountDetailsList = new ArrayList<>();
			for (OrderDiscount orderDiscount : orderDiscountList) {
				DiscountDetails discountDetails = new DiscountDetails();
				discountDetails.setId(orderDiscount.getId());
				discountDetails.setTitle(orderDiscount.getTitle());
				discountDetails.setType(orderDiscount.getType());
				discountDetails.setPrice(orderDiscount.getPrice());
				discountDetailsList.add(discountDetails);
			}
			discountOrderRequest.setDetails(discountDetailsList);
		}
		return discountOrderRequest;
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

	public static List<GstDetails> getGstDetails(String gstLiable, String amount) {
		List<GstDetails> gstDetailsList = new ArrayList<>();
		GstDetails gstdetails = new GstDetails();
		gstdetails.setGstLiable(CommonUtils.emptyIfNullOrZeroToString(gstLiable));
		gstdetails.setAmount(CommonUtils.emptyIfNullOrZeroToString(amount));
		gstDetailsList.add(gstdetails);
		return gstDetailsList;
	}
}
