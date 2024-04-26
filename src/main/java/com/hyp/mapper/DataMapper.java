package com.hyp.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hyp.dto.AddonGroupDto;
import com.hyp.dto.OrderDto;
import com.hyp.entity.AddonGroup;
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

	public AddonGroupDto toAddonGroupDto(AddonGroup addonGroup) {
		return modelMapper.map(addonGroup, AddonGroupDto.class);
	}

	public AddonGroup toAddonGroupEntity(AddonGroupDto addonGroupDto) {
		return modelMapper.map(addonGroupDto, AddonGroup.class);
	}

	public List<AddonGroupDto> toAddonGroupDtoList(List<AddonGroup> addonGroups) {
		return addonGroups.stream().map(this::toAddonGroupDto).collect(Collectors.toList());
	}

	public List<AddonGroup> toAddonGroupEntityList(List<AddonGroupDto> addonGroupDtos) {
		return addonGroupDtos.stream().map(this::toAddonGroupEntity).collect(Collectors.toList());
	}
}
