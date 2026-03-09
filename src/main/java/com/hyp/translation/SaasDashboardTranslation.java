package com.hyp.translation;

import com.hyp.dto.SaasDashboardDto;
import com.hyp.entity.SaasDashboard;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SaasDashboardTranslation extends BaseTranslationServiceImpl<SaasDashboardDto, SaasDashboard> {

    @Override
    protected Class<SaasDashboardDto> getDtoClass() {
        return SaasDashboardDto.class;
    }

    @Override
    protected Class<SaasDashboard> getEntityClass() {
        return SaasDashboard.class;
    }
}
