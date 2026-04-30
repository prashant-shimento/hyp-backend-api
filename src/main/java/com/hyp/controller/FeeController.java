package com.hyp.controller;

import com.hyp.dto.FeeDto;
import com.hyp.entity.Fee;
import com.hyp.translation.FeeTranslation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"/api/v2/fee", "/api/v3/fee"})
public class FeeController extends BaseController<FeeDto, Fee, String> {

    @Autowired
    public FeeTranslation feeTranslation;
}
