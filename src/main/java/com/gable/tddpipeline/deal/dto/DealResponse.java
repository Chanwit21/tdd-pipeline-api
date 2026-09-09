package com.gable.tddpipeline.deal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DealResponse(
        Long id,
        String recordId,
        Long departmentId,
        String departmentCode,
        String departmentName,
        String dealOwner,
        String customer,
        String dealName,
        String dealType,
        String dealStatus,
        String dealStage,
        String probability,
        String situation,
        String closedDate,     // "yyyy-MM"
        BigDecimal amount,
        String projectCode,
        String costSheetNo,
        String createdDate,    // "yyyy-MM-dd"
        boolean legacyMigrated,
        String migrationRemark,
        boolean overdue,
        String rowColor,       // success | slate | danger | normal
        Instant createdAt,
        Instant updatedAt,
        List<NoteDto> notes,
        List<HistoryDto> history
) {
    public record NoteDto(Long id, String text, String authorName, Instant createdAt) {}
    public record HistoryDto(Long id, String field, String oldValue, String newValue,
                             String changedByName, Instant changedAt) {}
}
