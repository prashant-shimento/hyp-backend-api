package com.hyp.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
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
import com.hyp.exception.DeliveryException;
import com.hyp.exception.PosException;
import com.hyp.exception.RequestTranslationException;
import com.hyp.repository.OrderRepository;
import com.hyp.request.DeliveryOrderRequest;
import com.hyp.request.PosCallbackRequest;
import com.hyp.request.PosOrderRequest;
import com.hyp.translation.DeliveryRequestTranslation;
import com.hyp.translation.OrderTranslation;
import com.hyp.translation.PosOrderRequestTranslation;
import com.hyp.util.CommonUtils;
import com.hyp.util.ValidationUtils;

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
	PosService posService;

	@Autowired
	DeliveryService deliveryService;

	@Autowired
	PaymentService paymentService;

	@Autowired
	PosOrderRequestTranslation posOrderRequestTranslation;

	@Autowired
	SimpMessagingTemplate messageTemplate;

	@Autowired
	LocationService locationService;

	public Order create(OrderDto orderDto) throws Exception {
		if (!restaurantService.isExistsById(orderDto.getRestaurantId())) {
			throw new Exception("Restaurant not found " + orderDto.getRestaurantId());
		}
		if (!customerService.isExistsById(orderDto.getCustomerId())) {
			throw new Exception("Customer not found " + orderDto.getCustomerId());
		}

		if (!addressService.isExistsById(orderDto.getDeliveryDetails().getAddressId())) {
			throw new Exception("Delivery Address not found " + orderDto.getDeliveryDetails().getAddressId());
		}
		
		Restaurant restaurant = restaurantService.findById(orderDto.getRestaurantId());
		if (!ValidationUtils.isWithinDeliveryHours(restaurant.getDeliveryHours())) {
			throw new Exception("Order cannot be processed: Outside delivery hours.");
		}

		Address address = addressService.findById(orderDto.getDeliveryDetails().getAddressId());
		if (!locationService.isLocationDeliverable(address.getLocation().getLatitude(),
				address.getLocation().getLongitude(), restaurant.getLocation().getLatitude(),
				restaurant.getLocation().getLongitude(), restaurant.getDeliveryRadius())) {
			throw new Exception("Location Not Deliverable");
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

			if (orderItem.getVariationId() != null) {
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
		}

		Order order = orderTranslation.getEntity(orderDto);
		order.setStatus(OrderStatusType.CREATED);
		order.setOrderTime(LocalDateTime.now());
		order.setCreatedAt(LocalDateTime.now());
		return this.save(order);
	}

	public Order processCallback(PosCallbackRequest posCallbackRequest) throws Exception {

		try {
			Restaurant restaurant = restaurantService.findByMenuSharingCode(posCallbackRequest.getRestaurantId());
			if (restaurant == null) {
				throw new Exception("Restaurant not found " + posCallbackRequest.getRestaurantId());
			}
			if (!this.isExistsById(posCallbackRequest.getOrderId())) {
				throw new Exception("Order not found " + posCallbackRequest.getOrderId());
			}
			Order order = this.findById(posCallbackRequest.getOrderId());
			OrderStatusType newOrderStatus = OrderStatusType.getOrderStatusByPosStatus(posCallbackRequest.getStatus());

			if (newOrderStatus == OrderStatusType.READY_FOR_DELIVERY) {
				Delivery delivery = deliveryService.findByOrderId(order.getId());
				if (delivery != null && delivery.getStatus().equals(DeliveryOrderStatusType.PENDING)) {
					deliveryService.processDeliveryFulfill(delivery, Constants.PET_POOJA);
				}
			}

			if (newOrderStatus == OrderStatusType.CANCELLED) {
				paymentService.createRefund(order.getId(), order.getTotalAmount(), true);
				Delivery delivery = deliveryService.findByOrderId(order.getId());
				if (delivery != null && delivery.getStatus().equals(DeliveryOrderStatusType.PENDING)) {
					deliveryService.cancelDeliveryOrder(delivery.getDeliveryOrderId());
				}
			}

			order.setStatus(newOrderStatus);
			order.setMinDeliveryTime(posCallbackRequest.getMinDeliveryTime());
			order.setMinPrepTime(posCallbackRequest.getMinPrepTime());
			order = this.update(order);
			messageTemplate.convertAndSend("/topic/order-status", order);
			return order;

		} catch (DeliveryException e) {
			this.updateOrderStatus(posCallbackRequest.getOrderId(), OrderStatusType.DELIVERY_ERROR);
			// Add Alert Mechanism
			throw new RuntimeException("Exception Occured while createOrder in Delivery Service " + e.getMessage());
		} catch (Exception e) {
			this.updateOrderStatus(posCallbackRequest.getOrderId(), OrderStatusType.ERROR);
			throw new RuntimeException("Exception Occured while processCallback Order " + e.getMessage());
		}

	}

	public void processOrder(Order order) {
		try {
			Address address = addressService.findById(order.getDeliveryDetails().getAddressId());
			Customer customer = customerService.findById(order.getCustomerId());
			Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
			PosOrderRequest posOrderRequest = posOrderRequestTranslation
					.getPosOrderRequest(restaurantService.findById(order.getRestaurantId()), order, customer, address);

			if (posService.createPosOrder(posOrderRequest)) {
				DeliveryOrderRequest deliveryOrderRequest = DeliveryRequestTranslation
						.getDeliveryOrderRequest(restaurant, address, customer, order);
				String deliveryOrderId = deliveryService.createDeliveryOrder(deliveryOrderRequest);
				Delivery delivery = DeliveryRequestTranslation.getDeliveryEntity(deliveryOrderRequest);
				delivery.setId(CommonUtils.genId());
				delivery.setDeliveryOrderId(deliveryOrderId);
				delivery.setStatus(DeliveryOrderStatusType.PENDING);
				delivery.setService(order.getDeliveryDetails().getService());
				delivery.setNetworkId(order.getDeliveryDetails().getNetworkId());
				delivery.setPickupNow(order.getDeliveryDetails().isPickupNow());
				deliveryService.save(delivery);
			}
		} catch (RequestTranslationException e) {
			// Need to handle Payment Refund or Retry Mechanism
			throw new RuntimeException("Exception Occured while requestTranslation " + e.getMessage());
		} catch (PosException e) {
			this.updateOrderStatus(order.getId(), OrderStatusType.POS_ERROR);
			// Need to handle Payment Refund or Retry Mechanism
			throw new RuntimeException("Exception Occured while createOrder in POS Service " + e.getMessage());
		} catch (DeliveryException e) {
			this.updateOrderStatus(order.getId(), OrderStatusType.DELIVERY_ERROR);
			throw new RuntimeException("Exception Occured while createOrder in Delivery Service " + e.getMessage());
		} catch (Exception e) {
			throw new RuntimeException("Exception Occured while Processing Order " + e.getMessage());
		}

	}

	public void updateOrderStatus(String orderId, OrderStatusType orderStatus) {
		Order order = this.findById(orderId);
		order.setStatus(orderStatus);
		this.save(order);
		messageTemplate.convertAndSend("/topic/order-status", order);
	}

	@Scheduled(fixedRate = 60000)
	public void scheduleDeliveryFullfill() throws DeliveryException {
		LocalDateTime currentTime = LocalDateTime.now();
		List<Order> ordersToProcess = orderRepository.findByStatus(OrderStatusType.ACCEPTED).stream().filter(order -> {
			int minPrepTime = order.getMinPrepTime().equalsIgnoreCase("") ? 10
					: Integer.parseInt(order.getMinPrepTime());
			int bufferTime = minPrepTime > 20 ? minPrepTime - 10 : minPrepTime - 5;
			LocalDateTime triggerTime = order.getOrderTime().plusMinutes(bufferTime);
			return triggerTime.isBefore(currentTime) || triggerTime.isEqual(currentTime);
		}).collect(Collectors.toList());

		for (Order order : ordersToProcess) {
			Delivery delivery = deliveryService.findByOrderId(order.getId());
			if (delivery != null && delivery.getStatus().equals(DeliveryOrderStatusType.PENDING)) {
				deliveryService.processDeliveryFulfill(delivery, Constants.SYSTEM);
			}
		}

	}
}
