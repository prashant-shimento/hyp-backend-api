package com.hyp.aspect;

import com.hyp.annotation.GenerateId;
import com.hyp.service.SequenceService;
import com.hyp.util.CommonUtils;
import java.lang.reflect.Field;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class IdGenerationAspect {

    @Autowired
    SequenceService sequenceService;

    @Before("execution(* org.springframework.data.repository.CrudRepository.save(..)) && args(entity)")
    public void generateId(Object entity) throws IllegalAccessException {
        long start = System.currentTimeMillis();
        String entityName = entity.getClass().getSimpleName();
        boolean generated = false;

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
                        generated = true;
                    }
                }
            }
            clazz = clazz.getSuperclass(); // check parent class fields
        }

        long elapsed = System.currentTimeMillis() - start;
        if (elapsed > 10) { // Only log if > 10ms
            log.info("IdGenerationAspect for {} took {}ms (generated={})", entityName, elapsed, generated);
        }
    }
}
