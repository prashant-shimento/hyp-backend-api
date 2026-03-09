package com.hyp.service;

import com.hyp.entity.SaasDashboard;
import com.hyp.repository.SaasDashboardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SaasDashboardService extends BaseServiceImpl<SaasDashboard, String> {
    @Autowired
    SaasDashboardRepository riderRecordRepository;
}
