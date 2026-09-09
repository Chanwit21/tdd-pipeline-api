package com.gable.tddpipeline.repo;

import com.gable.tddpipeline.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByCode(String code);
    List<Department> findAllByOrderBySortOrderAsc();
    boolean existsByCodeIgnoreCase(String code);
}
