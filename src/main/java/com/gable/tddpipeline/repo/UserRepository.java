package com.gable.tddpipeline.repo;

import com.gable.tddpipeline.domain.Role;
import com.gable.tddpipeline.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
    Optional<User> findByDepartmentIdAndRoleAndActiveTrue(Long departmentId, Role role);
    List<User> findAllByOrderByUsernameAsc();
}
