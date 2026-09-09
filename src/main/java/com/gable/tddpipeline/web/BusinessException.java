package com.gable.tddpipeline.web;

import lombok.Getter;

/** Generic business-rule violation with an error code -> HTTP 409. */
@Getter
public class BusinessException extends RuntimeException {
    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
}
