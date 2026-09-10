package com.gable.tddpipeline.user;

import com.gable.tddpipeline.domain.Department;
import com.gable.tddpipeline.domain.Role;
import com.gable.tddpipeline.domain.User;
import com.gable.tddpipeline.repo.DepartmentRepository;
import com.gable.tddpipeline.repo.UserRepository;
import com.gable.tddpipeline.web.BusinessException;
import com.gable.tddpipeline.web.DealValidationException;
import com.gable.tddpipeline.web.FieldError;
import com.gable.tddpipeline.web.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final DepartmentRepository departmentRepo;
    private final PasswordEncoder passwordEncoder;

    public record UserRequest(Long id, String username, String fullName, String password,
                              String role, Long departmentId, Boolean active) {}

    @Transactional(readOnly = true)
    public List<Map<String, Object>> list() {
        return userRepo.findAllByOrderByUsernameAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public Map<String, Object> save(UserRequest req) {
        boolean creating = req.id() == null;
        User user = creating ? new User()
                : userRepo.findById(req.id()).orElseThrow(() -> new NotFoundException("ไม่พบผู้ใช้"));

        List<FieldError> errors = new ArrayList<>();
        if (isBlank(req.username())) errors.add(new FieldError("username", "REQ-USERNAME", "กรุณากรอก Username"));
        if (isBlank(req.fullName())) errors.add(new FieldError("fullName", "REQ-FULLNAME", "กรุณากรอกชื่อ-นามสกุล"));

        Role role = parseRole(req.role());
        if (role == null) errors.add(new FieldError("role", "REQ-ROLE", "กรุณาเลือก Role"));

        if (creating && isBlank(req.password())) {
            errors.add(new FieldError("password", "REQ-PASSWORD", "กรุณากรอกรหัสผ่านอย่างน้อย 8 ตัวอักษร"));
        } else if (!isBlank(req.password()) && req.password().length() < 8) {
            errors.add(new FieldError("password", "FMT-PASSWORD", "กรุณากรอกรหัสผ่านอย่างน้อย 8 ตัวอักษร"));
        }

        if (!isBlank(req.username())) {
            userRepo.findByUsername(req.username().trim())
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(x -> errors.add(new FieldError("username", "DUP-USERNAME", "Username นี้มีผู้ใช้แล้ว")));
        }

        Department dept = null;
        if (role == Role.MANAGER) {
            if (req.departmentId() == null) {
                errors.add(new FieldError("departmentId", "REQ-USERDEPT", "กรุณาเลือกแผนก"));
            } else {
                dept = departmentRepo.findById(req.departmentId())
                        .orElseThrow(() -> new NotFoundException("ไม่พบแผนก"));
                boolean active = req.active() == null || req.active();
                if (active) {
                    userRepo.findByDepartmentIdAndRoleAndActiveTrue(dept.getId(), Role.MANAGER)
                            .filter(existing -> !existing.getId().equals(user.getId()))
                            .ifPresent(existing -> errors.add(new FieldError("departmentId",
                                    "XREF-DEPT-ONEMANAGER",
                                    "แผนกนี้มี Manager อยู่แล้ว (" + existing.getFullName()
                                            + ") — ปิดใช้งานคนเดิมก่อนหรือเปลี่ยนแผนก")));
                }
            }
        }

        if (!errors.isEmpty()) throw new DealValidationException(errors);

        user.setUsername(req.username().trim());
        user.setFullName(req.fullName().trim());
        user.setRole(role);
        user.setDepartment(role == Role.MANAGER ? dept : null);
        user.setActive(req.active() == null || req.active());
        if (!isBlank(req.password())) {
            user.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        userRepo.save(user);
        return toDto(user);
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        User user = userRepo.findById(id).orElseThrow(() -> new NotFoundException("ไม่พบผู้ใช้"));
        user.setActive(active);
        userRepo.save(user);
    }

    private Map<String, Object> toDto(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("fullName", u.getFullName());
        m.put("role", u.getRole().name());
        m.put("departmentId", u.getDepartment() != null ? u.getDepartment().getId() : null);
        m.put("departmentCode", u.getDepartment() != null ? u.getDepartment().getCode() : null);
        m.put("active", u.isActive());
        m.put("lastLoginAt", u.getLastLoginAt());
        return m;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static Role parseRole(String s) {
        try {
            return s == null ? null : Role.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
