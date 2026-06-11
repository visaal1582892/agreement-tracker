package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import com.medplus.agreement_tracker_backend.enums.RuleType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agreement_division_rules", indexes = {
        @Index(name = "idx_adr_agreement_version_id", columnList = "agreement_version_id"),
        @Index(name = "idx_adr_division_id", columnList = "division_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementDivisionRule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_version_id", nullable = false)
    private AgreementVersion agreementVersion;

    @Column(name = "division_id", nullable = false)
    private Long divisionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 10)
    private RuleType ruleType;
}
