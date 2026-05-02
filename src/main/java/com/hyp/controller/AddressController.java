package com.hyp.controller;

import com.hyp.dto.AddressDto;
import com.hyp.entity.Address;
import com.hyp.translation.AddressTranslation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(path = {"/api/v2/address", "/api/v3/address"})
public class AddressController extends BaseController<AddressDto, Address, String> {

    @Autowired
    public AddressTranslation addressTranslation;

    {
        scopingMode = ScopingMode.SMART;
    }
}
