package com.gable.tddpipeline.repo;

import com.gable.tddpipeline.domain.DealNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DealNoteRepository extends JpaRepository<DealNote, Long> {
    List<DealNote> findByDealIdOrderByCreatedAtDesc(Long dealId);
}
