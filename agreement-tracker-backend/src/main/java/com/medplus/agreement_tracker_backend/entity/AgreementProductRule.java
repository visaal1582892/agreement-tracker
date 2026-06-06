package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import com.medplus.agreement_tracker_backend.enums.RuleType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agreement_product_rules", indexes = {
        @Index(name = "idx_apr_agreement_id", columnList = "agreement_id"),
        @Index(name = "idx_apr_product_id", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementProductRule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false)
    private Agreement agreement;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 10)
    private RuleType ruleType;
}
