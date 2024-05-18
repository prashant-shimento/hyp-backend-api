package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.ContentDto;
import com.hyp.entity.Content;
import com.hyp.translation.ContentTranslation;

@RestController
@RequestMapping("/content")
public class ContentController extends BaseController<ContentDto, Content, String> {

	@Autowired
	public ContentTranslation contentTranslation;
}
