package com.hyp.controller;

import com.hyp.dto.TaxDto;
import com.hyp.entity.Tax;
import com.hyp.translation.TaxTranslation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {"/api/v2/tax", "/api/v3/tax"})
public class TaxController extends BaseController<TaxDto, Tax, String> {

    @Autowired
    public TaxTranslation taxTranslation;
}
