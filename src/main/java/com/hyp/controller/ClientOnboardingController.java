package com.hyp.controller;

import com.hyp.dto.ClientOnboardingDto;
import com.hyp.entity.ClientOnboarding;
import com.hyp.translation.ClientOnboardingTranslation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client")
public class ClientOnboardingController extends BaseController<ClientOnboardingDto, ClientOnboarding, String> {

    @Autowired
    ClientOnboardingTranslation clientOnboardingTranslation;
}
