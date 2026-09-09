package com.gable.tddpipeline.web;

import lombok.Getter;

import java.util.List;

/** Thrown by DealValidator when a create/update payload fails business rules. -> HTTP 422 */
@Getter
public class DealValidationException extends RuntimeException {
    private final List<FieldError> errors;

    public DealValidationException(List<FieldError> errors) {
        super("Deal validation failed: " + errors.size() + " error(s)");
        this.errors = errors;
    }
}
