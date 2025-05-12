package com.hyp.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hyp.enums.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.hyp.constants.Constants;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Partner;
import com.hyp.entity.Restaurant;
import com.hyp.entity.User;
import com.hyp.enums.PartnerType;
import com.hyp.enums.PosPartner;
import com.hyp.event.OrderEvent;
import com.hyp.event.OrderEventPublisher;
import com.hyp.event.OrderStatusChangeEvent;
import com.hyp.exception.OneSignalException;
import com.hyp.request.OneSignalNotificationAlias;
import com.hyp.request.OneSignalNotificationRequest;
import com.hyp.service.CustomerService;
import com.hyp.service.DeliveryService;
import com.hyp.service.NotificationService;
import com.hyp.service.PartnerService;
import com.hyp.service.RestaurantService;
import com.hyp.service.UserService;
import com.hyp.util.CommonUtils;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OrderEventListener {

	@Autowired
	SimpMessagingTemplate messageTemplate;

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	CustomerService customerService;

	@Autowired
	DeliveryService deliveryService;

	@Autowired
	NotificationService notificationService;

	@Autowired
	private OrderEventPublisher orderEventPublisher;

	@Autowired
	private PartnerService partnerService;
	
	@Autowired
	UserService userService;
	
	@Value("${onesignal.app.id}")
	private String appId;

	@Async
	@EventListener
	public void handleProcessOrder(OrderEvent event) {
		log.info("Order Event listener handleProcessOrder");
		Order order = event.getOrder();
		if(restaurantService.findById(order.getRestaurantId()).getPosPartner().equalsIgnoreCase(PosPartner.PET_POOJA.name())) {
			orderEventPublisher.publishPosOrderEvent(order);			
		}
		log.info("Order Type {}", order.getOrderType());
		if (OrderType.fromCode(order.getOrderType()) == OrderType.H) {
			orderEventPublisher.publishDeliveryOrderEvent(order);
		}

	}

	@Async
	@EventListener
	public void handleOrderStatusChange(OrderStatusChangeEvent event) {
		log.info("Order Event listener handleOrderStatusChange");
		List<String> templateParameters = null;
		Order order = event.getOrder();

		messageTemplate.convertAndSend("/topic/order-status", order);

		Customer customer = customerService.findById(order.getCustomerId());
		Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
		Delivery delivery = deliveryService.findByOrderId(order.getId());
		Partner partner = partnerService.findPartnersByRestaurantId(restaurant.getId(), PartnerType.THEATRE);
		switch (order.getStatus()) {
		case CREATED:
			if (partner != null) {
				List<String> parameters = CommonUtils.buildStringList(customer.getName(),
						restaurant.getRestaurantName(), order.getId());
				notificationService.sendNotification(customer.getMobile(),
						Constants.META_ORDER_CREATED_THEATRE_TEMPLATE, parameters);
			}
			break;
		case ACCEPTED:
			if (partner != null) {
				List<String> parameters = CommonUtils.buildStringList(customer.getName(),
						restaurant.getRestaurantName(), order.getId(), order.getScreen(), order.getSeat());
				notificationService.sendNotification(customer.getMobile(),
						Constants.META_ORDER_CONFIRMED_THEATRE_TEMPLATE, parameters);
			} else {
				List<String> parameters = CommonUtils.buildStringList(customer.getName(),
						restaurant.getRestaurantName(), restaurant.getCity(), order.getId(), restaurant.getContact(),
						restaurant.getSupportContact());
				notificationService.sendNotification(customer.getMobile(), Constants.META_ORDER_CONFIRMED_TEMPLATE,
						parameters);
				//deliveryService.setFullfillExpiry(order.getId()); #Commenting out to trigger delivery immediately
			}
			break;
		case PAID:
			if(restaurant.getPosPartner().equalsIgnoreCase(PosPartner.SELF.name())) {
				User user = userService.findByRestaurantId(order.getRestaurantId());
				Map<String, Object> customDataMap = new HashMap<>();
				customDataMap.put("userName", user.getName());
				customDataMap.put("orderId", order.getId());
				customDataMap.put("customerName", customer.getName());
				OneSignalNotificationRequest request = OneSignalNotificationRequest.builder()
				.targetChannel("push")
				.includeAliases(OneSignalNotificationAlias.builder()
						.externalId(List.of(user.getId())).build())
				.appId(appId)
				.templateId(Constants.ONE_SIGNAL_ORDER_PLACED_TEMPLATE)
				.customData(customDataMap)
				.build();			
				try {
					notificationService.sendOneSignalNotification(request);
				} catch (OneSignalException e) {
					log.error("Error occurred in sending push notification " + request.toString());
				}
			}
			templateParameters = CommonUtils.buildStringList(customer.getName(), restaurant.getRestaurantName(),
					order.getId(), order.getStatus(), restaurant.getSupportContact(), restaurant.getContact());
			notificationService.sendNotification(customer.getMobile(), Constants.META_ORDER_PAID_TEMPLATE,
					templateParameters);
			break;

		case PICKED_UP:
			templateParameters = CommonUtils.buildStringList(customer.getName(), order.getId(),
					delivery.getFulfillment().getRider().getName(), delivery.getFulfillment().getRider().getMobile(),
					restaurant.getContact(), restaurant.getSupportContact());
			notificationService.sendNotification(customer.getMobile(), Constants.META_ORDER_PICKEDUP_TEMPLATE,
					templateParameters, delivery.getFulfillment().getTrackCode());
			break;
		case DELIVERED:
			if (partner != null) {
				templateParameters = CommonUtils.buildStringList(customer.getName(), order.getId(),
						restaurant.getSupportContact());
				notificationService.sendNotification(customer.getMobile(),
						Constants.META_ORDER_DELIVERED_THEATRE_TEMPLATE, templateParameters);
			} else {
				templateParameters = CommonUtils.buildStringList(customer.getName(), order.getId(),
						restaurant.getContact(), restaurant.getSupportContact(), restaurant.getRestaurantName(),
						restaurant.getWebsiteUrl());
				notificationService.sendNotification(customer.getMobile(), Constants.META_ORDER_DELIVERED_TEMPLATE,
						templateParameters);
			}
			break;
		case CANCELLED:
			templateParameters = CommonUtils.buildStringList(customer.getName(), order.getId(),
					restaurant.getRestaurantName());
			notificationService.sendNotification(customer.getMobile(), Constants.META_ORDER_CANCELLED_TEMPLATE,
					templateParameters);
			break;
		default:
			break;
		}

	}

}
