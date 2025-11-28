package com.hyp.validation;

import com.hyp.exception.EntityNotFoundException;
import com.hyp.exception.ValidationException;

public interface BaseValidator<DTO> {
    void validate(DTO dto) throws ValidationException, EntityNotFoundException;
}
