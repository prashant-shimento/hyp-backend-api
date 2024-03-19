package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.Item;
import com.hyp.repository.ItemRepository;

@Service
public class ItemService extends BaseServiceImpl<Item, String> {
	@Autowired
	ItemRepository itemRepository;


	

}
