package com.hyp.service;

import java.util.List;

public interface TranslationService<DTO, Entity> {

	Entity getEntity(DTO dto);

	DTO getDto(Entity entity);
	
	List<DTO> getDtoList(List<Entity> entities);

	Entity getPatchDto(Entity existingEntity, DTO dto);
}
