package com.hyp.service;

import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;

public interface PosOrderService {

	public String createOrder(PosOrderRequest posOrderRequest);
	
	public String updateOrder(PosOrderUpdateRequest posOrderUpdateRequest);
	
	public String updateRiderStatus(PosRiderUpdateRequest posRiderUpdateRequest);


}
