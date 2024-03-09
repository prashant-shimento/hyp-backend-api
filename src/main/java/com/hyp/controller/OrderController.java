package com.hyp.controller;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.OrderDto;
import com.hyp.entity.Order;
import com.hyp.enums.OrderStatusType;
import com.hyp.mapper.DataMapper;
import com.hyp.response.ResponseTemplate;
import com.hyp.service.OrderService;
import com.hyp.util.Utils;

@RestController
@RequestMapping("/api/order")
public class OrderController extends BaseListController<OrderDto, Order, String> {

	@Autowired
	OrderService orderService;

	@Autowired
	DataMapper dataMapper;

	@PostMapping()
	public ResponseEntity<ResponseTemplate> create(@RequestBody OrderDto orderDto){
		try {
			Order createdOrder = orderService.create(orderDto);
	        OrderDto createdOrderDto = dataMapper.toOrderDto(createdOrder);
			ResponseTemplate response = new ResponseTemplate(Collections.singletonList(createdOrderDto), false, "Order Created");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			ResponseTemplate response = new ResponseTemplate(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
