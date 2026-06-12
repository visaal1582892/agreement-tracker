package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "agreement_targets", indexes = {
        @Index(name = "idx_at_agreement_version_id", columnList = "agreement_version_id"),
        @Index(name = "idx_at_time_period_id", columnList = "time_period_id"),
        @Index(name = "idx_at_slab_id", columnList = "slab_id")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_target_coordinate",
        columnNames = {"agreement_version_id", "time_period_id", "slab_id"}
))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementTarget extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_version_id", nullable = false)
    private AgreementVersion agreementVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_period_id", nullable = false)
    private AgreementTimePeriod timePeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slab_id", nullable = false)
    private AgreementSlab slab;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    @Builder.Default
    private CommercialSlabType targetType = CommercialSlabType.SALE;

    @Column(name = "target_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetValue;
}
