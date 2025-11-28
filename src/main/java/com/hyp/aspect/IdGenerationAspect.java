package com.hyp.aspect;

import com.hyp.annotation.GenerateId;
import com.hyp.service.SequenceService;
import com.hyp.util.CommonUtils;
import java.lang.reflect.Field;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class IdGenerationAspect {

    @Autowired
    SequenceService sequenceService;

    @Before("execution(* org.springframework.data.repository.CrudRepository.save(..)) && args(entity)")
    public void generateId(Object entity) throws IllegalAccessException {
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(GenerateId.class)) {
                    field.setAccessible(true);
                    Object currentValue = field.get(entity);
                    if (currentValue == null || (currentValue instanceof String && ((String) currentValue).isEmpty())) {
                        GenerateId annotation = field.getAnnotation(GenerateId.class);
                        String sequenceName = annotation.sequenceName();

                        String generatedId = !sequenceName.isEmpty()
                                ? sequenceService.generateSequence(sequenceName)
                                : CommonUtils.genId();

                        field.set(entity, generatedId);
                    }
                }
            }
            clazz = clazz.getSuperclass(); // check parent class fields
        }
    }
}
