package com.hyp.controller;

import com.hyp.dto.SettlementDto;
import com.hyp.entity.Settlement;
import com.hyp.translation.SettlementTranslation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(path = {"/api/v2/settlement", "/api/v3/settlement"})
public class SettlementController extends BaseListController<SettlementDto, Settlement, String> {

    @Autowired
    public SettlementTranslation settlementTranslation;
}
