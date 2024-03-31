package com.hyp.controller;

import java.util.Collections;

import javax.validation.Valid;

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
import com.hyp.entity.Order;
import com.hyp.entity.Restaurant;
import com.hyp.enums.RiderStatusType;
import com.hyp.mapper.DataMapper;
import com.hyp.request.PosCallbackRequest;
import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.response.Response;
import com.hyp.response.ResponseTemplate;
import com.hyp.service.CustomerService;
import com.hyp.service.OrderService;
import com.hyp.service.PosService;
import com.hyp.service.RestaurantService;
import com.hyp.translation.PosOrderRequestTranslation;

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

	@PostMapping()
	public ResponseEntity<ResponseTemplate> create(@RequestBody @Valid OrderDto orderDto) {
		ResponseTemplate response;
		try {
			Order createdOrder = orderService.create(orderDto);
			OrderDto createdOrderDto = dataMapper.toOrderDto(createdOrder);

			PosOrderRequest posOrderRequest = PosOrderRequestTranslation.getPosOrderRequest(
					restaurantService.findById(orderDto.getRestaurantId()), createdOrder,
					customerService.findById(orderDto.getCustomerId()));

			posService.createOrder(posOrderRequest);
			response = new ResponseTemplate(Collections.singletonList(createdOrderDto), false, "Order Created");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new ResponseTemplate(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@PostMapping("/callback")
	public ResponseEntity<Response> orderCallBack(@RequestBody PosCallbackRequest posCallbackRequest) {
		JSONObject jb = new JSONObject(posCallbackRequest);
		System.out.println(jb.toString());
		Response response = null;
		try {
			orderService.processCallback(posCallbackRequest);
			response = new Response.Builder().httpCode(HttpStatus.OK.value()).message("Order Updated Successfully")
					.error(null).build();
			return new ResponseEntity<Response>(response, HttpStatus.OK);
		} catch (Exception e) {
			e.printStackTrace();
			response = new Response.Builder().httpCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
					.message("Error Occured").error(e.getMessage()).build();
			return new ResponseEntity<Response>(response, HttpStatus.OK);
		}
	}

	@PostMapping("/rider/{orderId}")
	public ResponseEntity<ResponseTemplate> orderRiderUpdate(@PathVariable("orderId") String orderId) {
		ResponseTemplate response;
		try {
			Order order = orderService.findById(orderId);
			Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
			PosRiderUpdateRequest posRiderUpdateRequest = PosOrderRequestTranslation
					.getPosRiderStatusUpdateRequest(restaurant, order, RiderStatusType.rider_assigned);
			String posResponse = posService.updateRiderStatus(posRiderUpdateRequest);
			response = new ResponseTemplate(Collections.singletonList(posResponse), false, "Rider Status Updated");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new ResponseTemplate(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@PostMapping("/cancel/{orderId}")
	public ResponseEntity<ResponseTemplate> orderCancel(@PathVariable("orderId") String orderId) {
		ResponseTemplate response;
		try {
			Order order = orderService.findById(orderId);
			Restaurant restaurant = restaurantService.findById(order.getRestaurantId());
			PosOrderUpdateRequest posOrderUpdateRequest = PosOrderRequestTranslation
					.getPosOrderUpdateRequest(restaurant, order, "Customer Cancellation");
			String posResponse = posService.updateOrder(posOrderUpdateRequest);
			response = new ResponseTemplate(Collections.singletonList(posResponse), false, "Order Cancelled");
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			response = new ResponseTemplate(null, true, e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
