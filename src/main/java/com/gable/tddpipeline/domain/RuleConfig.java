package com.gable.tddpipeline.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "rule_config")
@Getter
@Setter
public class RuleConfig {
    @Id
    @Column(name = "config_key", length = 60)
    private String configKey;

    @Column(name = "config_value", nullable = false)
    private String configValue;

    @Column(length = 255)
    private String description;
}
