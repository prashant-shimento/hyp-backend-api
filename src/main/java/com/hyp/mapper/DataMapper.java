package com.hyp.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hyp.dto.OrderDto;
import com.hyp.entity.Order;

@Component
public class DataMapper {

	@Autowired
	ModelMapper modelMapper;
	
	public void setModelMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
	}

	public OrderDto toOrderDto(Order order) {
		return modelMapper.map(order, OrderDto.class);
	}

	public Order toOrderEntity(OrderDto orderDto) {
		return modelMapper.map(orderDto, Order.class);
	}
}
