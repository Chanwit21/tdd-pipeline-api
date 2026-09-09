package com.gable.tddpipeline.dashboard;

import com.gable.tddpipeline.deal.DealMapper;
import com.gable.tddpipeline.domain.Deal;
import com.gable.tddpipeline.repo.DealRepository;
import com.gable.tddpipeline.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DealRepository dealRepo;

    @Transactional(readOnly = true)
    public Map<String, Object> summary(Long departmentId, String from, String to, AppUserPrincipal me) {
        Long scopeDept = me.isAdmin() ? departmentId : me.getDepartmentId();

        YearMonth fromYm = parse(from);
        YearMonth toYm = parse(to);

        List<Deal> deals = dealRepo.findAll().stream()
                .filter(d -> scopeDept == null || Objects.equals(d.getDepartment().getId(), scopeDept))
                .toList();

        List<Deal> active = deals.stream()
                .filter(d -> !"Inactive".equals(d.getDealStatus()))
                .toList();

        BigDecimal totalPipeline = sum(active);
        BigDecimal bestCase = sum(active.stream()
                .filter(d -> "Best Case".equals(d.getSituation())).toList());
        BigDecimal wonAmount = sum(deals.stream()
                .filter(d -> Set.of("Won", "PO").contains(d.getDealStage()))
                .filter(d -> inRange(YearMonth.from(d.getClosedDate()), fromYm, toYm))
                .toList());

        List<Deal> overdue = deals.stream().filter(DealMapper::isOverdue)
                .sorted(Comparator.comparing(Deal::getClosedDate))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("statCards", Map.of(
                "totalPipelineAmount", totalPipeline,
                "bestCaseAmount", bestCase,
                "wonAmount", wonAmount,
                "activeDealCount", active.size()));
        result.put("overdue", overdue.stream().map(d -> Map.of(
                "id", d.getId(),
                "recordId", d.getRecordId(),
                "customer", d.getCustomer(),
                "dealName", d.getDealName(),
                "department", d.getDepartment().getCode(),
                "dealOwner", d.getDealOwner(),
                "closedDate", YearMonth.from(d.getClosedDate()).toString(),
                "amount", d.getAmount())).toList());

        if (me.isAdmin()) {
            Map<String, List<Deal>> byDept = active.stream()
                    .collect(Collectors.groupingBy(d -> d.getDepartment().getCode(), TreeMap::new, Collectors.toList()));
            result.put("byDepartment", byDept.entrySet().stream().map(e -> Map.of(
                    "department", e.getKey(),
                    "dealCount", e.getValue().size(),
                    "amount", sum(e.getValue()),
                    "bestCase", sum(e.getValue().stream()
                            .filter(d -> "Best Case".equals(d.getSituation())).toList()))).toList());
        }
        return result;
    }

    private static YearMonth parse(String s) {
        try {
            return s == null || s.isBlank() ? null : YearMonth.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean inRange(YearMonth v, YearMonth from, YearMonth to) {
        if (from != null && v.isBefore(from)) return false;
        return to == null || !v.isAfter(to);
    }

    private static BigDecimal sum(List<Deal> deals) {
        return deals.stream().map(Deal::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
