package com.hyp.service;

import org.springframework.stereotype.Service;

import com.hyp.request.PosDataRequest;

@Service
public interface PosDataService {

	public boolean savePosData(PosDataRequest posDataRequest);
	
}
