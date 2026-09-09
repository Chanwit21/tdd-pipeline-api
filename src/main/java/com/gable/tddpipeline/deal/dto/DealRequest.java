package com.gable.tddpipeline.deal.dto;

/**
 * Raw create/update payload for a deal. Intentionally loose types (String amount,
 * String closedDate as "yyyy-MM") so DealValidator can report FMT-* errors instead
 * of failing to deserialize. Situation & dealOwner are NOT accepted from the client.
 */
public record DealRequest(
        Long departmentId,
        String customer,
        String dealName,
        String dealType,
        String dealStatus,
        String dealStage,
        String probability,
        String closedDate,     // "yyyy-MM"
        String amount,         // digits + commas
        String projectCode,
        String costSheetNo,
        String createdDate     // "yyyy-MM-dd"
) {}
