package com.hyp.service;

import com.hyp.entity.ClientOnboarding;
import com.hyp.repository.ClientOnboardingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientOnboardingService extends BaseServiceImpl<ClientOnboarding, String> {

    @Autowired
    ClientOnboardingRepository clientOnboardingRepository;
}
