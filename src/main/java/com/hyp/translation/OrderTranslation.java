package com.hyp.translation;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hyp.dto.OrderDto;
import com.hyp.entity.Order;
import com.hyp.service.TranslationService;

@Service
public class OrderTranslation implements TranslationService<OrderDto, Order> {

	@Override
	public Order getEntity(OrderDto dto) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderDto getDto(Order entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<OrderDto> getDtoList(List<Order> entities) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Order getPatchDto(Order existingEntity, OrderDto dto) {
		// TODO Auto-generated method stub
		return null;
	}

}
