package com.gable.tddpipeline.dashboard;

import com.gable.tddpipeline.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/api/dashboard/summary")
    public Map<String, Object> summary(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String from,   // "yyyy-MM"
            @RequestParam(required = false) String to) {   // "yyyy-MM"
        return service.summary(departmentId, from, to, CurrentUser.get());
    }
}
