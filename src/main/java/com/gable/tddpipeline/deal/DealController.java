package com.gable.tddpipeline.deal;

import com.gable.tddpipeline.deal.dto.DealQuery;
import com.gable.tddpipeline.deal.dto.DealRequest;
import com.gable.tddpipeline.deal.dto.DealResponse;
import com.gable.tddpipeline.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deals")
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) List<Long> departmentId,
            @RequestParam(required = false) List<String> dealStatus,
            @RequestParam(required = false) List<String> dealStage,
            @RequestParam(required = false) String closedFrom,
            @RequestParam(required = false) String closedTo,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean overdueOnly,
            @RequestParam(required = false) List<String> probability,
            @RequestParam(required = false) Integer createdYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        var query = new DealQuery(departmentId, dealStatus, dealStage, closedFrom, closedTo, search, overdueOnly, probability, createdYear);
        var pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 200)), Sort.by("recordId"));
        Page<DealResponse> result = dealService.list(query, pageable, CurrentUser.get());
        return Map.of(
                "content", result.getContent(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages());
    }

    @GetMapping("/{id}")
    public DealResponse get(@PathVariable Long id) {
        return dealService.get(id, CurrentUser.get());
    }

    @PostMapping
    public DealResponse create(@RequestBody DealRequest req) {
        return dealService.create(req, CurrentUser.get());
    }

    @PutMapping("/{id}")
    public DealResponse update(@PathVariable Long id, @RequestBody DealRequest req) {
        return dealService.update(id, req, CurrentUser.get());
    }

    public record NoteRequest(String text) {}

    @PostMapping("/{id}/notes")
    public DealResponse addNote(@PathVariable Long id, @RequestBody NoteRequest req) {
        return dealService.addNote(id, req.text(), CurrentUser.get());
    }
}
