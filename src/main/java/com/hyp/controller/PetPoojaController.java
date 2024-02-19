package com.hyp.controller;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hyp.entity.OrderType;
import com.hyp.request.MenuRequest;
import com.hyp.request.OrderTypeRequest;
import com.hyp.request.StoreStatusRequest;
import com.hyp.service.OrderTypeService;
import com.hyp.service.PushMenuService;

@RestController
@RequestMapping("/api/rms")
public class PetPoojaController {

	@Autowired
	OrderTypeService orderTypeService;

	@Autowired
	PushMenuService pushMenuService;

	@PostMapping("/petpooja/menu")
	public void pushMenu(@RequestBody MenuRequest menuRequest) {
		JSONObject jb = new JSONObject(menuRequest);
		System.out.println(jb.toString());
		pushMenuService.pushMenu(menuRequest);
	}

	@PostMapping("/petpooja/stock")
	public String updateStock() {
		return "Stock Updated";
	}

	@PostMapping("/petpooja/get-status")
	public String getStatus(@RequestBody StoreStatusRequest getStoreStatusRequest) {
		System.out.println(getStoreStatusRequest.toString());
		getStoreStatusRequest.setHttp_code("200");
		getStoreStatusRequest.setMessage("Store status retrieved successfully");
		getStoreStatusRequest.setStatus("Open");
		getStoreStatusRequest.setStore_status("1");
		return getStoreStatusRequest.toString();
	}

	@PostMapping("/petpooja/update-status")
	public String updateStatus(@RequestBody StoreStatusRequest updateStoreStatusRequest) {
		System.out.println(updateStoreStatusRequest.toString());
		return updateStoreStatusRequest.toString();
	}

	@PostMapping("/petpooja/order-type")
    public String addOrderType(@RequestBody OrderTypeRequest orderTypeRequest) {
		OrderType orderType = new OrderType();
		orderType.setId(String.valueOf(orderTypeRequest.getOrderTypeId()));
		orderType.setOrderType(orderTypeRequest.getOrderType());
		orderTypeService.save(orderType);
        return "Success";
    }
}
