package com.hyp.translation;

import com.hyp.dto.ClientOnboardingDto;
import com.hyp.entity.ClientOnboarding;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ClientOnboardingTranslation extends BaseTranslationServiceImpl<ClientOnboardingDto, ClientOnboarding> {

    @Override
    protected Class<ClientOnboardingDto> getDtoClass() {
        return ClientOnboardingDto.class;
    }

    @Override
    protected Class<ClientOnboarding> getEntityClass() {
        return ClientOnboarding.class;
    }
}
