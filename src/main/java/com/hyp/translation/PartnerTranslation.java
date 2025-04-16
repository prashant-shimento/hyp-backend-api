package com.hyp.translation;

import org.springframework.stereotype.Service;

import com.hyp.dto.PartnerDto;
import com.hyp.model.ApiConfig;
import com.hyp.entity.Partner;
import com.hyp.service.BaseTranslationServiceImpl;
import com.hyp.util.EncryptionUtils;

import lombok.SneakyThrows;

@Service
public class PartnerTranslation extends BaseTranslationServiceImpl<PartnerDto, Partner> {

	@Override
	protected Class<PartnerDto> getDtoClass() {
		return PartnerDto.class;
	}

	@Override
	protected Class<Partner> getEntityClass() {
		return Partner.class;
	}
	
	@Override
	@SneakyThrows
	public PartnerDto getDto(Partner entity) {
	    ApiConfig config = entity.getApiConfigs();
	    if (config != null) {
	        if (config.getKey() != null) config.setKey(EncryptionUtils.decrypt(config.getKey()));
	        if (config.getSecret() != null) config.setSecret(EncryptionUtils.decrypt(config.getSecret()));
	        if (config.getToken() != null) config.setToken(EncryptionUtils.decrypt(config.getToken()));
	        if (config.getPassword() != null) config.setPassword(EncryptionUtils.decrypt(config.getPassword()));
	    }
	    return super.getDto(entity);
	}
	
	@Override
	@SneakyThrows
	public Partner getEntity(PartnerDto dto) {
	    Partner entity = super.getEntity(dto);
	    ApiConfig config = entity.getApiConfigs();
	    if (config != null) {
	        if (config.getKey() != null) config.setKey(EncryptionUtils.encrypt(config.getKey()));
	        if (config.getSecret() != null) config.setSecret(EncryptionUtils.encrypt(config.getSecret()));
	        if (config.getToken() != null) config.setToken(EncryptionUtils.encrypt(config.getToken()));
	        if (config.getPassword() != null) config.setPassword(EncryptionUtils.encrypt(config.getPassword()));
	    }
	    return entity;
	}

}
