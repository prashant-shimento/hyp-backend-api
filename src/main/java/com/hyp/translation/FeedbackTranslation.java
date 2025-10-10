package com.hyp.translation;

import com.hyp.dto.FeedbackDto;
import com.hyp.entity.Feedback;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class FeedbackTranslation extends BaseTranslationServiceImpl<FeedbackDto, Feedback> {

    @Override
    protected Class<FeedbackDto> getDtoClass() {
        return FeedbackDto.class;
    }

    @Override
    protected Class<Feedback> getEntityClass() {
        return Feedback.class;
    }
}
