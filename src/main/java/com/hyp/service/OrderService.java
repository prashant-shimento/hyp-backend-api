package com.hyp.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
import com.hyp.exception.PosException;
import com.hyp.mapper.DataMapper;
import com.hyp.repository.OrderRepository;
import com.hyp.request.DeliveryOrderRequest;
import com.hyp.request.PosCallbackRequest;
import com.hyp.request.PosOrderRequest;
import com.hyp.translation.DeliveryRequestTranslation;
import com.hyp.translation.PosOrderRequestTranslation;

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
	DataMapper dataMapper;

	@Autowired
	SequenceService sequenceService;

	@Autowired
	AddressService addressService;

	@Autowired
	PosService posService;

	@Autowired
	DeliveryService deliveryService;

	public Order create(OrderDto orderDto) throws Exception {
		if (!restaurantService.isExistsById(orderDto.getRestaurantId())) {
			throw new Exception("Restaurant not found " + orderDto.getRestaurantId());
		}
		if (!customerService.isExistsById(orderDto.getCustomerId())) {
			throw new Exception("Restaurant not found " + orderDto.getCustomerId());
		}

		if (!addressService.isExistsById(orderDto.getDeliveryAddress())) {
			throw new Exception("Delivery Address not found " + orderDto.getDeliveryAddress());
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

		Order order = dataMapper.toOrderEntity(orderDto);
		order.setId(sequenceService.generateSequence(Order.SEQUENCE_NAME));
		order.setStatus(OrderStatusType.CREATED);
		order.setCreatedAt(LocalDateTime.now());
		return this.save(order);
	}

	public Order processCallback(PosCallbackRequest posCallbackRequest) throws Exception {

		Restaurant restaurant = restaurantService.findByMenuSharingCode(posCallbackRequest.getRestaurantId());
		if (restaurant == null) {
			throw new Exception("Restaurant not found " + posCallbackRequest.getRestaurantId());
		}
		if (!this.isExistsById(posCallbackRequest.getOrderId())) {
			throw new Exception("Restaurant not found " + posCallbackRequest.getOrderId());
		}

		Order order = this.findById(posCallbackRequest.getOrderId());
		order.setStatus(OrderStatusType.getOrderStatusByPosStatus(posCallbackRequest.getStatus()));
		order.setMinDeliveryTime(posCallbackRequest.getMinDeliveryTime());
		order.setMinPrepTime(posCallbackRequest.getMinPrepTime());
		return this.update(order);
	}

	public void processPosOrder(Order order) {
		try {
			Address address = addressService.findById(order.getDeliveryAddress());
			Customer customer = customerService.findById(order.getCustomerId());
			customer.setAddress(address);
			Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
			PosOrderRequest posOrderRequest = PosOrderRequestTranslation
					.getPosOrderRequest(restaurantService.findById(order.getRestaurantId()), order, customer);

			posService.createOrder(posOrderRequest);

			DeliveryOrderRequest deliveryOrderRequest = DeliveryRequestTranslation.getDeliveryOrderRequest(restaurant,
					address, customer, order);

			String deliveryOrderId = deliveryService.createDeliveryOrder(deliveryOrderRequest);

			Delivery delivery = DeliveryRequestTranslation.getDeliveryEntity(deliveryOrderRequest);
			delivery.setDeliveryOrderId(deliveryOrderId);
			delivery.setStatus(DeliveryOrderStatusType.PENDING);
			deliveryService.save(delivery);

		} catch (PosException e) {
			this.updateOrderStatus(order.getId(), OrderStatusType.ERROR);
			// Need to handle Payment Refund or Retry Mechanism
			throw new RuntimeException("Exception Occured while createOrder in POS Service " + e.getMessage());
		} catch (Exception e) {
			throw new RuntimeException("Exception Occured while Processing Order " + e.getMessage());
		}

	}

	public void updateOrderStatus(String orderId, OrderStatusType orderStatus) {
		Order order = this.findById(orderId);
		order.setStatus(orderStatus);
		this.save(order);
	}
}
