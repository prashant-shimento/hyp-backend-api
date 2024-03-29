package com.hyp.service;

import java.time.LocalDateTime;

import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyp.dto.BaseDto;
import com.hyp.dto.OrderDto;
import com.hyp.dto.OrderDto.OrderAddonItem;
import com.hyp.dto.OrderDto.OrderItem;
import com.hyp.dto.OrderDto.OrderTax;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.OrderStatusType;
import com.hyp.enums.OrderType;
import com.hyp.mapper.DataMapper;
import com.hyp.repository.OrderRepository;
import com.hyp.request.PosCallbackRequest;
import com.hyp.request.PosOrderRequest;
import com.hyp.translation.PosOrderRequestTranslation;
import com.hyp.util.CommonUtils;

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

	public Order create(OrderDto orderDto) throws Exception {
		if (!restaurantService.isExistsById(orderDto.getRestaurantId())) {
			throw new Exception("Restaurant not found " + orderDto.getRestaurantId());
		}
		if (!customerService.isExistsById(orderDto.getCustomerId())) {
			throw new Exception("Restaurant not found " + orderDto.getCustomerId());
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
		// order.setMinDeliveryTime(posCallbackRequest.getMinDeliveryTime());
		// order.setMinPrepTime(posCallbackRequest.getMinPrepTime());
		return this.update(order);
	}
}
