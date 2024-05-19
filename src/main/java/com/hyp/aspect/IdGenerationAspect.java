package com.hyp.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hyp.annotation.GenerateId;
import com.hyp.service.SequenceService;
import com.hyp.util.CommonUtils;

import java.lang.reflect.Field;

@Aspect
@Component
public class IdGenerationAspect {

	@Autowired
	SequenceService sequenceService;

	@Before("execution(* org.springframework.data.repository.CrudRepository.save(..)) && args(entity)")
	public void generateId(Object entity) throws IllegalAccessException {
		Class<?> entityClass = entity.getClass();
		for (Field field : entityClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(GenerateId.class)) {
				field.setAccessible(true);
				if (field.get(entity) == null) {
					GenerateId annotation = field.getAnnotation(GenerateId.class);
					String sequenceName = annotation.sequenceName();
					field.set(entity, !sequenceName.isEmpty() ? sequenceService.generateSequence(sequenceName)
							: CommonUtils.genId());
				}
			}
		}
	}
}
