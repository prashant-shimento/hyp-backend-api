package com.hyp.service;

import java.util.List;

public interface BaseTranslationService<DTO, Entity> {

	Entity getEntity(DTO dto);

	DTO getDto(Entity entity);
	
	List<DTO> getDtoList(List<Entity> entities);
	
	void updateEntityFromDto(DTO dto, Entity entity);
}
