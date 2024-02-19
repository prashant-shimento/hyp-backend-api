package com.hyp.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class DataMapper {

	@Autowired
	ModelMapper modelMapper;
	

	public void setModelMapper(ModelMapper modelMapper) {
		this.modelMapper = modelMapper;
	}
}
