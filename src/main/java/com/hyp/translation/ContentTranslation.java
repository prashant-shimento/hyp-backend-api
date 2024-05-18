package com.hyp.translation;

import org.springframework.stereotype.Service;

import com.hyp.dto.ContentDto;
import com.hyp.entity.Content;
import com.hyp.service.BaseTranslationServiceImpl;

@Service
public class ContentTranslation extends BaseTranslationServiceImpl<ContentDto, Content> {

	@Override
	protected Class<ContentDto> getDtoClass() {
		return ContentDto.class;
	}

	@Override
	protected Class<Content> getEntityClass() {
		return Content.class;
	}

}
