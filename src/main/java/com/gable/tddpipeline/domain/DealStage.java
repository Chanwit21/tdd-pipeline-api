package com.gable.tddpipeline.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "deal_stages")
@Getter
@Setter
public class DealStage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    /** Deal Status names this stage is valid under (PostgreSQL {@code text[]}). */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "allowed_for", nullable = false, columnDefinition = "text[]")
    private String[] allowedFor = new String[0];

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public List<String> allowedForList() {
        return allowedFor == null ? List.of() : Arrays.asList(allowedFor);
    }
}
