package com.gable.tddpipeline.masterconfig;

import com.gable.tddpipeline.domain.*;
import com.gable.tddpipeline.repo.*;
import com.gable.tddpipeline.web.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class MasterConfigService {

    public static final String KEY_WON_PROB = "XREF_WON_PROBABILITY";
    public static final String KEY_PO_PROB = "XREF_PO_PROBABILITY";

    private final DepartmentRepository departmentRepo;
    private final DealTypeRepository dealTypeRepo;
    private final DealStatusRepository dealStatusRepo;
    private final DealStageRepository dealStageRepo;
    private final ProbabilitySituationMapRepository probMapRepo;
    private final RuleConfigRepository ruleConfigRepo;
    private final DealRepository dealRepo;

    /* ---------- read helpers used by DealValidator / form ---------- */

    public List<Department> departments() {
        return departmentRepo.findAllByOrderBySortOrderAsc();
    }

    public List<String> dealTypeNames() {
        return dealTypeRepo.findAllByOrderBySortOrderAsc().stream().map(DealType::getName).toList();
    }

    public List<String> dealStatusNames() {
        return dealStatusRepo.findAllByOrderBySortOrderAsc().stream().map(DealStatus::getName).toList();
    }

    public List<DealStage> dealStages() {
        return dealStageRepo.findAllByOrderBySortOrderAsc();
    }

    public List<String> stagesForStatus(String status) {
        return dealStages().stream()
                .filter(s -> s.allowedForList().contains(status))
                .map(DealStage::getName)
                .toList();
    }

    public List<ProbabilitySituationMap> probabilityMap() {
        return probMapRepo.findAllByOrderBySortOrderAsc();
    }

    public List<String> probabilityValues() {
        return probabilityMap().stream().map(ProbabilitySituationMap::getProbability).toList();
    }

    /** Situation computed from probability. Never trusts client input. */
    public String situationFor(String probability) {
        return probMapRepo.findById(probability)
                .map(ProbabilitySituationMap::getSituation)
                .orElse(null);
    }

    public String ruleValue(String key) {
        return ruleConfigRepo.findById(key).map(RuleConfig::getConfigValue).orElse(null);
    }

    /* ---------- aggregate DTO for the frontend form ---------- */

    public Map<String, Object> fullConfig() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("departments", departments().stream().map(d -> Map.of(
                "id", d.getId(), "code", d.getCode(), "name", d.getName(),
                "defaultOwner", Optional.ofNullable(d.getDefaultOwner()).orElse(""))).toList());
        m.put("dealTypes", dealTypeNames());
        m.put("dealStatuses", dealStatusNames());
        m.put("dealStages", dealStages().stream().map(s -> Map.of(
                "name", s.getName(), "allowedFor", s.allowedForList())).toList());
        m.put("probabilities", probabilityMap().stream().map(p -> Map.of(
                "probability", p.getProbability(), "situation", p.getSituation())).toList());
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("wonProbability", Optional.ofNullable(ruleValue(KEY_WON_PROB)).orElse("75% - 98%"));
        rules.put("poProbability", Optional.ofNullable(ruleValue(KEY_PO_PROB)).orElse("99% - 100%"));
        m.put("rules", rules);
        return m;
    }

    /* ---------- CRUD (Admin) ---------- */

    @Transactional
    public Department saveDepartment(Department input) {
        Department d = input.getId() != null
                ? departmentRepo.findById(input.getId()).orElseThrow()
                : new Department();
        if (d.getId() == null && departmentRepo.existsByCodeIgnoreCase(input.getCode())) {
            throw new BusinessException("DUP-CODE", "รหัสแผนกนี้มีอยู่แล้ว: " + input.getCode());
        }
        d.setCode(input.getCode());
        d.setName(input.getName());
        d.setDefaultOwner(input.getDefaultOwner());
        d.setSortOrder(input.getSortOrder());
        return departmentRepo.save(d);
    }

    @Transactional
    public DealStage saveStage(DealStage input) {
        DealStage s = input.getId() != null
                ? dealStageRepo.findById(input.getId()).orElseThrow()
                : new DealStage();
        s.setName(input.getName());
        s.setAllowedFor(input.getAllowedFor());
        s.setSortOrder(input.getSortOrder());
        return dealStageRepo.save(s);
    }

    @Transactional
    public void deleteStage(Integer id) {
        DealStage s = dealStageRepo.findById(id).orElseThrow();
        boolean inUse = dealRepo.count((root, q, cb) -> cb.equal(root.get("dealStage"), s.getName())) > 0;
        if (inUse) {
            throw new BusinessException("XREF-INUSE-DELETE",
                    "ลบไม่ได้: ยังมี deal ที่ใช้ Deal Stage นี้อยู่");
        }
        dealStageRepo.delete(s);
    }

    @Transactional
    public void updateRule(String key, String value) {
        RuleConfig rc = ruleConfigRepo.findById(key)
                .orElseThrow(() -> new BusinessException("RULE-KEY", "ไม่พบ config: " + key));
        rc.setConfigValue(value);
        ruleConfigRepo.save(rc);
    }
}
