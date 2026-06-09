package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import com.medplus.agreement_tracker_backend.enums.SlabValueType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "agreement_purchase_slabs", indexes = {
        @Index(name = "idx_aps_agreement_id", columnList = "agreement_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementPurchaseSlab extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    private Agreement agreement;

    @Column(name = "from_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal fromValue;

    @Column(name = "to_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal toValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private SlabValueType valueType;

    @Column(name = "commercial_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal commercialValue;
}
