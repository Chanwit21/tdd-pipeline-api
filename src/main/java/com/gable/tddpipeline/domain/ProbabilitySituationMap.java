package com.gable.tddpipeline.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "probability_situation_map")
@Getter
@Setter
public class ProbabilitySituationMap {
    @Id
    @Column(length = 20)
    private String probability;

    @Column(nullable = false, length = 20)
    private String situation;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
