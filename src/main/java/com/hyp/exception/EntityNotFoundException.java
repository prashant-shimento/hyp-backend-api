package com.hyp.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EntityNotFoundException extends Exception {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String entityName;
    private String entityValue;

    public EntityNotFoundException(String entityName, String entityValue) {
        super(String.format("%s not found with value: %s", entityName, entityValue));
        this.entityName = entityName;
        this.entityValue = entityValue;
    }
}
