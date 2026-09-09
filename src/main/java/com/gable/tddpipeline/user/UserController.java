package com.gable.tddpipeline.user;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<Map<String, Object>> list() {
        return userService.list();
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody UserService.UserRequest req) {
        return userService.save(req);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody UserService.UserRequest req) {
        return userService.save(new UserService.UserRequest(id, req.username(), req.fullName(),
                req.password(), req.role(), req.departmentId(), req.active()));
    }

    @PatchMapping("/{id}/active")
    public Map<String, Object> setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        userService.setActive(id, Boolean.TRUE.equals(body.get("active")));
        return Map.of("ok", true);
    }
}
