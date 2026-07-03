package com.medplus.agreement_tracker_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "agreement_asset_payout_periods", indexes = {
        @Index(name = "idx_asset_periods_version", columnList = "agreement_version_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementAssetPayoutPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_version_id", nullable = false)
    private AgreementVersion agreementVersion;

    @Column(name = "period_months", nullable = false)
    private Integer periodMonths;

    @Column(name = "payout_per_store", nullable = false, precision = 15, scale = 2)
    private BigDecimal payoutPerStore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
