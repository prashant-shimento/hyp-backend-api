package com.hyp.controller;

import com.hyp.dto.RiderRecordDto;
import com.hyp.entity.RiderRecord;
import com.hyp.service.RiderRecordService;
import com.hyp.translation.RiderRecordTranslation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"/api/v2/rider", "/api/v3/rider"})
public class RiderRecordController extends BaseController<RiderRecordDto, RiderRecord, String> {

    @Autowired
    public RiderRecordTranslation riderRecordTranslation;

    @Autowired
    public RiderRecordService riderRecordService;
}
