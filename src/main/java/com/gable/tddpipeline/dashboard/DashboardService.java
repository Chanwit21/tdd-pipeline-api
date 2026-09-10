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
    public Map<String, Object> summary(List<Long> departmentId, String from, String to, List<String> probability,
                                      List<String> dealStatus, Integer createdYear, Integer quarter, AppUserPrincipal me) {
        Long scopeDept = me.isAdmin() ? null : Objects.requireNonNullElse(me.getDepartmentId(), -1L);
        if (quarter != null && (quarter < 1 || quarter > 4)) throw new com.gable.tddpipeline.web.BusinessException("QUARTER", "Quarter must be 1–4");

        YearMonth fromYm = parse(from);
        YearMonth toYm = parse(to);

        List<Deal> scoped = dealRepo.findAll().stream()
                .filter(d -> scopeDept != null ? Objects.equals(d.getDepartment().getId(), scopeDept) : departmentId == null || departmentId.isEmpty() || departmentId.contains(d.getDepartment().getId()))
                .filter(d -> probability == null || probability.isEmpty() || probability.contains(d.getProbability()))
                .filter(d -> dealStatus == null || dealStatus.isEmpty() || dealStatus.contains(d.getDealStatus()))
                .filter(d -> inRange(YearMonth.from(d.getClosedDate()), fromYm, toYm))
                .toList();
        List<Deal> deals = scoped.stream()
                .filter(d -> createdYear == null || d.getCreatedDate().getYear() == createdYear)
                .filter(d -> quarter == null || (d.getCreatedDate().getMonthValue() - 1) / 3 + 1 == quarter)
                .toList();

        List<Deal> active = deals.stream()
                .filter(d -> !"Inactive".equals(d.getDealStatus()))
                .toList();

        BigDecimal totalPipeline = sum(active);
        BigDecimal bestCase = sum(active.stream()
                .filter(d -> "Best Case".equals(d.getSituation())).toList());
        BigDecimal wonAmount = sum(deals.stream()
                .filter(d -> Set.of("Won", "PO").contains(d.getDealStage()))
                .filter(d -> YearMonth.from(d.getClosedDate()).equals(YearMonth.now()))
                .toList());

        List<Deal> overdue = deals.stream().filter(DealMapper::isOverdue)
                .sorted(Comparator.comparing(Deal::getClosedDate))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("byYear", scoped.stream().collect(Collectors.groupingBy(d -> d.getCreatedDate().getYear(), TreeMap::new, Collectors.toList())).entrySet().stream().map(e -> Map.of(
                "year", e.getKey(), "dealCount", e.getValue().size(), "amount", sum(e.getValue()))).toList());
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

        {
            Map<String, List<Deal>> byDept = active.stream()
                    .collect(Collectors.groupingBy(d -> d.getDepartment().getCode(), TreeMap::new, Collectors.toList()));
            result.put("byDepartment", byDept.entrySet().stream().map(e -> Map.of(
                    "department", e.getKey(),
                    "dealCount", e.getValue().size(),
                    "amount", sum(e.getValue()),
                    "wonAmount", sum(e.getValue().stream().filter(d -> Set.of("Won", "PO").contains(d.getDealStage()) && YearMonth.from(d.getClosedDate()).equals(YearMonth.now())).toList()),
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
