package com.gable.tddpipeline.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "deals")
@Getter
@Setter
public class Deal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false, unique = true, length = 20)
    private String recordId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "deal_owner", nullable = false, length = 100)
    private String dealOwner;

    @Column(nullable = false, length = 255)
    private String customer;

    @Column(name = "deal_name", nullable = false, columnDefinition = "text")
    private String dealName;

    @Column(name = "deal_type", nullable = false, length = 50)
    private String dealType;

    @Column(name = "deal_status", nullable = false, length = 50)
    private String dealStatus;

    @Column(name = "deal_stage", nullable = false, length = 80)
    private String dealStage;

    @Column(nullable = false, length = 20)
    private String probability;

    @Column(nullable = false, length = 20)
    private String situation;

    @Column(name = "closed_date", nullable = false)
    private LocalDate closedDate;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "project_code", length = 50)
    private String projectCode;

    @Column(name = "cost_sheet_no", length = 50)
    private String costSheetNo;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    @Column(name = "is_legacy_migrated", nullable = false)
    private boolean legacyMigrated = false;

    @Column(name = "migration_remark", columnDefinition = "text")
    private String migrationRemark;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
