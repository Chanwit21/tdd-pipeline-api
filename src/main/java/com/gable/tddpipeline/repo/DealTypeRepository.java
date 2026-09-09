package com.gable.tddpipeline.repo;

import com.gable.tddpipeline.domain.DealType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DealTypeRepository extends JpaRepository<DealType, Integer> {
    List<DealType> findAllByOrderBySortOrderAsc();
    boolean existsByNameIgnoreCase(String name);
}
