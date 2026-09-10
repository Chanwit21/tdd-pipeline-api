package com.gable.tddpipeline.report;

import com.gable.tddpipeline.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService service;

    @GetMapping("/api/reports/pr-by-team")
    public Map<String, Object> prByTeam(
            @RequestParam(required = false) List<Long> departmentId,
            @RequestParam(required = false) Integer year) {
        return service.prByTeam(departmentId, resolveYear(year), CurrentUser.get());
    }

    @GetMapping("/api/reports/smt-qbr")
    public Map<String, Object> smtQbr(
            @RequestParam(required = false) List<Long> departmentId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) List<String> dealStatus) {
        return service.smtQbr(departmentId, resolveYear(year), dealStatus, CurrentUser.get());
    }

    @GetMapping("/api/reports/pipeline-by-team")
    public Map<String, Object> pipelineByTeam(
            @RequestParam(required = false) List<Long> departmentId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) List<String> dealStatus,
            @RequestParam(required = false) List<String> probability,
            @RequestParam(required = false) List<String> dealStage) {
        return service.pipelineByTeam(departmentId, resolveYear(year), dealStatus, probability, dealStage,
                CurrentUser.get());
    }

    private int resolveYear(Integer year) {
        return year != null ? year : Year.now().getValue();
    }
}
