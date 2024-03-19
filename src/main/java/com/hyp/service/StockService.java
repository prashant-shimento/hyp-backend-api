package com.hyp.service;

import org.springframework.stereotype.Service;

import com.hyp.request.PosStockRequest;

@Service
public interface StockService {

	public boolean updateStock(PosStockRequest stockRequest);
}
