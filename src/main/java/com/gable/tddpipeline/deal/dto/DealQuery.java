package com.gable.tddpipeline.deal.dto;

import java.util.List;

/** Filters for GET /api/deals (all optional). */
public record DealQuery(
        Long departmentId,
        List<String> dealStatus,
        List<String> dealStage,
        String closedFrom,   // "yyyy-MM"
        String closedTo,     // "yyyy-MM"
        String search,       // matches customer / dealName
        Boolean overdueOnly
) {}
