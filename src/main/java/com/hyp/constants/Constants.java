package com.hyp.constants;

import com.hyp.enums.OrderStatusType;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Constants {

    String RAZOR_PAY = "RAZOR_PAY";
    int VENDOR_HANDLE_DELIVERY = 0;
    int RESTAURANT_HANDLE_DELIVERY = 1;
    String GST_RESTAURANT_LIABLE = "restaurant";
    String PET_POOJA = "PET_POOJA";
    String URBAN_PIPER = "urbanpiper";
    String SYSTEM = "SYSTEM";
    String API = "API";

    List<String> CUSTOMER_API_PARAMS = Arrays.asList("id", "name", "mobile", "restaurants", "createdAt");
    List<String> ORDER_API_PARAMS = Arrays.asList(
            "id",
            "status",
            "customerId",
            "preOrder",
            "createdAt",
            "totalAmount",
            "orderType",
            "orderTime",
            "restaurantId",
            "partnerId",
            "offerCode");
    List<String> ITEM_API_PARAMS = Arrays.asList(
            "id",
            "itemName",
            "inStock",
            "itemAllowVariation",
            "itemCategoryId",
            "itemAllowAddon",
            "itemAttributeId",
            "status",
            "restaurantId");
    List<String> ADDRESS_API_PARAMS = Arrays.asList("id", "customerId", "addressType", "restaurantId");
    List<String> DELIVERY_API_PARAMS = Arrays.asList(
            "id", "networkId", "status", "orderId", "service", "deliveryOrderId", "channel", "restaurantId");
    List<String> CONTENT_API_PARAMS = Arrays.asList("id", "type", "restaurantId");
    List<String> VARIATIONS_API_PARAMS =
            Arrays.asList("id", "name", "variationId", "variationAllowAddon", "restaurantId");
    List<String> DATE_API_PARAMS = Arrays.asList("createdAt", "orderTime");
    List<String> CATEGORIES_API_PARAMS = Arrays.asList("id", "restaurantId", "categoryName", "active");
    List<String> PARTNER_API_PARAMS = Arrays.asList("id", "name", "domain", "active", "type");
    List<String> RESTAURANT_API_PARAMS = Arrays.asList(
            "id", "restaurantName", "active", "serviceable", "menuSharingCode", "contact", "discoverable", "pincode");
    List<String> PAYMENT_API_PARAMS =
            Arrays.asList("id", "orderId", "paymentOrderId", "paymentId", "provider", "status", "amount", "paymentId");
    List<String> ORDER_TYPE_API_PARAMS = Arrays.asList("id", "orderType", "orderTypeId", "restaurantId");
    List<String> CUSTOMER_TESTIMONIAL_PARAMS = List.of("partnerId");
    List<String> FILE_EXTENSIONS = Arrays.asList(".jpg", ".png", ".jpeg");
    List<String> BOOLEAN_API_PARAMS = Arrays.asList("active", "serviceable");
    List<String> FEE_API_PARAMS = Arrays.asList("id", "partnerId", "restaurantId", "active");
    List<String> SETTLEMENT_API_PARAMS =
            Arrays.asList("id", "partnerId", "restaurantId", "orderId", "createdAt", "updatedAt", "settlementStatus");
    List<String> REFERRAL_CODE_API_PARAMS =
            Arrays.asList("id", "referrerName", "partnerId", "code", "createdAt", "updatedAt");
    List<String> REFERRAL_TOKEN_API_PARAMS = Arrays.asList(
            "id", "token", "referralCode", "restaurantId", "source", "createdAt", "updatedAt", "usedOrder");
    List<String> TAX_API_PARAMS = Arrays.asList("id", "restaurantId");
    List<String> USERS_PARAM = Arrays.asList("id", "restaurantId", "partnerId", "email");
    List<String> RIDER_RECORD_PARAM = Arrays.asList("id", "riderName", "riderContact");

    static Map<String, String> getContentTypes() {
        Map<String, String> contentTypes = new HashMap<>();
        contentTypes.put(".jpg", "image/jpeg");
        contentTypes.put(".jpeg", "image/jpeg");
        contentTypes.put(".png", "image/png");
        contentTypes.put(".gif", "image/gif");
        return contentTypes;
    }

    String META_WHATSAPP = "whatsapp";
    String TEMPLATE = "template";
    String META_ORDER_CONFIRMED_TEMPLATE = "order_confirmation";
    String META_ORDER_PICKEDUP_TEMPLATE = "order_pickedup";
    String META_ORDER_DELIVERED_TEMPLATE = "delivered_order";
    String META_ORDER_CANCELLED_TEMPLATE = "order_cancelled";
    String META_ORDER_ALERT_TEMPLATE = "order_alert";
    String META_DELIVERY_DELAY_ALERT_TEMPLATE = "delivery_delay_alert";
    String META_ORDER_PAID_TEMPLATE = "order_payment_success";
    String META_ORDER_CREATED_THEATRE_TEMPLATE = "theatre_order_placed";
    String META_ORDER_CONFIRMED_THEATRE_TEMPLATE = "theatre_order_accepted";
    String META_ORDER_DELIVERED_THEATRE_TEMPLATE = "theatre_order_delivered";
    String META_GENERIC_ALERT_TEMPLATE = "generic_alert";
    String META_MENU_PUSH_ALERT_TEMPLATE = "menu_push_alert";
    String META_RIDER_DELAY_ALERT_TEMPLATE = "rider_not_moving";
    String META_ORDER_DELAY_ALERT_TEMPLATE = "order_delay_customer_alert";
    String META_REFUND_TEMPLATE = "order_refund_alert";
    String REFUND_REASON = "Token of Apology";

    String ONE_SIGNAL_ORDER_PLACED_TEMPLATE = "9d435490-203b-4062-9e4b-faea4fb786e5";

    // Rider availability monitor — format with restaurantId
    String REDIS_RIDER_SEARCH_COUNT = "restaurant:%s:searching_rider:count";
    String REDIS_RIDER_SEARCH_STATE = "restaurant:%s:searching_rider:state";
    // Broadcast auto-expiry — format with broadcastId
    String REDIS_BROADCAST_EXPIRY = "broadcast:expiry:%s";

    String REDIS_KEY_PIDGE_TOKEN = "pidgeToken";
    String REDIS_KEY_FULFILL = "fulfill";
    String REDIS_KEY_TRIGGER_FULFILL_ON_RIDER_DELAY = "triggerFulfillOnRiderDelay";
    String REDIS_KEY_RIDER_LOCATION_PICKUP_STAGE = "riderLocationPickupStage";
    String REDIS_KEY_RIDER_LOCATION_OFD_STAGE = "riderLocationOfdStage";
    String REDIS_KEY_DELAY_ALERT_TIME = "delayAlertTime";
    String REDIS_KEY_DELIVERY_DELAY = "deliveryDelay";
    String REDIS_KEY_RIDER_LOCATION_DELAY = "riderDelay";

    String SUPPORTED_DATE_FORMATS = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    String KEY_FULFILL = "fulfill";
    String KEY_SMART = "smart";
    String PAYMENT_WORKFLOW_ENABLED = "payment_workflow_enabled";
    String FULFILLMENT_WORKFLOW_ENABLED = "fulfillment_workflow_enabled";
    String STOCK_WORKFLOW_ENABLED = "stock_workflow_enabled";
    Double PLATFORM_DELIVERY_SHARE = 50.0;

    Set<OrderStatusType> CANCELABLE_STATUSES = EnumSet.of(
            OrderStatusType.ACCEPTED,
            OrderStatusType.READY_FOR_DELIVERY,
            OrderStatusType.PAID,
            OrderStatusType.SEARCHING_RIDER,
            OrderStatusType.OUT_FOR_PICKUP,
            OrderStatusType.REACHED_PICKUP,
            OrderStatusType.PICKED_UP,
            OrderStatusType.OUT_FOR_DELIVERY,
            OrderStatusType.REACHED_DELIVERY,
            OrderStatusType.DELIVERED);

    DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
}
