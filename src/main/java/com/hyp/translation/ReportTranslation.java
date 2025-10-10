package com.hyp.translation;

import com.hyp.dto.ReportDto;
import com.hyp.entity.Report;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ReportTranslation extends BaseTranslationServiceImpl<ReportDto, Report> {

    @Override
    protected Class<ReportDto> getDtoClass() {
        return ReportDto.class;
    }

    @Override
    protected Class<Report> getEntityClass() {
        return Report.class;
    }
}
