package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.enums.JbpValueType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agreement_jbp_commercial_periods", indexes = {
        @Index(name = "idx_jbp_cp_fast_lookup", columnList = "agreement_version_id, time_period_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_jbp_cell_coordinate", columnNames = {
                "agreement_version_id", "jbp_configuration_id", "slab_tier_number", "time_period_id"
        })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementJbpCommercialPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_version_id", nullable = false)
    private AgreementVersion agreementVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jbp_configuration_id", nullable = false)
    private AgreementJbpConfiguration jbpConfiguration;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private JbpValueType targetType;

    @Column(name = "target", nullable = false, precision = 15, scale = 2)
    private BigDecimal target;

    @Column(name = "qualifier_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal qualifierPercent = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_type", length = 20)
    private JbpValueType payoutType;

    @Column(name = "payout", precision = 15, scale = 2)
    private BigDecimal payout;

    @Column(name = "max_purchase", precision = 15, scale = 2)
    private BigDecimal maxPurchase;

    @Column(name = "max_payout", precision = 15, scale = 2)
    private BigDecimal maxPayout;

    @Column(name = "slab_tier_number", nullable = false)
    private Integer slabTierNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_period_id", nullable = false)
    private AgreementTimePeriod timePeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_time_period_id")
    private AgreementTimePeriod parentTimePeriod;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (qualifierPercent == null) {
            qualifierPercent = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
