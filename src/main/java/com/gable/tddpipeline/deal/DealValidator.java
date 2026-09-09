package com.gable.tddpipeline.deal;

import com.gable.tddpipeline.deal.dto.DealRequest;
import com.gable.tddpipeline.web.FieldError;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * All deal business rules from validation-business-rules-spec.md §2–§4.
 * Pure logic — no Spring, no DB — so it can be unit-tested directly (DealValidatorTest).
 * Master-config-derived facts are passed in via {@link Context}.
 */
public class DealValidator {

    public record Context(
            Set<String> validDealTypes,
            Set<String> validDealStatuses,
            Set<String> validProbabilities,
            /** allowed Deal Stage names for the submitted Deal Status */
            Set<String> stagesForSubmittedStatus,
            boolean departmentExists,
            String wonProbability,
            String poProbability
    ) {}

    public List<FieldError> validate(DealRequest r, Context ctx) {
        List<FieldError> errors = new ArrayList<>();

        // 1. Department
        if (r.departmentId() == null || !ctx.departmentExists()) {
            errors.add(new FieldError("departmentId", "REQ-DEPT", "กรุณาเลือกแผนก"));
        }

        // 3. Customer
        if (isBlank(r.customer())) {
            errors.add(new FieldError("customer", "REQ-CUSTOMER", "กรุณากรอกชื่อลูกค้า"));
        }

        // 4. Deal Type
        if (isBlank(r.dealType()) || !ctx.validDealTypes().contains(r.dealType())) {
            errors.add(new FieldError("dealType", "REQ-DEALTYPE", "กรุณาเลือก Deal Type"));
        }

        // 5. Deal Name
        if (isBlank(r.dealName())) {
            errors.add(new FieldError("dealName", "REQ-DEALNAME", "กรุณากรอกชื่อ Deal"));
        }

        // 6. Deal Status
        boolean statusOk = !isBlank(r.dealStatus()) && ctx.validDealStatuses().contains(r.dealStatus());
        if (!statusOk) {
            errors.add(new FieldError("dealStatus", "REQ-STATUS", "กรุณาเลือก Deal Status"));
        }

        // 7. Deal Stage — required + cascade rule (§3)
        if (isBlank(r.dealStage())) {
            errors.add(new FieldError("dealStage", "REQ-STAGE", "กรุณาเลือก Deal Stage"));
        } else if (statusOk && !ctx.stagesForSubmittedStatus().contains(r.dealStage())) {
            errors.add(new FieldError("dealStage", "RULE-STAGE-CASCADE",
                    "Deal Stage ไม่ตรงกับ Deal Status ที่เลือก"));
        }

        // 8. Probability
        boolean probOk = !isBlank(r.probability()) && ctx.validProbabilities().contains(r.probability());
        if (!probOk) {
            errors.add(new FieldError("probability", "REQ-PROB", "กรุณาเลือก Probability"));
        }

        // 10. Closed Date (month picker -> "yyyy-MM")
        if (parseYearMonth(r.closedDate()) == null) {
            errors.add(new FieldError("closedDate", "REQ-CLOSEDDATE", "กรุณาเลือก Closed Date"));
        }

        // 11. Amount — digits/commas only, parses > 0
        BigDecimal amount = parseAmount(r.amount());
        if (isBlank(r.amount())) {
            errors.add(new FieldError("amount", "REQ-AMOUNT", "กรุณากรอก Amount เป็นตัวเลข"));
        } else if (amount == null || amount.signum() <= 0) {
            errors.add(new FieldError("amount", "FMT-AMOUNT", "กรุณากรอก Amount เป็นตัวเลขบวก"));
        }

        // 14. Created Date
        if (parseDate(r.createdDate()) == null) {
            errors.add(new FieldError("createdDate", "REQ-CREATEDDATE", "กรุณาเลือก Created Date"));
        }

        // §4 Cross-field: Probability <-> Deal Stage (only when both present & coherent)
        if (probOk && !isBlank(r.dealStage())) {
            if ("Won".equals(r.dealStage()) && !ctx.wonProbability().equals(r.probability())) {
                errors.add(new FieldError("probability", "XREF-WON-PROB",
                        "Deal Stage = Won ต้องมี Probability = " + ctx.wonProbability() + " ก่อนบันทึก"));
            }
            if ("PO".equals(r.dealStage()) && !ctx.poProbability().equals(r.probability())) {
                errors.add(new FieldError("probability", "XREF-PO-PROB",
                        "Deal Stage = PO ต้องมี Probability = " + ctx.poProbability() + " ก่อนบันทึก"));
            }
        }

        return errors;
    }

    /* ---------- parsing helpers (also reused by DealService for persistence) ---------- */

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static YearMonth parseYearMonth(String s) {
        if (isBlank(s)) return null;
        try {
            return YearMonth.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalDate parseDate(String s) {
        if (isBlank(s)) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static BigDecimal parseAmount(String s) {
        if (isBlank(s)) return null;
        String cleaned = s.trim().replace(",", "");
        if (!cleaned.matches("\\d+(\\.\\d{1,2})?")) return null;
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
