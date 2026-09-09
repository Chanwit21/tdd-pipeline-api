package com.gable.tddpipeline.config;

import com.gable.tddpipeline.domain.Role;
import com.gable.tddpipeline.domain.User;
import com.gable.tddpipeline.repo.DepartmentRepository;
import com.gable.tddpipeline.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds a default ADMIN and one MANAGER on first boot (idempotent). */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setFullName("System Administrator");
            admin.setRole(Role.ADMIN);
            admin.setPasswordHash(passwordEncoder.encode("admin1234"));
            userRepository.save(admin);
            log.info("Seeded default ADMIN user 'admin' / 'admin1234'");
        }

        if (userRepository.findByUsername("manager.irm").isEmpty()) {
            departmentRepository.findByCode("IRM").ifPresent(dept -> {
                User m = new User();
                m.setUsername("manager.irm");
                m.setFullName("Manager IRM");
                m.setRole(Role.MANAGER);
                m.setDepartment(dept);
                m.setPasswordHash(passwordEncoder.encode("manager1234"));
                userRepository.save(m);
                log.info("Seeded MANAGER user 'manager.irm' / 'manager1234' (dept IRM)");
            });
        }
    }
}
