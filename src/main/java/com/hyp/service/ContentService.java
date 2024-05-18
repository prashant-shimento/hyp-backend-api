package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.Content;
import com.hyp.repository.ContentRepository;

@Service
public class ContentService extends BaseServiceImpl<Content, String> {
	@Autowired
	ContentRepository featureContentRepository;
}
