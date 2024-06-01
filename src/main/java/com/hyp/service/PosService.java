package com.hyp.service;

import org.springframework.stereotype.Service;

import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.exception.PosException;
import com.hyp.request.PosDataRequest;
import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.request.PosStockRequest;

@Service
public interface PosService {

	public boolean savePosData(PosDataRequest posDataRequest);

	public boolean createPosOrder(PosOrderRequest posOrderRequest) throws PosException;

	public String updatePosOrder(PosOrderUpdateRequest posOrderUpdateRequest) throws PosException;

	public String updatePosRiderStatus(PosRiderUpdateRequest posRiderUpdateRequest);

	public boolean updateStock(PosStockRequest stockRequest);
	
	public boolean isPosUpdateRequired(DeliveryFulfillStatusType fullFillStatus);
	
	public void updatePosRiderStatus(Delivery delivery, Order order);

}
