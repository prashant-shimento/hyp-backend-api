package com.hyp.controller;

import com.hyp.dto.ContentDto;
import com.hyp.entity.Content;
import com.hyp.translation.ContentTranslation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"/api/v2/content", "/api/v3/content"})
public class ContentController extends BaseController<ContentDto, Content, String> {

    @Autowired
    public ContentTranslation contentTranslation;
}
