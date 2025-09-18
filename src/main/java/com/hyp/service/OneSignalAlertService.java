package com.hyp.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.enums.OrderStatusType;
import com.hyp.exception.OneSignalException;
import com.hyp.onesignalnotification.OneSignalNotificationTemplates;
import com.hyp.request.OneSignalNotificationRequest;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OneSignalAlertService {

	@Value("${onesignal.app.id}")
	private String appId;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private SimpMessagingTemplate messageTemplate;

	@Autowired
	private OneSignalNotificationTemplates oneSignalNotificationTemplates;

	public void notifyErrorResponseAlert(String orderId, String errorMessage, String platform) {
		String messageBody = oneSignalNotificationTemplates.buildErrorResponseAlertMessage(orderId, errorMessage,
				platform);

		OneSignalNotificationRequest request = OneSignalNotificationRequest.builder().targetChannel("push").appId(appId)
				.includedSegments(List.of("All")).contents(Map.of("en", messageBody)).build();

		log.info("Sending Error Response Alert [orderId={}]", orderId);
		try {
			notificationService.sendOneSignalNotification(request);
			log.info("Error Response Alert sent successfully for orderId={}", orderId);
		} catch (OneSignalException ex) {
			log.error("Error sending Error Response Alert for orderId={} — payload={}", orderId, request, ex);
		}
	}

	public void notifyDeliveryDelay(String orderId, String restaurantName, OrderStatusType status, String customerName,
			String mobile) {
		String messageBody = oneSignalNotificationTemplates.buildDeliveryDelayNotificationMessage(orderId,
				restaurantName, status, customerName, mobile, "-", "-");

		OneSignalNotificationRequest request = OneSignalNotificationRequest.builder().targetChannel("push").appId(appId)
				.includedSegments(List.of("All")).contents(Map.of("en", messageBody)).build();

		log.info("Sending Delivery Delay notification [orderId={}]", orderId);
		try {
			notificationService.sendOneSignalNotification(request);
			log.info("Delivery Delay notification sent successfully for orderId={}", orderId);
		} catch (OneSignalException e) {
			log.error("Error sending Delivery Delay notification for orderId={} — payload={}", orderId, request, e);
		}
	}

	public void notifyRiderNotMovingAlert(String orderId, String restaurantName, DeliveryFulfillStatusType orderStatus,
			String customerName, String customerMobile, String riderName, String riderMobile) {

		String messageBody = oneSignalNotificationTemplates.buildRiderNotMovingAlertMessage(orderId, restaurantName,
				orderStatus, customerName, customerMobile, riderName, riderMobile);

		OneSignalNotificationRequest request = OneSignalNotificationRequest.builder().targetChannel("push").appId(appId)
				.includedSegments(List.of("All")).contents(Map.of("en", messageBody)).build();

		log.info("Sending Rider Not Moving Alert [orderId={}]", orderId);
		try {
			notificationService.sendOneSignalNotification(request);
			log.info("Rider Not Moving Alert sent successfully for orderId={}", orderId);
		} catch (OneSignalException ex) {
			log.error("Error sending Rider Not Moving Alert for orderId={} — payload={}", orderId, request, ex);
		}

	}

	public void notifyNewOrder(String customerName, String customerMobile, String orderId, OrderStatusType orderStatus,
			String restaurantName) {
		String messageBody = oneSignalNotificationTemplates.buildNewOrderNotificationMessage(customerName,
				customerMobile, orderId, orderStatus, restaurantName);

		OneSignalNotificationRequest request = OneSignalNotificationRequest.builder().targetChannel("push").appId(appId)
				.includedSegments(List.of("All")).contents(Map.of("en", messageBody)).build();

		log.info("Sending OneSignal static notification [appId={}, segments={}]", request.getAppId(),
				request.getIncludedSegments());

		try {
			notificationService.sendOneSignalNotification(request);
			log.info("OneSignal static notification sent successfully for orderId={}", orderId);
		} catch (OneSignalException e) {
			log.error("Error sending OneSignal static notification for orderId={} — payload={}", orderId, request, e);
		}
	}

	public void notifyMenuPush(String restaurantname, String restaurantid, String menusharingcode) {
		String messageBody = oneSignalNotificationTemplates.buildMenuPushNotificationMessage(restaurantname,
				restaurantid, menusharingcode);

		OneSignalNotificationRequest request = OneSignalNotificationRequest.builder().targetChannel("push").appId(appId)
				.includedSegments(List.of("All")).contents(Map.of("en", messageBody)).build();
		log.info("Sending Menu Push notification [restaurantId={}]", restaurantid);
		try {
			notificationService.sendOneSignalNotification(request);

			log.info("Menu Push notification sent successfully for restaurantId={}", restaurantid);
		} catch (OneSignalException e) {
			log.error("Error sending Menu Push notification for restaurantId={} — payload={}", restaurantid, request,
					e);
		}
	}

	public void notifyOrderTrackDelay(String orderId, String restaurantName, String currentStatus,
			long finalMinutesSinceCreation, String nextExpected) {
		String messageBody = oneSignalNotificationTemplates.buildOrderTrackAlertMessage(orderId, restaurantName,
				currentStatus, finalMinutesSinceCreation, nextExpected);
		messageTemplate.convertAndSend("/topic/order-track", messageBody);
		OneSignalNotificationRequest request = OneSignalNotificationRequest.builder().targetChannel("push").appId(appId)
				.includedSegments(List.of("All")).contents(Map.of("en", messageBody)).build();

		log.info("Sending Order Track Delay notification [orderId={}]", orderId);
		try {
			notificationService.sendOneSignalNotification(request);
			log.info("Order Track Delay notification sent successfully for orderId={}", orderId);
		} catch (OneSignalException e) {
			log.error("Error sending Order Track Delay notification for orderId={} — payload={}", orderId, request, e);
		}
	}

}
