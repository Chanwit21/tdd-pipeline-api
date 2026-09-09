package com.gable.tddpipeline.repo;

import com.gable.tddpipeline.domain.Deal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DealRepository extends JpaRepository<Deal, Long>, JpaSpecificationExecutor<Deal> {
    Optional<Deal> findByRecordId(String recordId);

    @Query(value = "SELECT nextval('deal_record_seq')", nativeQuery = true)
    long nextRecordSeq();
}
