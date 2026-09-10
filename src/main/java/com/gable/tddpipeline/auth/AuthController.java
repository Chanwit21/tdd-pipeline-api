package com.gable.tddpipeline.auth;

import com.gable.tddpipeline.domain.User;
import com.gable.tddpipeline.repo.UserRepository;
import com.gable.tddpipeline.security.AppUserPrincipal;
import com.gable.tddpipeline.security.CurrentUser;
import com.gable.tddpipeline.security.JwtTokenProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req) {
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
            String token = tokenProvider.generate(principal);
            User user = userRepository.findById(principal.getId()).orElseThrow();
            user.setLastLoginAt(java.time.Instant.now());
            userRepository.save(user);
            return Map.of(
                    "token", token,
                    "expiresInMs", tokenProvider.getExpirationMs(),
                    "user", userInfo(user));
        } catch (BadCredentialsException e) {
            throw new UsernameNotFoundException("username หรือ password ไม่ถูกต้อง");
        }
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        User user = userRepository.findById(CurrentUser.get().getId()).orElseThrow();
        return userInfo(user);
    }

    private Map<String, Object> userInfo(User u) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("fullName", u.getFullName());
        m.put("role", u.getRole().name());
        m.put("departmentId", u.getDepartment() != null ? u.getDepartment().getId() : null);
        m.put("departmentCode", u.getDepartment() != null ? u.getDepartment().getCode() : null);
        m.put("departmentName", u.getDepartment() != null ? u.getDepartment().getName() : null);
        return m;
    }
}
