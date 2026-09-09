package com.gable.tddpipeline.deal;

import com.gable.tddpipeline.deal.dto.DealRequest;
import com.gable.tddpipeline.web.FieldError;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Covers validation-business-rules-spec.md §2–§4 by error code. */
class DealValidatorTest {

    private final DealValidator validator = new DealValidator();

    private DealValidator.Context ctx(String status) {
        Set<String> stages = switch (status == null ? "" : status) {
            case "Follow Up" -> Set.of("Contact Created", "Prospecting", "Appointment Scheduled",
                    "Operational People Bought-In", "Decision Maker Bought-In", "Final Proposal Submit", "Won");
            case "PR" -> Set.of("PO");
            case "Inactive" -> Set.of("Lost", "Cancelled", "On Hold");
            default -> Set.of();
        };
        return new DealValidator.Context(
                Set.of("New", "Renew"),
                Set.of("Follow Up", "PR", "Inactive"),
                Set.of("< 50%", "50% - 74%", "75% - 98%", "99% - 100%"),
                stages,
                true,
                "75% - 98%",
                "99% - 100%");
    }

    private DealRequest valid() {
        return new DealRequest(1L, "ACME", "Deal name", "New", "Follow Up", "Prospecting",
                "50% - 74%", "2026-03", "1,000,000", null, null, "2026-01-15");
    }

    private Set<String> codes(List<FieldError> errs) {
        return errs.stream().map(FieldError::code).collect(Collectors.toSet());
    }

    @Test
    void validPayload_hasNoErrors() {
        assertThat(validator.validate(valid(), ctx("Follow Up"))).isEmpty();
    }

    @Test
    void test_REQ_fields_whenAllBlank() {
        var r = new DealRequest(null, "  ", "", "", "", "", "", "", "", null, null, "");
        assertThat(codes(validator.validate(r, ctx(null))))
                .contains("REQ-DEPT", "REQ-CUSTOMER", "REQ-DEALTYPE", "REQ-DEALNAME",
                        "REQ-STATUS", "REQ-STAGE", "REQ-PROB", "REQ-CLOSEDDATE",
                        "REQ-AMOUNT", "REQ-CREATEDDATE");
    }

    @Test
    void test_RULE_STAGE_CASCADE_whenStageNotInStatusGroup() {
        var r = new DealRequest(1L, "ACME", "Deal", "New", "PR", "Prospecting",
                "99% - 100%", "2026-03", "1000", null, null, "2026-01-15");
        assertThat(codes(validator.validate(r, ctx("PR")))).contains("RULE-STAGE-CASCADE");
    }

    @Test
    void test_XREF_WON_PROB_blocksWhenProbabilityMismatch() {
        var r = new DealRequest(1L, "ACME", "Deal", "New", "Follow Up", "Won",
                "50% - 74%", "2026-03", "1000", null, null, "2026-01-15");
        assertThat(codes(validator.validate(r, ctx("Follow Up")))).contains("XREF-WON-PROB");
    }

    @Test
    void test_XREF_WON_PROB_passesWhenProbabilityMatches() {
        var r = new DealRequest(1L, "ACME", "Deal", "New", "Follow Up", "Won",
                "75% - 98%", "2026-03", "1000", null, null, "2026-01-15");
        assertThat(codes(validator.validate(r, ctx("Follow Up")))).doesNotContain("XREF-WON-PROB");
    }

    @Test
    void test_XREF_PO_PROB_blocksWhenProbabilityMismatch() {
        var r = new DealRequest(1L, "ACME", "Deal", "Renew", "PR", "PO",
                "75% - 98%", "2026-03", "1000", null, null, "2026-01-15");
        assertThat(codes(validator.validate(r, ctx("PR")))).contains("XREF-PO-PROB");
    }

    @Test
    void test_FMT_AMOUNT_whenNegativeOrNonNumeric() {
        var r = new DealRequest(1L, "ACME", "Deal", "New", "Follow Up", "Prospecting",
                "50% - 74%", "2026-03", "abc", null, null, "2026-01-15");
        assertThat(codes(validator.validate(r, ctx("Follow Up")))).contains("FMT-AMOUNT");
    }

    @Test
    void test_closedDate_acceptsYearMonth() {
        assertThat(DealValidator.parseYearMonth("2026-03")).isNotNull();
        assertThat(DealValidator.parseYearMonth("2026-3-1")).isNull();
    }
}
