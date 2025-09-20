package com.hyp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hyp.entity.RiderRecord;
import com.hyp.repository.RiderRecordRepository;

@Service
public class RiderRecordService extends BaseServiceImpl<RiderRecord, String> {
	@Autowired
	RiderRecordRepository riderRecordRepository;
}
