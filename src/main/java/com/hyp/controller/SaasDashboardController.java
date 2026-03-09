package com.hyp.controller;

import com.hyp.dto.SaasDashboardDto;
import com.hyp.entity.SaasDashboard;
import com.hyp.service.SaasDashboardService;
import com.hyp.translation.SaasDashboardTranslation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/saas-dashboard")
public class SaasDashboardController extends BaseController<SaasDashboardDto, SaasDashboard, String> {
    @Autowired
    public SaasDashboardTranslation saasDashboardTranslation;

    @Autowired
    public SaasDashboardService saasDashboardService;
}
