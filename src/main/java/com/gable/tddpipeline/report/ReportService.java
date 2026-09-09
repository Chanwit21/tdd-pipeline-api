package com.gable.tddpipeline.report;

import com.gable.tddpipeline.domain.Deal;
import com.gable.tddpipeline.masterconfig.MasterConfigService;
import com.gable.tddpipeline.repo.DealRepository;
import com.gable.tddpipeline.security.AppUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final DealRepository dealRepo;
    private final MasterConfigService masterConfig;

    private static final List<String> MONTHS =
            List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

    /* ---------- 6.1 PR by Team ---------- */
    @Transactional(readOnly = true)
    public Map<String, Object> prByTeam(Long departmentId, int year, AppUserPrincipal me) {
        List<Deal> deals = scopedDeals(departmentId, me).stream()
                .filter(d -> "PR".equals(d.getDealStatus()))
                .filter(d -> YearMonth.from(d.getClosedDate()).getYear() == year)
                .toList();
        return monthPivot(deals, "แผนก", d -> d.getDepartment().getCode(), rowLabelsDepartments());
    }

    /* ---------- 6.3 Pipeline by Team ---------- */
    @Transactional(readOnly = true)
    public Map<String, Object> pipelineByTeam(Long departmentId, int year,
                                              List<String> dealStatus, List<String> probability,
                                              List<String> dealStage, AppUserPrincipal me) {
        List<Deal> deals = scopedDeals(departmentId, me).stream()
                .filter(d -> YearMonth.from(d.getClosedDate()).getYear() == year)
                .filter(d -> isEmpty(dealStatus) || dealStatus.contains(d.getDealStatus()))
                .filter(d -> isEmpty(probability) || probability.contains(d.getProbability()))
                .filter(d -> isEmpty(dealStage) || dealStage.contains(d.getDealStage()))
                .toList();
        return monthPivot(deals, "แผนก", d -> d.getDepartment().getCode(), rowLabelsDepartments());
    }

    /* ---------- 6.2 SMT QBR ---------- */
    @Transactional(readOnly = true)
    public Map<String, Object> smtQbr(Long departmentId, int year, List<String> dealStatus, AppUserPrincipal me) {
        List<Deal> deals = scopedDeals(departmentId, me).stream()
                .filter(d -> YearMonth.from(d.getClosedDate()).getYear() == year)
                .filter(d -> isEmpty(dealStatus) || dealStatus.contains(d.getDealStatus()))
                .toList();

        List<String> rowLabels = masterConfig.probabilityValues();
        List<String> columns = masterConfig.dealStages().stream()
                .map(com.gable.tddpipeline.domain.DealStage::getName).toList();

        return pivot(deals, "Probability", Deal::getProbability, rowLabels,
                columns, Deal::getDealStage);
    }

    /* ---------- shared pivot builders ---------- */

    private Map<String, Object> monthPivot(List<Deal> deals, String rowHeader,
                                           java.util.function.Function<Deal, String> rowKey,
                                           List<String> rowLabels) {
        return pivot(deals, rowHeader, rowKey, rowLabels, MONTHS,
                d -> MONTHS.get(YearMonth.from(d.getClosedDate()).getMonthValue() - 1));
    }

    private Map<String, Object> pivot(List<Deal> deals, String rowHeader,
                                      java.util.function.Function<Deal, String> rowKey, List<String> rowLabels,
                                      List<String> columns, java.util.function.Function<Deal, String> colKey) {

        Map<String, Map<String, BigDecimal>> grid = new LinkedHashMap<>();
        for (String rl : rowLabels) grid.put(rl, newRow(columns));

        for (Deal d : deals) {
            String r = rowKey.apply(d);
            String c = colKey.apply(d);
            if (!grid.containsKey(r)) grid.put(r, newRow(columns));
            Map<String, BigDecimal> row = grid.get(r);
            if (row.containsKey(c)) {
                row.merge(c, d.getAmount(), BigDecimal::add);
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, BigDecimal> colTotals = newRow(columns);
        BigDecimal grand = BigDecimal.ZERO;

        for (var entry : grid.entrySet()) {
            Map<String, BigDecimal> row = entry.getValue();
            BigDecimal rowTotal = row.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            grand = grand.add(rowTotal);
            row.forEach((k, v) -> colTotals.merge(k, v, BigDecimal::add));
            Map<String, Object> rowOut = new LinkedHashMap<>();
            rowOut.put("label", entry.getKey());
            rowOut.put("values", row);
            rowOut.put("total", rowTotal);
            rows.add(rowOut);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rowHeader", rowHeader);
        result.put("columns", columns);
        result.put("rows", rows);
        result.put("columnTotals", colTotals);
        result.put("grandTotal", grand);
        return result;
    }

    private static Map<String, BigDecimal> newRow(List<String> columns) {
        Map<String, BigDecimal> m = new LinkedHashMap<>();
        for (String c : columns) m.put(c, BigDecimal.ZERO);
        return m;
    }

    private List<String> rowLabelsDepartments() {
        return masterConfig.departments().stream()
                .map(com.gable.tddpipeline.domain.Department::getCode).toList();
    }

    private List<Deal> scopedDeals(Long departmentId, AppUserPrincipal me) {
        Long scope = me.isAdmin() ? departmentId : me.getDepartmentId();
        return dealRepo.findAll().stream()
                .filter(d -> scope == null || Objects.equals(d.getDepartment().getId(), scope))
                .toList();
    }

    private static boolean isEmpty(List<String> l) {
        return l == null || l.isEmpty();
    }
}
