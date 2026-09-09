package com.gable.tddpipeline.deal;

import com.gable.tddpipeline.domain.Deal;
import com.gable.tddpipeline.domain.DealHistory;
import com.gable.tddpipeline.domain.DealNote;
import com.gable.tddpipeline.deal.dto.DealResponse;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DealMapper {
    private DealMapper() {}

    private static final Set<String> GREEN = Set.of("Won", "PO");
    private static final Set<String> GREY = Set.of("Lost", "Cancelled", "On Hold");

    public static boolean isOverdue(Deal d) {
        return "Follow Up".equals(d.getDealStatus())
                && d.getClosedDate() != null
                && YearMonth.from(d.getClosedDate()).isBefore(YearMonth.now());
    }

    /** Legacy deal still breaks a Won/PO probability rule? -> shows red until fixed. */
    public static boolean hasUnresolvedLegacyError(Deal d, String wonProb, String poProb) {
        if (!d.isLegacyMigrated()) return false;
        if ("Won".equals(d.getDealStage()) && !wonProb.equals(d.getProbability())) return true;
        return "PO".equals(d.getDealStage()) && !poProb.equals(d.getProbability());
    }

    public static String rowColor(Deal d, boolean overdue, boolean legacyError) {
        if (overdue || legacyError) return "danger";
        if (GREEN.contains(d.getDealStage())) return "success";
        if (GREY.contains(d.getDealStage())) return "slate";
        return "normal";
    }

    public static DealResponse toResponse(Deal d,
                                          List<DealNote> notes,
                                          List<DealHistory> history,
                                          Map<Long, String> userNames,
                                          String wonProb, String poProb) {
        boolean overdue = isOverdue(d);
        boolean legacyError = hasUnresolvedLegacyError(d, wonProb, poProb);

        List<DealResponse.NoteDto> noteDtos = notes.stream()
                .map(n -> new DealResponse.NoteDto(n.getId(), n.getNoteText(),
                        userNames.getOrDefault(n.getCreatedBy(), "—"), n.getCreatedAt()))
                .toList();

        List<DealResponse.HistoryDto> histDtos = history.stream()
                .map(h -> new DealResponse.HistoryDto(h.getId(), h.getChangedField(),
                        h.getOldValue(), h.getNewValue(),
                        userNames.getOrDefault(h.getChangedBy(), "—"), h.getChangedAt()))
                .toList();

        return new DealResponse(
                d.getId(), d.getRecordId(),
                d.getDepartment().getId(), d.getDepartment().getCode(), d.getDepartment().getName(),
                d.getDealOwner(), d.getCustomer(), d.getDealName(), d.getDealType(),
                d.getDealStatus(), d.getDealStage(), d.getProbability(), d.getSituation(),
                d.getClosedDate() != null ? YearMonth.from(d.getClosedDate()).toString() : null,
                d.getAmount(), d.getProjectCode(), d.getCostSheetNo(),
                d.getCreatedDate() != null ? d.getCreatedDate().toString() : null,
                d.isLegacyMigrated(), d.getMigrationRemark(),
                overdue, rowColor(d, overdue, legacyError),
                d.getCreatedAt(), d.getUpdatedAt(),
                noteDtos, histDtos);
    }

    public static LocalDate firstOfMonth(YearMonth ym) {
        return ym.atDay(1);
    }
}
