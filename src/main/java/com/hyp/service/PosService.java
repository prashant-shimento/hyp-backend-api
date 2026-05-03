package com.hyp.service;

import com.hyp.entity.Delivery;
import com.hyp.entity.Order;
import com.hyp.enums.DeliveryFulfillStatusType;
import com.hyp.exception.PosException;
import com.hyp.request.PosDataRequest;
import com.hyp.request.PosOrderRequest;
import com.hyp.request.PosOrderUpdateRequest;
import com.hyp.request.PosRiderUpdateRequest;
import com.hyp.request.PosStatusRequest;
import com.hyp.request.PosStockRequest;

public interface PosService {

    void processPosOrder(Order order);

    boolean savePosData(PosDataRequest posDataRequest);

    void createPosOrder(PosOrderRequest posOrderRequest) throws PosException;

    void updatePosOrder(PosOrderUpdateRequest posOrderUpdateRequest) throws PosException;

    String updatePosRiderStatus(PosRiderUpdateRequest posRiderUpdateRequest);

    void updateRestaurant(PosStatusRequest updateStatus);

    boolean updateStock(PosStockRequest stockRequest);

    boolean isPosUpdateRequired(DeliveryFulfillStatusType fullFillStatus);

    void updatePosRiderStatus(Delivery delivery, Order order);
}
