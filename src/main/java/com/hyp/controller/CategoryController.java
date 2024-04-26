package com.hyp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.CategoryDto;
import com.hyp.entity.Category;
import com.hyp.translation.CategoryTranslation;

@RestController
@RequestMapping("/category")
public class CategoryController extends BaseController<CategoryDto, Category, String> {

	@Autowired
	CategoryTranslation categoryTranslation;

}
