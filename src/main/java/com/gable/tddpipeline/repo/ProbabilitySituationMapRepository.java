package com.gable.tddpipeline.repo;

import com.gable.tddpipeline.domain.ProbabilitySituationMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProbabilitySituationMapRepository extends JpaRepository<ProbabilitySituationMap, String> {
    List<ProbabilitySituationMap> findAllByOrderBySortOrderAsc();
}
