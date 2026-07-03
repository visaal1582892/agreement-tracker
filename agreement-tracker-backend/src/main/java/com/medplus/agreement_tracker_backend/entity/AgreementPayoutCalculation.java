package com.medplus.agreement_tracker_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agreement_payout_calculations", indexes = {
        @Index(name = "idx_payout_calc_version", columnList = "agreement_version_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementPayoutCalculation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_version_id", nullable = false)
    private AgreementVersion agreementVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_period_id", nullable = false)
    private AgreementTimePeriod timePeriod;

    @Column(name = "achieved_slab_tier")
    private Integer achievedSlabTier;

    @Column(name = "calculated_payout", nullable = false, precision = 15, scale = 2)
    private BigDecimal calculatedPayout;

    @Column(name = "is_max_payout_capped")
    private Boolean maxPayoutCapped;

    @Column(name = "calculation_date")
    private LocalDateTime calculationDate;

    @PrePersist
    void onCreate() {
        if (calculationDate == null) {
            calculationDate = LocalDateTime.now();
        }
        if (maxPayoutCapped == null) {
            maxPayoutCapped = false;
        }
    }
}
