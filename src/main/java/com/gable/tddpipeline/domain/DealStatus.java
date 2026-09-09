package com.gable.tddpipeline.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "deal_statuses")
@Getter
@Setter
public class DealStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
