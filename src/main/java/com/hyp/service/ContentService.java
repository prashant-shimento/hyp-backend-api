package com.hyp.service;

import com.hyp.entity.Content;
import com.hyp.repository.ContentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContentService extends BaseServiceImpl<Content, String> {
    @Autowired
    ContentRepository contentRepository;
}
