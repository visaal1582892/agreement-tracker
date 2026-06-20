package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import com.medplus.agreement_tracker_backend.enums.SlabValueType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "agreement_slabs", indexes = {
        @Index(name = "idx_as_agreement_version_id", columnList = "agreement_version_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementSlab extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_version_id", nullable = false)
    private AgreementVersion agreementVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "slab_type", nullable = false, length = 20)
    @Builder.Default
    private CommercialSlabType slabType = CommercialSlabType.PURCHASE;

    @Column(name = "from_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal fromValue;

    @Column(name = "to_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal toValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private SlabValueType valueType;

    @Column(name = "commercial_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal commercialValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_frequency", length = 20)
    private PayoutFrequency payoutFrequency;
}
