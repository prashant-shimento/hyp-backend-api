package com.hyp.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.hyp.dto.OrderDto;
import com.hyp.dto.OrderDto.OrderAddonItem;
import com.hyp.dto.OrderDto.OrderItem;
import com.hyp.dto.OrderDto.OrderTax;
import com.hyp.entity.AddonItem;
import com.hyp.entity.Discount;
import com.hyp.entity.Item;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.entity.Tax;
import com.hyp.enums.OrderStatusType;
import com.hyp.mapper.DataMapper;
import com.hyp.repository.OrderRepository;
import com.hyp.request.PosOrderRequest;
import com.hyp.response.ResponseTemplate;
import com.hyp.translation.PosOrderRequestTranslation;
import com.hyp.util.Utils;

@Service
public class OrderService extends BaseServiceImpl<Order, String> {
	@Autowired
	OrderRepository orderRepository;

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	TaxService taxService;

	@Autowired
	DiscountService discountService;

	@Autowired
	ItemService itemService;

	@Autowired
	AddonItemService addonItemService;

	@Autowired
	DataMapper dataMapper;

	@Autowired
	private PosOrderService posOrderService;

	public Order create(OrderDto orderDto) throws Exception {
		if (!restaurantService.isExistsById(orderDto.getRestaurantId())) {
			throw new Exception("Restaurant not found " + orderDto.getRestaurantId());
		}
		if (orderDto.getOrderDiscount() != null) {
			if (!discountService.isExistsById(orderDto.getOrderDiscount().getDiscountId())) {
				throw new Exception("Discount not found " + orderDto.getOrderDiscount().getDiscountId());
			}
		}
		if (orderDto.getOrderTax() != null) {
			for (OrderTax ordertax : orderDto.getOrderTax()) {
				if (!taxService.isExistsById(ordertax.getTaxId())) {
					throw new Exception("Tax not found " + ordertax.getTaxId());
				}
			}
		}
		for (OrderItem orderItem : orderDto.getOrderItems()) {
			if (!itemService.isExistsById(orderItem.getItemId())) {
				throw new Exception("Item not found " + orderItem.getItemId());
			}
			if(orderItem.getOrderAddonItems() != null) {
				for (OrderAddonItem orderAddonItem : orderItem.getOrderAddonItems()) {
					if (addonItemService.isExistsById(orderAddonItem.getAddonItemId())) {
						throw new Exception("AddonItem not found " + orderAddonItem.getAddonItemId());
					}
				}
			}
		}

		Order order = dataMapper.toOrderEntity(orderDto);
		order.setId(Utils.genId());
		order.setStatus(OrderStatusType.CREATED);
		return this.save(order);
	}
}
