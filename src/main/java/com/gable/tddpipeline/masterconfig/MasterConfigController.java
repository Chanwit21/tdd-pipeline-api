package com.gable.tddpipeline.masterconfig;

import com.gable.tddpipeline.domain.Department;
import com.gable.tddpipeline.domain.DealStage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MasterConfigController {

    private final MasterConfigService service;

    /** Consumed by the deal form — available to any authenticated user. */
    @GetMapping("/api/master-config")
    public Map<String, Object> full() {
        return service.fullConfig();
    }

    /* -------- Admin CRUD (gated by /api/admin/** rule in SecurityConfig) -------- */

    @PostMapping("/api/admin/master-config/departments")
    public Department saveDepartment(@RequestBody Department body) {
        return service.saveDepartment(body);
    }

    @PostMapping("/api/admin/master-config/stages")
    public DealStage saveStage(@RequestBody DealStage body) {
        return service.saveStage(body);
    }

    @DeleteMapping("/api/admin/master-config/stages/{id}")
    public Map<String, Object> deleteStage(@PathVariable Integer id) {
        service.deleteStage(id);
        return Map.of("deleted", true);
    }

    public record RuleUpdate(String key, String value) {}

    @PostMapping("/api/admin/master-config/types")
    public com.gable.tddpipeline.domain.DealType saveType(@RequestBody com.gable.tddpipeline.domain.DealType body) { return service.saveType(body); }

    @PostMapping("/api/admin/master-config/statuses")
    public com.gable.tddpipeline.domain.DealStatus saveStatus(@RequestBody com.gable.tddpipeline.domain.DealStatus body) { return service.saveStatus(body); }

    @DeleteMapping("/api/admin/master-config/{kind:types|statuses}/{id}")
    public Map<String, Object> deleteNamed(@PathVariable String kind, @PathVariable Integer id) { service.deleteNamed(kind, id); return Map.of("deleted", true); }

    @PutMapping("/api/admin/master-config/rules")
    public Map<String, Object> updateRule(@RequestBody RuleUpdate body) {
        service.updateRule(body.key(), body.value());
        return Map.of("updated", true);
    }
}
