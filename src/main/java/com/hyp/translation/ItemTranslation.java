package com.hyp.translation;

import com.hyp.dto.ItemDto;
import com.hyp.entity.Item;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ItemTranslation extends BaseTranslationServiceImpl<ItemDto, Item> {

    @Override
    protected Class<ItemDto> getDtoClass() {
        return ItemDto.class;
    }

    @Override
    protected Class<Item> getEntityClass() {
        return Item.class;
    }
}
