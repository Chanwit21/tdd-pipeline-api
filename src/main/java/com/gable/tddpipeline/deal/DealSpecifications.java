package com.gable.tddpipeline.deal;

import com.gable.tddpipeline.deal.dto.DealQuery;
import com.gable.tddpipeline.domain.Deal;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class DealSpecifications {
    private DealSpecifications() {}

    public static Specification<Deal> build(DealQuery q, Long forcedDepartmentId) {
        return (root, cq, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            Long deptId = forcedDepartmentId != null ? forcedDepartmentId : q.departmentId();
            if (deptId != null) {
                ps.add(cb.equal(root.get("department").get("id"), deptId));
            }
            if (q.dealStatus() != null && !q.dealStatus().isEmpty()) {
                ps.add(root.get("dealStatus").in(q.dealStatus()));
            }
            if (q.dealStage() != null && !q.dealStage().isEmpty()) {
                ps.add(root.get("dealStage").in(q.dealStage()));
            }
            YearMonth from = DealValidator.parseYearMonth(q.closedFrom());
            if (from != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("closedDate"), from.atDay(1)));
            }
            YearMonth to = DealValidator.parseYearMonth(q.closedTo());
            if (to != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("closedDate"), to.atEndOfMonth()));
            }
            if (q.search() != null && !q.search().isBlank()) {
                String like = "%" + q.search().trim().toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("customer")), like),
                        cb.like(cb.lower(root.get("dealName")), like)));
            }
            if (Boolean.TRUE.equals(q.overdueOnly())) {
                ps.add(cb.and(
                        cb.equal(root.get("dealStatus"), "Follow Up"),
                        cb.lessThan(root.get("closedDate"), YearMonth.now().atDay(1))));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }
}
