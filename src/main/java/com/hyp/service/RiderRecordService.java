package com.hyp.service;

import com.hyp.entity.RiderRecord;
import com.hyp.repository.RiderRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RiderRecordService extends BaseServiceImpl<RiderRecord, String> {
    @Autowired
    RiderRecordRepository riderRecordRepository;
}
