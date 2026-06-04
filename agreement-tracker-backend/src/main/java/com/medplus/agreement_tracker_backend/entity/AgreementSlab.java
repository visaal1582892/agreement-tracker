package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "agreement_slabs", indexes = {
        @Index(name = "idx_slab_agreement_id", columnList = "agreement_id")
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
    @JoinColumn(name = "agreement_id", nullable = false)
    private Agreement agreement;

    @Column(name = "slab_name", nullable = false, length = 100)
    private String slabName;

    @Column(name = "from_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal fromValue;

    @Column(name = "to_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal toValue;

    @Column(name = "display_order")
    private Integer displayOrder;
}
