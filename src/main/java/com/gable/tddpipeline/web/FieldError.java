package com.gable.tddpipeline.web;

/** One validation failure. {@code code} matches the validation spec — FE maps message by code. */
public record FieldError(String field, String code, String message) {}
