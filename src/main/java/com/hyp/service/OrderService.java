package com.hyp.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.hyp.enums.OrderType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.constants.Constants;
import com.hyp.dto.OrderDto;
import com.hyp.dto.OrderDto.OrderAddonItem;
import com.hyp.dto.OrderDto.OrderItem;
import com.hyp.dto.OrderDto.OrderTax;
import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.DeliveryOrderStatusType;
import com.hyp.enums.OrderStatusType;
import com.hyp.enums.PaymentType;
import com.hyp.event.OrderEventPublisher;
import com.hyp.exception.DeliveryException;
import com.hyp.exception.EntityNotFoundException;
import com.hyp.repository.OrderRepository;
import com.hyp.request.PosCallbackRequest;
import com.hyp.translation.OrderTranslation;
import com.hyp.translation.PosOrderRequestTranslation;
import com.hyp.util.CommonUtils;
import com.hyp.util.ValidationUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrderService extends BaseServiceImpl<Order, String> {

	@Autowired
	OrderRepository orderRepository;

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	CustomerService customerService;

	@Autowired
	TaxService taxService;

	@Autowired
	DiscountService discountService;

	@Autowired
	ItemService itemService;

	@Autowired
	AddonItemService addonItemService;

	@Autowired
	VariationService variationService;

	@Autowired
	OrderTranslation orderTranslation;

	@Autowired
	AddressService addressService;

	@Autowired
	DeliveryService deliveryService;

	@Autowired
	PaymentService paymentService;

	@Autowired
	PosOrderRequestTranslation posOrderRequestTranslation;

	@Autowired
	LocationService locationService;

	@Autowired
	OrderTypeService orderTypeService;

	@Autowired
	NotificationService notificationService;

	@Autowired
	RedisService redisService;

	@Autowired
	private OrderEventPublisher orderEventPublisher;

	public Order create(OrderDto orderDto) throws Exception {

		Restaurant restaurant = Optional.ofNullable(restaurantService.findById(orderDto.getRestaurantId())).orElseThrow(
				() -> new EntityNotFoundException(Restaurant.class.getSimpleName(), orderDto.getRestaurantId()));

		Customer customer = Optional.ofNullable(customerService.findById(orderDto.getCustomerId())).orElseThrow(
				() -> new EntityNotFoundException(Customer.class.getSimpleName(), orderDto.getCustomerId()));

		Address address = null;

		//TODO: Need to validate orderType from DB once front end accommodate the changes
		if(OrderType.fromCode(orderDto.getOrderType()) == OrderType.H) {
			if (orderDto.getDeliveryDetails() != null) {
				address = Optional.ofNullable(addressService.findById(orderDto.getDeliveryDetails().getAddressId()))
						.orElseThrow(() -> new EntityNotFoundException(Address.class.getSimpleName(),
								orderDto.getDeliveryDetails().getAddressId()));

				if (!locationService.isLocationDeliverable(address.getLocation().getLatitude(),
						address.getLocation().getLongitude(), restaurant.getLocation().getLatitude(),
						restaurant.getLocation().getLongitude(), restaurant.getDeliveryRadius())) {
					throw new Exception("Location Not Deliverable");
				}
			} else { //TODO: this has to be moved to orderType Dine and needs front end changes in mocoda
				if (orderDto.getSeat() == null || orderDto.getScreen() == null) {
					throw new Exception("Delivery Details are missing, and both Seat and Screen must be provided.");
				}
			}
		}


		if (!ValidationUtils.isWithinDeliveryHours(restaurant.getDeliveryHours())) {
			throw new Exception("Order cannot be processed: Outside delivery hours.");
		}

		if (orderDto.getOrderDiscount() != null) {
			for (OrderDto.OrderDiscount discount : orderDto.getOrderDiscount()) {
				if (!discountService.isExistsById(discount.getId())) {
					throw new Exception("Discount not found " + discount.getId());
				}
			}
		}

		if (orderDto.getOrderTax() != null) {
			for (OrderTax ordertax : orderDto.getOrderTax()) {
				if (!taxService.isExistsById(ordertax.getId())) {
					throw new Exception("Tax not found " + ordertax.getId());
				}
			}
		}

		for (OrderItem orderItem : orderDto.getOrderItems()) {
			if (orderItem.getVariationId() != null || orderItem.getVariationName() != null) {
				if (!variationService.isExistsById(orderItem.getId())) {
					throw new Exception("Variation not found " + orderItem.getId());
				}
			} else if (!itemService.isExistsById(orderItem.getId())) {
				throw new Exception("Item not found " + orderItem.getId());
			}

			if (orderItem.getOrderAddonItems() != null) {
				for (OrderAddonItem orderAddonItem : orderItem.getOrderAddonItems()) {
					if (!addonItemService.isExistsById(orderAddonItem.getAddonItemId())) {
						throw new Exception("AddonItem not found " + orderAddonItem.getAddonItemId());
					}
				}
			}
			if (orderItem.getVariationId() == null) {
				orderItem.setItemAttribute(itemService.findById(orderItem.getId()).getItemAttributeId());
			}
		}
		Order order = orderTranslation.getEntity(orderDto);
		order.setStatus(OrderStatusType.CREATED);
		order.setOrderTime(LocalDateTime.now());
		order.setCreatedAt(LocalDateTime.now());
		order.getOrderLogs().add(new Order.OrderLog(OrderStatusType.CREATED.name()));
		order.setPlatformFee(paymentService.calculatePlatformFee(orderDto.getGrandTotalAmount(), restaurant));
		order = this.save(order);

		if (order.getPaymentType() == PaymentType.COD) {
			orderEventPublisher.publishPosOrderEvent(order);
			orderEventPublisher.publishOrderStatusChangeEvent(order);
		}

		List<String> parameters = CommonUtils.buildStringList(customer.getName(), customer.getMobile(), order.getId(),
				order.getStatus(), restaurant.getRestaurantName());
		notificationService.sendInternalGroupNotification(Constants.META_ORDER_ALERT_TEMPLATE, parameters);
		return order;

	}

	public void processOrderCallback(PosCallbackRequest posCallbackRequest) {

		try {
			Restaurant restaurant = restaurantService.findByMenuSharingCode(posCallbackRequest.getRestaurantId());
			if (restaurant == null) {
				throw new Exception("Restaurant not found " + posCallbackRequest.getRestaurantId());
			}
			if (!this.isExistsById(posCallbackRequest.getOrderId())) {
				throw new Exception("Order not found " + posCallbackRequest.getOrderId());
			}
			Order order = this.findById(posCallbackRequest.getOrderId());
			PaymentType paymentType = order.getPaymentType();
			if (paymentType != PaymentType.COD) {
				if (paymentService.findByOrderId(order.getId()) == null) {
					throw new Exception("Payment not completed" + posCallbackRequest.getOrderId());
				}
			}

			OrderStatusType newOrderStatus = OrderStatusType.getOrderStatusByPosStatus(posCallbackRequest.getStatus());
			order.setStatus(newOrderStatus);
			if (newOrderStatus == OrderStatusType.ACCEPTED) {
				order.setMinDeliveryTime(posCallbackRequest.getMinDeliveryTime());
				order.setMinPrepTime(posCallbackRequest.getMinPrepTime());
				order = update(order);

				int delayMinutes = Optional.ofNullable(restaurant.getFulfillmentDelay()).orElse(0);

				if (delayMinutes > 0) {
					log.info("Scheduling fulfillment for order {} after {} minutes", order.getId(), delayMinutes);
					deliveryService.setFulfillExpiry(order.getId(), delayMinutes);
					return;
				}
				String fulfillmentMode = redisService.getRedisData("fulfill").orElse("smart");
				Delivery delivery = deliveryService.findByOrderId(order.getId());
				if (delivery == null || !DeliveryOrderStatusType.PENDING.equals(delivery.getStatus())) {
					log.warn("No PENDING delivery found for order {}. Skipping fulfillment.", order.getId());
					return;
				}
				if ("smart".equalsIgnoreCase(fulfillmentMode)) {
					log.info("Processing smart fulfillment for order {}", order.getId());
					deliveryService.processDeliverySmartFulfill(delivery, Constants.PET_POOJA);
				} else {
					log.info("Processing standard fulfillment for order {}", order.getId());
					deliveryService.processDeliveryStandardFulfill(delivery, Constants.PET_POOJA);
				}
				order.setDeliveryTrackingLink("https://api.hyperapps.in/order/track/"+order.getId());
			} else if (newOrderStatus == OrderStatusType.CANCELLED) {
				paymentService.createRefund(order.getId(), order.getGrandTotalAmount(), restaurant.isInstantRefund());
				Delivery delivery = deliveryService.findByOrderId(order.getId());
				if (delivery != null && (delivery.getStatus().equals(DeliveryOrderStatusType.PENDING)
						|| delivery.getStatus().equals(DeliveryOrderStatusType.FULFILLED))) {
					deliveryService.cancelDeliveryOrder(delivery.getDeliveryOrderId());
				}
			}
			updateOrderStatus(order.getId(), newOrderStatus);
		} catch (DeliveryException e) {
			throw new RuntimeException("Exception Occured while createOrder in Delivery Service " + e.getMessage());
		} catch (Exception e) {
			this.updateOrderStatus(posCallbackRequest.getOrderId(), OrderStatusType.ERROR);
			throw new RuntimeException("Exception Occured while processCallback Order " + e.getMessage());
		}
	}

	public void updateOrderStatus(String orderId, OrderStatusType orderStatus) {
		Order order = this.findById(orderId);
		order.setStatus(orderStatus);
		order.getOrderLogs().add(new Order.OrderLog(orderStatus.name()));
		save(order);
		orderEventPublisher.publishOrderStatusChangeEvent(order);
	}
}