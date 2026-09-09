package com.gable.tddpipeline.repo;

import com.gable.tddpipeline.domain.DealStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DealStatusRepository extends JpaRepository<DealStatus, Integer> {
    List<DealStatus> findAllByOrderBySortOrderAsc();
    boolean existsByNameIgnoreCase(String name);
}
