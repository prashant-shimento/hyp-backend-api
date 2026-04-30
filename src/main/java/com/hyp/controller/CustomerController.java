package com.hyp.controller;

import com.hyp.dto.CustomerDto;
import com.hyp.entity.Customer;
import com.hyp.translation.CustomerTranslation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(path = {"/api/v2/customer", "/api/v3/customer"})
public class CustomerController extends BaseController<CustomerDto, Customer, String> {

    @Autowired
    public CustomerTranslation customerTranslation;

    {
        scopingMode = ScopingMode.SMART;
    }
}
