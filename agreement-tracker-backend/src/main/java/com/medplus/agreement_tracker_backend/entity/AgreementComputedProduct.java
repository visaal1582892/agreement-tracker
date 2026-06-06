package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agreement_computed_products", indexes = {
        @Index(name = "idx_acp_agreement_id", columnList = "agreement_id"),
        @Index(name = "idx_acp_product_id", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementComputedProduct extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    private Agreement agreement;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name_snapshot", nullable = false, length = 255)
    private String productNameSnapshot;

    @Column(name = "division_name_snapshot", length = 255)
    private String divisionNameSnapshot;

    @Column(name = "manufacturer_name_snapshot", length = 255)
    private String manufacturerNameSnapshot;
}
