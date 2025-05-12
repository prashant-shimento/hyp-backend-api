package com.hyp.controller;

import com.hyp.dto.OrderTypeDto;
import com.hyp.entity.OrderType;
import com.hyp.entity.Partner;
import com.hyp.translation.OrderTypeTranslation;
import com.hyp.translation.PartnerTranslation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order-type")
public class OrderTypeController extends BaseController<OrderTypeDto, OrderType, String> {

	@Autowired
	public OrderTypeTranslation orderTypeTranslation;
	
}
