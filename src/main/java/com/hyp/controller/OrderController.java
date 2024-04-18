package com.hyp.controller;

import java.util.Collections;


import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.dto.OrderDto;
import com.hyp.entity.Address;
import com.hyp.entity.Customer;
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.RiderStatusType;
import com.hyp.mapper.DataMapper;
import com.hyp.request.PosCallbackRequest;
import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest.RiderDetails;
import com.hyp.response.PosResponse;
import com.hyp.response.Response;
import com.hyp.service.AddressService;
import com.hyp.service.CustomerService;
import com.hyp.service.OrderService;
import com.hyp.service.PosService;
import com.hyp.service.RestaurantService;
import com.hyp.translation.PosOrderRequestTranslation;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/order")
public class OrderController extends BaseListController<OrderDto, Order, String> {

	@Autowired
	OrderService orderService;

	@Autowired
	DataMapper dataMapper;

	@Autowired
	RestaurantService restaurantService;

	@Autowired
	CustomerService customerService;

	@Autowired
	PosService posService;

	@Autowired
	AddressService addressService;

	@PostMapping()
	public ResponseEntity<Response> create(@RequestBody @Valid OrderDto orderDto) {
		Response response;
		try {
			Order createdOrder = orderService.create(orderDto);
			OrderDto createdOrderDto = dataMapper.toOrderDto(createdOrder);
			response = new Response(Collections.singletonList(createdOrderDto), false, "Order Created");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	// This is dummy api to simulate rider allocation, not to be used directly
	@PostMapping("/rider/{orderId}")
	public ResponseEntity<Response> orderRiderUpdate(@PathVariable("orderId") String orderId) {
		Response response;
		try {
			Order order = orderService.findById(orderId);
			Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
			PosRiderUpdateRequest posRiderUpdateRequest = PosOrderRequestTranslation
					.getPosRiderStatusUpdateRequest(restaurant, order, new RiderDetails("rider", "9964552656"),RiderStatusType.rider_assigned);
			String posResponse = posService.updatePosRiderStatus(posRiderUpdateRequest);
			response = new Response(Collections.singletonList(posResponse), false, "Rider Status Updated");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	// This is dummy api to simulate cancel order allocation, not to be used
	// directly
	@PostMapping("/cancel/{orderId}")
	public ResponseEntity<Response> orderCancel(@PathVariable("orderId") String orderId) {
		Response response;
		try {
			Order order = orderService.findById(orderId);
			Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
			PosOrderUpdateRequest posOrderUpdateRequest = PosOrderRequestTranslation
					.getPosOrderUpdateRequest(restaurant, order, "Customer Cancellation");
			String posResponse = posService.updatePosOrder(posOrderUpdateRequest);
			response = new Response(Collections.singletonList(posResponse), false, "Order Cancelled");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new Response(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
