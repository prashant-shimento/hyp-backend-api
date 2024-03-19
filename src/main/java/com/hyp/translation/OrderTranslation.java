package com.hyp.translation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.dto.OrderDto;
import com.hyp.entity.Order;
import com.hyp.mapper.DataMapper;
import com.hyp.service.TranslationService;

@Service
public class OrderTranslation implements TranslationService<OrderDto, Order> {

	@Autowired
	DataMapper dataMapper;
	
	@Override
	public Order getEntity(OrderDto dto) {
		return dataMapper.toOrderEntity(dto);
	}

	@Override
	public OrderDto getDto(Order entity) {
		return dataMapper.toOrderDto(entity);
	}

	@Override
	public List<OrderDto> getDtoList(List<Order> entities) {
		List<OrderDto> orderDtoList = new ArrayList<>();
		for (Order order : entities) {
			orderDtoList.add(dataMapper.toOrderDto(order));
		}
		return orderDtoList;
	}

	@Override
	public Order getPatchDto(Order existingEntity, OrderDto dto) {
		// TODO Auto-generated method stub
		return null;
	}

}
