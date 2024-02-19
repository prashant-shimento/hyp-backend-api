package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hyp.entity.Category;
import com.hyp.repository.CategoryRepository;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class CategoryServiceImpl extends BaseServiceImpl<Category, String,CategoryRepository> implements CategoryService {

	@Autowired
	private CategoryRepository repository;

}
