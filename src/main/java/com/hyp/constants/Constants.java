package com.hyp.constants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	List<String> RESTAURANT_API_PARAMS = Arrays.asList("restaurantName", "menuSharingCode", "contact");
	List<String> PAYMENT_API_PARAMS = Arrays.asList("id", "orderId", "paymentOrderId","paymentId","provider","status","amount","paymentId");

	// Constants for logging API integration
	String INBOUND_API_LOG = "InboundAPILog";
	String OUTBOUND_API_LOG = "OutboundAPILog";

	public enum ApiStatus {
		SUCCESSFUL, FAILURE
	}

	List<String> FILE_EXTENSIONS = Arrays.asList(".jpg", ".png", ".jpeg");

	static Map<String, String> getContentTypes() {
		Map<String, String> contentTypes = new HashMap<>();
		contentTypes.put(".jpg", "image/jpeg");
		contentTypes.put(".jpeg", "image/jpeg");
		contentTypes.put(".png", "image/png");
		contentTypes.put(".gif", "image/gif");
		return contentTypes;
	}

	String META_WHATSAPP = "whatsapp";
	String META_RECIPIENT_TYPE = "";
	String TEMPLATE = "template";
	String META_ORDER_CONFIRMED_TEMPLATE = "order_confirmation";
	String META_ORDER_PICKEDUP_TEMPLATE = "order_pickedup";
	String META_ORDER_DELIVERED_TEMPLATE = "order_delivered_alert";
	String META_ORDER_CANCELLED_TEMPLATE = "order_cancelled";
	String META_ORDER_ALERT_TEMPLATE = "order_alert";
	String META_DELIVERY_DELAY_ALERT_TEMPLATE = "delivery_delay_alert";
	String META_ORDER_PAID_TEMPLATE = "order_payment_success";
	String META_ORDER_CREATED_THEATRE_TEMPLATE = "theatre_order_placed";
	String META_ORDER_CONFIRMED_THEATRE_TEMPLATE = "theatre_order_accepted";
	String META_ORDER_DELIVERED_THEATRE_TEMPLATE = "theatre_order_delivered";
	String META_GENERIC_ALERT_TEMPLATE = "generic_alert";



	String DEFAULT_LOGO = "https://storage.googleapis.com/hyp-app-bucket/default-logo.png";

	public static final String SUPPORTED_DATE_FORMATS = "yyyy-MM-dd'T'HH:mm:ss.SSS" ;

	String WELCOME_MESSAGE = "Welcome to *Rasyumm*, View Our Digital Menu";
	String FACEBOOK_MESSAGE = "*Rasyumm*:Thank you for visiting our restaurant! We value your feedback. Please rate your experience between 1 and 5";
}
