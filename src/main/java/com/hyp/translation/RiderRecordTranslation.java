package com.hyp.translation;

import org.springframework.stereotype.Service;

import com.hyp.dto.RiderRecordDto;
import com.hyp.entity.RiderRecord;
import com.hyp.service.BaseTranslationServiceImpl;

@Service
public class RiderRecordTranslation extends BaseTranslationServiceImpl<RiderRecordDto, RiderRecord> {

	@Override
	protected Class<RiderRecordDto> getDtoClass() {
		return RiderRecordDto.class;
	}

	@Override
	protected Class<RiderRecord> getEntityClass() {
		return RiderRecord.class;
	}

}
