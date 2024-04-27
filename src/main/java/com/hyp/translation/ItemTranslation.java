package com.hyp.translation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.dto.ItemDto;
import com.hyp.dto.OrderDto;
import com.hyp.entity.Item;
import com.hyp.entity.Order;
import com.hyp.mapper.DataMapper;
import com.hyp.service.TranslationService;

@Service
public class ItemTranslation implements TranslationService<ItemDto, Item> {

	@Autowired
	DataMapper dataMapper;

	@Override
	public Item getEntity(ItemDto dto) {
		return dataMapper.toItemEntity(dto);
	}

	@Override
	public ItemDto getDto(Item entity) {
		return dataMapper.toItemDto(entity);
	}

	@Override
	public List<ItemDto> getDtoList(List<Item> entities) {
		List<ItemDto> itemDtoList = new ArrayList<>();
		for (Item item : entities) {
			itemDtoList.add(dataMapper.toItemDto(item));
		}
		return itemDtoList;
	}

	@Override
	public Item getPatchDto(Item existingEntity, ItemDto dto) {
		// TODO Auto-generated method stub
		return null;
	}

}
