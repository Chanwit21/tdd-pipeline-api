package com.gable.tddpipeline.dashboard;

import com.gable.tddpipeline.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/api/dashboard/summary")
    public Map<String, Object> summary(
            @RequestParam(required = false) List<Long> departmentId,
            @RequestParam(required = false) List<String> probability,
            @RequestParam(required = false) List<String> dealStatus,
            @RequestParam(required = false) Integer createdYear,
            @RequestParam(required = false) Integer quarter,
            @RequestParam(required = false) String from,   // "yyyy-MM"
            @RequestParam(required = false) String to) {   // "yyyy-MM"
        return service.summary(departmentId, from, to, probability, dealStatus, createdYear, quarter, CurrentUser.get());
    }
}
