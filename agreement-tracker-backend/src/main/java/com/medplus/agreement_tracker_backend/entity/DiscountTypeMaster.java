package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import com.medplus.agreement_tracker_backend.enums.DiscountCalculationKind;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "discount_type_master", indexes = {
        @Index(name = "idx_discount_type_code", columnList = "discount_code"),
        @Index(name = "idx_discount_type_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountTypeMaster extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "discount_code", nullable = false, unique = true, length = 50)
    private String discountCode;

    @Column(name = "discount_name", nullable = false, length = 255)
    private String discountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_kind", nullable = false, length = 30)
    private DiscountCalculationKind calculationKind;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
