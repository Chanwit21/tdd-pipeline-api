package com.gable.tddpipeline.repo;

import com.gable.tddpipeline.domain.RuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleConfigRepository extends JpaRepository<RuleConfig, String> {
}
