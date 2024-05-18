package com.hyp.translation;

import org.springframework.stereotype.Service;

import com.hyp.dto.OrderDto;
import com.hyp.entity.Order;
import com.hyp.service.BaseTranslationServiceImpl;

@Service
public class OrderTranslation extends BaseTranslationServiceImpl<OrderDto, Order> {

	@Override
	protected Class<OrderDto> getDtoClass() {
		return OrderDto.class;
	}

	@Override
	protected Class<Order> getEntityClass() {
		return Order.class;
	}

}
