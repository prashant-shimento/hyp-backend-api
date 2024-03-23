package com.hyp.service;

import org.springframework.stereotype.Service;

import com.hyp.request.PosDataRequest;
import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;

@Service
public interface PosService {

	public boolean savePosData(PosDataRequest posDataRequest);

	public String createOrder(PosOrderRequest posOrderRequest);

	public String updateOrder(PosOrderUpdateRequest posOrderUpdateRequest);

	public String updateRiderStatus(PosRiderUpdateRequest posRiderUpdateRequest);

}
