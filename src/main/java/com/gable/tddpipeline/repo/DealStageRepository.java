package com.gable.tddpipeline.repo;

import com.gable.tddpipeline.domain.DealStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DealStageRepository extends JpaRepository<DealStage, Integer> {
    List<DealStage> findAllByOrderBySortOrderAsc();
    Optional<DealStage> findByName(String name);
    boolean existsByNameIgnoreCase(String name);
}
