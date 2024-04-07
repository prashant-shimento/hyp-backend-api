package com.hyp.service;

import org.springframework.stereotype.Service;

import com.hyp.exception.PosException;
import com.hyp.request.PosDataRequest;
import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.request.PosStockRequest;

@Service
public interface PosService {

	public boolean savePosData(PosDataRequest posDataRequest);

	public String createOrder(PosOrderRequest posOrderRequest) throws PosException;

	public String updateOrder(PosOrderUpdateRequest posOrderUpdateRequest) throws PosException;

	public String updateRiderStatus(PosRiderUpdateRequest posRiderUpdateRequest);

	public boolean updateStock(PosStockRequest stockRequest);

}
