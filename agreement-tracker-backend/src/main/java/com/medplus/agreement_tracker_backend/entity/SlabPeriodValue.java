package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "slab_period_values", indexes = {
        @Index(name = "idx_spv_slab_id", columnList = "agreement_slab_id"),
        @Index(name = "idx_spv_period_id", columnList = "agreement_time_period_id")
},
uniqueConstraints = @UniqueConstraint(name = "uk_slab_period", columnNames = {"agreement_slab_id", "agreement_time_period_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlabPeriodValue extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_slab_id", nullable = false)
    private AgreementSlab agreementSlab;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_time_period_id", nullable = false)
    private AgreementTimePeriod agreementTimePeriod;

    @Column(name = "commercial_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal commercialValue;
}
