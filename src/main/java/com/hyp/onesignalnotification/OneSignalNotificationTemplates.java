package com.hyp.onesignalnotification;

import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.OrderStatusType;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class OneSignalNotificationTemplates {

    public String buildNewOrderNotificationMessage(
            String customerName,
            String customerMobile,
            String orderId,
            OrderStatusType orderStatus,
            String restaurantName) {
        return String.format(
                """
				📢  New Order Placed

					Hi Team,

					A new order has been placed by the customer.
				    Please review the details below:

					👤 Customer Name: %s
					📱 Customer Mobile Number: %s
					🆔 Order ID: %s
					📊 Order Status: %s
					🍽️ Restaurant Name: %s

					Please take the necessary steps to ensure smooth processing.
					""",
                customerName, customerMobile, orderId, orderStatus, restaurantName);
    }

    public String buildErrorResponseAlertMessage(String orderId, String errorMessage, String platform) {
        return String.format(
                """
				💡 Hi Team,

				🚨 Error Response Alert 🚨
				🔴 Order ID: %s
				🔴 Message: %s
				🔴 Platform: %s

				⚠️ Action Required: Please address this issue promptly
				""",
                orderId, errorMessage, platform);
    }

    public String buildDeliveryDelayNotificationMessage(
            String orderId,
            String restaurantName,
            OrderStatusType status,
            String customerName,
            String mobile,
            String string,
            String string2) {
        return String.format(
                """
						📢 Delivery Delay - Action Required 📢

						We’ve encountered an delay with the delivery of the following order. Kindly review the details below and take immediate action to resolve the matter:

						Order ID: %s
						Restaurant Name: %s
						Order Status: %s
						Customer Name: %s
						Customer Mobile: %s
						Delivery Person Name: %s
						Delivery Person Mobile: %s

						Please address this issue as soon as possible to avoid any further inconvenience to the customer. Your prompt attention to resolving this matter is highly appreciated. Thank you for your cooperation!
						""",
                orderId, restaurantName, status, customerName, mobile, "-", "-");
    }

    public String buildMenuPushNotificationMessage(String restaurantname, String restaurantid, String menusharingcode) {
        return String.format(
                """
				📢 Menu Push Notification 📢

				The menu for the restaurant %s (ID: %s) has been successfully pushed. 🎉

				Restaurant Details:
				🍽️ Name: %s
				🏷️ ID: %s
				🔗 Menu Sharing Code: %s
				""",
                restaurantname, restaurantid, restaurantname, restaurantid, menusharingcode);
    }

    public String buildRiderNotMovingAlertMessage(
            String orderId,
            String restaurantName,
            DeliveryFulfillStatusType orderStatus,
            String customerName,
            String customerMobile,
            String riderName,
            String riderMobile) {
        return String.format(
                """
						📢 Rider Not Moving - Action Required 📢

						We’ve noticed that the assigned delivery person has not moved for a while. Kindly review the details below and take immediate action:

						🔹 Order ID: %s
						🔹 Restaurant Name: %s
						🔹 Order Status: %s
						🔹 Customer Name: %s
						🔹 Customer Mobile: %s
						🔹 Delivery Person Name: %s
						🔹 Delivery Person Mobile: %s

						Please check with the rider and ensure timely delivery to avoid any inconvenience to the customer. Your prompt response is appreciated.

						Thank you for your cooperation!
						""",
                orderId, restaurantName, orderStatus, customerName, customerMobile, riderName, riderMobile);
    }

    public String buildOrderDelayApologyMessage(
            String customerName,
            String orderId,
            String restaurantName1,
            String supportContact,
            String restaurantName2) {
        return String.format(
                """
				Hi %s,

				We’re really sorry! Your order # %s from %s is taking a bit longer than expected.

				Our team is on it, and we’ll get it to you as soon as possible. 🍽️
				If you have any questions or need help, call or WhatsApp us at:
				📞 %s

				Thanks for your patience and for ordering with %s. We truly appreciate it! 🙏
				""",
                customerName, orderId, restaurantName1, supportContact, restaurantName2);
    }

    public String buildOrderTrackAlertMessage(
            String orderId,
            String restaurantName,
            String currentStatus,
            long minutesSinceLastStatus,
            String expectedStatus) {
        return String.format(
                """
						🚨 Order Alert

						🆔 Order ID: %s
						🏪 Restaurant: %s
						📌 Current Status: %s
						⏱ Time Since Last Status: %d mins
						🎯 Expected Status: %s

						⚡ Action Required: Order is stuck at '%s'. Please take necessary action to move it towards '%s' immediately.
						""",
                orderId,
                restaurantName,
                currentStatus,
                minutesSinceLastStatus,
                expectedStatus,
                currentStatus,
                expectedStatus);
    }

    public String buildFraudRiderAlertMessage(String riderContact, String riderName) {
        String name = (riderName == null || riderName.isBlank()) ? "Unknown" : riderName;
        String contact = (riderContact == null || riderContact.isBlank()) ? "Unknown" : riderContact;
        String detectedAt = Instant.now().toString();

        return String.format(
                """
				🚨 Rider Alert

				👤 Rider: %s
				📱 Contact: %s
				🕒 Detected At: %s

				⚠️ This rider matched a flagged/flaggable record.

				🔎 Suggested actions:
				• Verify rider identity and recent allocations.
				• Pause new allocations for this rider until cleared.
				• Escalate to Ops/Security for investigation.

				⚡ Action Required: Review the rider immediately and take appropriate action.
				""",
                name, contact, detectedAt);
    }
}
