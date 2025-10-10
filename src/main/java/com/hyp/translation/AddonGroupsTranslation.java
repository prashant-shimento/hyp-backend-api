package com.hyp.translation;

import com.hyp.dto.AddonGroupDto;
import com.hyp.entity.AddonGroup;
import com.hyp.service.BaseTranslationServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class AddonGroupsTranslation extends BaseTranslationServiceImpl<AddonGroupDto, AddonGroup> {

    @Override
    protected Class<AddonGroupDto> getDtoClass() {
        return AddonGroupDto.class;
    }

    @Override
    protected Class<AddonGroup> getEntityClass() {
        return AddonGroup.class;
    }
}
