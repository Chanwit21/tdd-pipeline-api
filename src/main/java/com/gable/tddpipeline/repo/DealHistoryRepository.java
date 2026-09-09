package com.gable.tddpipeline.repo;

import com.gable.tddpipeline.domain.DealHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DealHistoryRepository extends JpaRepository<DealHistory, Long> {
    List<DealHistory> findByDealIdOrderByChangedAtDesc(Long dealId);
}
