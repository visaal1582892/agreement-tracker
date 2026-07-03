package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "agreement_jbp_configurations", indexes = {
        @Index(name = "idx_jbp_cfg_version", columnList = "agreement_version_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementJbpConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_version_id", nullable = false)
    private AgreementVersion agreementVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 50)
    private PayoutFrequency frequency;

    @Column(name = "slab_count", nullable = false)
    private Integer slabCount;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "agreement_jbp_config_periods",
            joinColumns = @JoinColumn(name = "jbp_configuration_id"),
            inverseJoinColumns = @JoinColumn(name = "time_period_id")
    )
    @Builder.Default
    private Set<AgreementTimePeriod> selectedPeriods = new LinkedHashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
