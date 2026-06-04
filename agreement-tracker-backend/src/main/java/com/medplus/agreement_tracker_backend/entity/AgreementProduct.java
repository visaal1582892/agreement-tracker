package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agreement_products", indexes = {
        @Index(name = "idx_ap_agreement_id", columnList = "agreement_id"),
        @Index(name = "idx_ap_product_id", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementProduct extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    private Agreement agreement;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "manufacturer_id")
    private Long manufacturerId;

    @Column(name = "division_id")
    private Long divisionId;

    @Column(name = "product_name_snapshot", nullable = false, length = 255)
    private String productNameSnapshot;

    @Column(name = "manufacturer_name_snapshot", length = 255)
    private String manufacturerNameSnapshot;

    @Column(name = "division_name_snapshot", length = 255)
    private String divisionNameSnapshot;
}
