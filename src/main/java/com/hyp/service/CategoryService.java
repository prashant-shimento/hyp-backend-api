package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.Category;
import com.hyp.repository.CategoryRepository;

@Service
public class CategoryService extends BaseServiceImpl<Category, String>{
	
	@Autowired
	CategoryRepository categoryRepository;

}
