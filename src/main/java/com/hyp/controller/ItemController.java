package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.ItemDto;
import com.hyp.entity.Item;
import com.hyp.translation.ItemTranslation;

@RestController
@RequestMapping("/item")
public class ItemController extends BaseListController<ItemDto, Item, String> {

	@Autowired
	public ItemTranslation itemTranslation;
}
