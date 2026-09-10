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
        m.put("typeOptions", dealTypeRepo.findAllByOrderBySortOrderAsc());
        m.put("statusOptions", dealStatusRepo.findAllByOrderBySortOrderAsc());
        m.put("dealStages", dealStages().stream().map(s -> Map.of(
                "id", s.getId(), "name", s.getName(), "allowedFor", s.allowedForList())).toList());
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
        requireName(input.getCode(), 20);
        requireName(input.getName(), 100);
        if (input.getDefaultOwner() != null && input.getDefaultOwner().length() > 100) throw new BusinessException("OWNER", "Owner must not exceed 100 characters");
        input.setCode(input.getCode().trim()); input.setName(input.getName().trim());
        Department d = input.getId() != null
                ? departmentRepo.findById(input.getId()).orElseThrow()
                : new Department();
        if (departmentRepo.findAll().stream().anyMatch(x -> x.getCode().equalsIgnoreCase(input.getCode()) && !Objects.equals(x.getId(), input.getId()))) {
            throw new BusinessException("DUP-CODE", "รหัสแผนกนี้มีอยู่แล้ว: " + input.getCode());
        }
        d.setCode(input.getCode());
        d.setName(input.getName());
        d.setDefaultOwner(input.getDefaultOwner());
        if (input.getId() == null) d.setSortOrder(input.getSortOrder());
        return departmentRepo.save(d);
    }

    @Transactional
    public DealStage saveStage(DealStage input) {
        requireName(input.getName(), 80); input.setName(input.getName().trim());
        if (input.allowedForList().isEmpty() || !dealStatusNames().containsAll(input.allowedForList())) throw new BusinessException("STAGE-STATUS", "Select at least one valid Deal Status");
        if (dealStages().stream().anyMatch(x -> x.getName().equalsIgnoreCase(input.getName()) && !Objects.equals(x.getId(), input.getId()))) throw new BusinessException("DUP-STAGE", "Deal Stage already exists");
        DealStage s = input.getId() != null
                ? dealStageRepo.findById(input.getId()).orElseThrow()
                : new DealStage();
        if (s.getId() != null) {
            assertRenameAllowed("dealStage", s.getName(), input.getName(), Set.of("Won", "PO", "Lost", "Cancelled", "On Hold"));
            boolean incompatible = dealRepo.count((r,q,cb) -> cb.and(cb.equal(r.get("dealStage"), s.getName()), cb.not(r.get("dealStatus").in(input.allowedForList())))) > 0;
            if (incompatible) throw new BusinessException("STAGE-INUSE", "Cannot remove a status mapping used by existing deals");
        }
        s.setName(input.getName());
        s.setAllowedFor(input.getAllowedFor());
        if (input.getId() == null) s.setSortOrder(input.getSortOrder());
        return dealStageRepo.save(s);
    }

    @Transactional
    public void deleteStage(Integer id) {
        DealStage s = dealStageRepo.findById(id).orElseThrow();
        if (Set.of("Won", "PO", "Lost", "Cancelled", "On Hold").contains(s.getName())) throw new BusinessException("SYSTEM-STAGE", "Cannot delete a system Deal Stage");
        boolean inUse = dealRepo.count((root, q, cb) -> cb.equal(root.get("dealStage"), s.getName())) > 0;
        if (inUse) {
            throw new BusinessException("XREF-INUSE-DELETE",
                    "ลบไม่ได้: ยังมี deal ที่ใช้ Deal Stage นี้อยู่");
        }
        dealStageRepo.delete(s);
    }

    @Transactional
    public void updateRule(String key, String value) {
        if (!Set.of(KEY_WON_PROB, KEY_PO_PROB).contains(key) || !probabilityValues().contains(value)) throw new BusinessException("RULE-VALUE", "Invalid rule or Probability");
        RuleConfig rc = ruleConfigRepo.findById(key)
                .orElseThrow(() -> new BusinessException("RULE-KEY", "ไม่พบ config: " + key));
        rc.setConfigValue(value);
        ruleConfigRepo.save(rc);
    }

    private void requireName(String value, int max) {
        if (value == null || value.isBlank() || value.trim().length() > max) throw new BusinessException("MASTER-NAME", "Name is required (maximum " + max + " characters)");
    }

    private void assertRenameAllowed(String field, String oldName, String name, Set<String> reserved) {
        if (Objects.equals(oldName, name)) return;
        if (reserved.contains(oldName) || dealRepo.count((r,q,cb) -> cb.equal(r.get(field), oldName)) > 0)
            throw new BusinessException("MASTER-INUSE", "Cannot rename a system value or a value referenced by existing deals");
    }

    @Transactional
    public DealType saveType(DealType input) {
        requireName(input.getName(), 50); String name = input.getName().trim();
        if (dealTypeRepo.findAll().stream().anyMatch(x -> x.getName().equalsIgnoreCase(name) && !Objects.equals(x.getId(), input.getId()))) throw new BusinessException("DUP-TYPE", "Deal Type already exists");
        DealType item = input.getId() == null ? new DealType() : dealTypeRepo.findById(input.getId()).orElseThrow();
        if (item.getId() != null) assertRenameAllowed("dealType", item.getName(), name, Set.of());
        item.setName(name); if (input.getId() == null) item.setSortOrder(input.getSortOrder()); return dealTypeRepo.save(item);
    }

    @Transactional
    public DealStatus saveStatus(DealStatus input) {
        requireName(input.getName(), 50); String name = input.getName().trim();
        if (dealStatusRepo.findAll().stream().anyMatch(x -> x.getName().equalsIgnoreCase(name) && !Objects.equals(x.getId(), input.getId()))) throw new BusinessException("DUP-STATUS", "Deal Status already exists");
        DealStatus item = input.getId() == null ? new DealStatus() : dealStatusRepo.findById(input.getId()).orElseThrow();
        if (item.getId() != null) {
            assertRenameAllowed("dealStatus", item.getName(), name, Set.of("Follow Up", "PR", "Inactive"));
            if (!item.getName().equals(name) && dealStages().stream().anyMatch(s -> s.allowedForList().contains(item.getName()))) throw new BusinessException("STATUS-INUSE", "Status is referenced by Deal Stage mappings");
        }
        item.setName(name); if (input.getId() == null) item.setSortOrder(input.getSortOrder()); return dealStatusRepo.save(item);
    }

    @Transactional
    public void deleteNamed(String kind, Integer id) {
        if ("types".equals(kind)) {
            DealType item = dealTypeRepo.findById(id).orElseThrow();
            assertRenameAllowed("dealType", item.getName(), "", Set.of()); dealTypeRepo.delete(item);
        } else if ("statuses".equals(kind)) {
            DealStatus item = dealStatusRepo.findById(id).orElseThrow();
            assertRenameAllowed("dealStatus", item.getName(), "", Set.of("Follow Up", "PR", "Inactive"));
            if (dealStages().stream().anyMatch(s -> s.allowedForList().contains(item.getName()))) throw new BusinessException("STATUS-INUSE", "Status is referenced by Deal Stage mappings");
            dealStatusRepo.delete(item);
        } else throw new BusinessException("MASTER-KIND", "Invalid master category");
    }
}

