package com.hyp.constants;

import java.util.Arrays;
import java.util.List;

public interface Constants {

	String RAZOR_PAY = "RAZOR_PAY";
	int VENDOR_HANDLE_DELIVERY = 0;
	int RESTAURANT_HANDLE_DELIVERY = 1;
	String GST_VENDOR_LIABLE = "vendor";
	String GST_RESTAURANT_LIABLE = "restaurant";
	String PET_POOJA = "PET_POOJA";
	String PIDGE = "PIDGE";
	String SYSTEM = "SYSTEM";
	String API = "API";
	String SMART = "SMART";
	String ON_CANCEL = "ON_CANCEL";

	List<String> CUSTOMER_API_PARAMS = Arrays.asList("id", "name", "mobile", "createdAt");
	List<String> ORDER_API_PARAMS = Arrays.asList("id", "status", "customerId", "createdAt", "totalAmount", "orderType",
			"orderTime", "restaurantId");
	List<String> ITEM_API_PARAMS = Arrays.asList("id", "itemName", "inStock", "itemAllowVariation", "itemCategoryId",
			"itemAllowAddon", "itemAttributeId", "status", "restaurantId");
	List<String> ADDRESS_API_PARAMS = Arrays.asList("id", "customerId", "addressType", "restaurantId");
	List<String> DELIVERY_API_PARAMS = Arrays.asList("id", "networkId", "status", "orderId", "service",
			"deliveryOrderId", "channel", "restaurantId");
	List<String> CONTENT_API_PARAMS = Arrays.asList("id", "type", "restaurantId");
	List<String> VARIATIONS_API_PARAMS = Arrays.asList("id", "name", "variationId", "variationAllowAddon",
			"restaurantId");
	List<String> DATE_API_PARAMS = Arrays.asList("createdAt", "orderTime");
	List<String> CATEGORIES_API_PARAMS = Arrays.asList("id", "restaurantId", "categoryName", "active");
	List<String> PARTNER_API_PARAMS = Arrays.asList("id", "name", "domain", "active", "type");
	List<String> RESTAURANT_API_PARAMS = Arrays.asList("restaurantName","menuSharingCode","contact");

	// Constants for logging API integration
	String INBOUND_API_LOG = "InboundAPILog";
	String OUTBOUND_API_LOG = "OutboundAPILog";

	public enum ApiStatus {
		SUCCESSFUL, FAILURE
	}

	String META_WHATSAPP = "whatsapp";
	String META_RECIPIENT_TYPE = "";
	String TEMPLATE = "template";
	String META_ORDER_CONFIRMED_TEMPLATE = "order_confirmation";
	String META_ORDER_PICKEDUP_TEMPLATE = "order_pickedup";
	String META_ORDER_DELIVERED_TEMPLATE = "order_delivered";
	String META_ORDER_CANCELLED_TEMPLATE = "order_cancelled";
	String META_ORDER_ALERT_TEMPLATE = "order_alert";

	String DEFAULT_LOGO = "https://storage.googleapis.com/hyp-app-bucket/default-logo.png";

	String WELCOME_MESSAGE = "Welcome to *Rasyumm*, View Our Digital Menu";
	String FACEBOOK_MESSAGE = "*Rasyumm*:Thank you for visiting our restaurant! We value your feedback. Please rate your experience between 1 and 5";
}
