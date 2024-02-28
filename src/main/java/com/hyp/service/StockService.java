package com.hyp.service;

import org.springframework.stereotype.Service;

import com.hyp.request.StockRequest;

@Service
public interface StockService {

	public boolean updateStock(StockRequest stockRequest);
}
