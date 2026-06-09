package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "agreement_sale_targets", indexes = {
        @Index(name = "idx_ast_agreement_id", columnList = "agreement_id"),
        @Index(name = "idx_ast_time_period_id", columnList = "time_period_id"),
        @Index(name = "idx_ast_slab_id", columnList = "slab_id")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_target_coordinate",
        columnNames = {"agreement_id", "time_period_id", "slab_id"}
))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementSaleTarget extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    private Agreement agreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_period_id", nullable = false)
    private AgreementTimePeriod timePeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slab_id", nullable = false)
    private AgreementPurchaseSlab slab;

    @Column(name = "target_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetValue;
}
