package com.gable.tddpipeline.deal;

import com.gable.tddpipeline.deal.dto.DealQuery;
import com.gable.tddpipeline.deal.dto.DealRequest;
import com.gable.tddpipeline.deal.dto.DealResponse;
import com.gable.tddpipeline.domain.*;
import com.gable.tddpipeline.masterconfig.MasterConfigService;
import com.gable.tddpipeline.repo.*;
import com.gable.tddpipeline.security.AppUserPrincipal;
import com.gable.tddpipeline.web.BusinessException;
import com.gable.tddpipeline.web.DealValidationException;
import com.gable.tddpipeline.web.FieldError;
import com.gable.tddpipeline.web.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.*;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class DealService {

    private final DealRepository dealRepo;
    private final DealNoteRepository noteRepo;
    private final DealHistoryRepository historyRepo;
    private final DepartmentRepository departmentRepo;
    private final UserRepository userRepo;
    private final MasterConfigService masterConfig;

    private final DealValidator validator = new DealValidator();

    /* ---------------- queries ---------------- */

    @Transactional(readOnly = true)
    public Page<DealResponse> list(DealQuery query, Pageable pageable, AppUserPrincipal me) {
        Long forcedDept = me.isAdmin() ? null : Objects.requireNonNullElse(me.getDepartmentId(), -1L);
        var spec = DealSpecifications.build(query, forcedDept);
        Page<Deal> page = dealRepo.findAll(spec, pageable);
        String wonProb = Objects.requireNonNullElse(masterConfig.ruleValue(MasterConfigService.KEY_WON_PROB), "75% - 98%");
        String poProb = Objects.requireNonNullElse(masterConfig.ruleValue(MasterConfigService.KEY_PO_PROB), "99% - 100%");
        return page.map(d -> DealMapper.toResponse(d, List.of(), List.of(), Map.of(), wonProb, poProb));
    }

    @Transactional(readOnly = true)
    public DealResponse get(Long id, AppUserPrincipal me) {
        Deal deal = dealRepo.findById(id).orElseThrow(() -> new NotFoundException("ไม่พบ deal id " + id));
        assertScope(deal, me);
        var notes = noteRepo.findByDealIdOrderByCreatedAtDesc(id);
        var history = historyRepo.findByDealIdOrderByChangedAtDesc(id);
        Map<Long, String> userNames = resolveUserNames(notes, history);
        String wonProb = Objects.requireNonNullElse(masterConfig.ruleValue(MasterConfigService.KEY_WON_PROB), "75% - 98%");
        String poProb = Objects.requireNonNullElse(masterConfig.ruleValue(MasterConfigService.KEY_PO_PROB), "99% - 100%");
        return DealMapper.toResponse(deal, notes, history, userNames, wonProb, poProb);
    }

    /* ---------------- mutations ---------------- */

    @Transactional
    public DealResponse create(DealRequest req, AppUserPrincipal me) {
        Long departmentId = resolveDepartmentForWrite(req.departmentId(), me);
        Department dept = validated(req, departmentId);

        Deal deal = new Deal();
        deal.setRecordId(nextRecordId());
        deal.setDepartment(dept);
        deal.setCreatedBy(me.getId());
        applyRequest(deal, req, dept);
        deal.setLegacyMigrated(false);
        deal.setUpdatedBy(me.getId());
        dealRepo.save(deal);

        return get(deal.getId(), me);
    }

    @Transactional
    public DealResponse update(Long id, DealRequest req, AppUserPrincipal me) {
        Deal deal = dealRepo.findById(id).orElseThrow(() -> new NotFoundException("ไม่พบ deal id " + id));
        assertScope(deal, me);

        Long departmentId = resolveDepartmentForWrite(req.departmentId(), me);
        Department dept = validated(req, departmentId);

        Map<String, String> before = snapshot(deal);
        applyRequest(deal, req, dept);
        // first save after migration must comply -> flip the flag off
        deal.setLegacyMigrated(false);
        deal.setMigrationRemark(null);
        deal.setUpdatedBy(me.getId());
        dealRepo.save(deal);

        writeHistory(deal, before, snapshot(deal), me.getId());
        return get(deal.getId(), me);
    }

    @Transactional
    public DealResponse addNote(Long dealId, String text, AppUserPrincipal me) {
        Deal deal = dealRepo.findById(dealId).orElseThrow(() -> new NotFoundException("ไม่พบ deal id " + dealId));
        assertScope(deal, me);
        if (DealValidator.isBlank(text)) {
            throw new BusinessException("REQ-NOTE", "กรุณากรอกข้อความ Note");
        }
        DealNote note = new DealNote();
        note.setDealId(dealId);
        note.setNoteText(text.trim());
        note.setCreatedBy(me.getId());
        noteRepo.save(note);
        return get(dealId, me);
    }

    /* ---------------- helpers ---------------- */

    private Department validated(DealRequest req, Long departmentId) {
        boolean deptExists = departmentId != null && departmentRepo.existsById(departmentId);

        DealValidator.Context ctx = new DealValidator.Context(
                new HashSet<>(masterConfig.dealTypeNames()),
                new HashSet<>(masterConfig.dealStatusNames()),
                new HashSet<>(masterConfig.probabilityValues()),
                new HashSet<>(req.dealStatus() != null ? masterConfig.stagesForStatus(req.dealStatus()) : List.of()),
                deptExists,
                Objects.requireNonNullElse(masterConfig.ruleValue(MasterConfigService.KEY_WON_PROB), "75% - 98%"),
                Objects.requireNonNullElse(masterConfig.ruleValue(MasterConfigService.KEY_PO_PROB), "99% - 100%"));

        List<FieldError> errors = validator.validate(req, ctx);
        if (!errors.isEmpty()) {
            throw new DealValidationException(errors);
        }
        return departmentRepo.findById(departmentId).orElseThrow();
    }

    /** Situation + Deal Owner are computed server-side; client values ignored. */
    private void applyRequest(Deal deal, DealRequest req, Department dept) {
        deal.setDepartment(dept);
        deal.setDealOwner(Objects.toString(dept.getDefaultOwner(), dept.getCode()));  // snapshot
        deal.setCustomer(req.customer().trim());
        deal.setDealName(req.dealName().trim());
        deal.setDealType(req.dealType());
        deal.setDealStatus(req.dealStatus());
        deal.setDealStage(req.dealStage());
        deal.setProbability(req.probability());
        deal.setSituation(masterConfig.situationFor(req.probability()));
        deal.setClosedDate(DealValidator.parseYearMonth(req.closedDate()).atDay(1));
        deal.setAmount(DealValidator.parseAmount(req.amount()));
        deal.setProjectCode(trimToNull(req.projectCode()));
        deal.setCostSheetNo(trimToNull(req.costSheetNo()));
        deal.setCreatedDate(DealValidator.parseDate(req.createdDate()));
    }

    private Long resolveDepartmentForWrite(Long requested, AppUserPrincipal me) {
        // Manager is locked to their own department regardless of payload
        return me.isAdmin() ? requested : me.getDepartmentId();
    }

    private void assertScope(Deal deal, AppUserPrincipal me) {
        if (!me.isAdmin() && !Objects.equals(deal.getDepartment().getId(), me.getDepartmentId())) {
            throw new AccessDeniedException("deal นี้อยู่นอกแผนกของคุณ");
        }
    }

    private String nextRecordId() {
        return String.format("TDD-%06d", dealRepo.nextRecordSeq());
    }

    private Map<Long, String> resolveUserNames(List<DealNote> notes, List<DealHistory> history) {
        Set<Long> ids = new HashSet<>();
        notes.forEach(n -> ids.add(n.getCreatedBy()));
        history.forEach(h -> ids.add(h.getChangedBy()));
        Map<Long, String> names = new HashMap<>();
        userRepo.findAllById(ids).forEach(u -> names.put(u.getId(), u.getFullName()));
        return names;
    }

    private static final Map<String, Function<Deal, String>> TRACKED = new LinkedHashMap<>();
    static {
        TRACKED.put("department", d -> d.getDepartment().getCode());
        TRACKED.put("customer", Deal::getCustomer);
        TRACKED.put("dealName", Deal::getDealName);
        TRACKED.put("dealType", Deal::getDealType);
        TRACKED.put("dealStatus", Deal::getDealStatus);
        TRACKED.put("dealStage", Deal::getDealStage);
        TRACKED.put("probability", Deal::getProbability);
        TRACKED.put("situation", Deal::getSituation);
        TRACKED.put("closedDate", d -> String.valueOf(d.getClosedDate() != null ? YearMonth.from(d.getClosedDate()) : null));
        TRACKED.put("amount", d -> String.valueOf(d.getAmount()));
        TRACKED.put("projectCode", Deal::getProjectCode);
        TRACKED.put("costSheetNo", Deal::getCostSheetNo);
        TRACKED.put("createdDate", d -> String.valueOf(d.getCreatedDate()));
    }

    private Map<String, String> snapshot(Deal d) {
        Map<String, String> m = new LinkedHashMap<>();
        TRACKED.forEach((k, fn) -> m.put(k, fn.apply(d)));
        return m;
    }

    private void writeHistory(Deal deal, Map<String, String> before, Map<String, String> after, Long userId) {
        before.forEach((field, oldVal) -> {
            String newVal = after.get(field);
            if (!Objects.equals(oldVal, newVal)) {
                DealHistory h = new DealHistory();
                h.setDealId(deal.getId());
                h.setChangedField(field);
                h.setOldValue(oldVal);
                h.setNewValue(newVal);
                h.setChangedBy(userId);
                historyRepo.save(h);
            }
        });
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
