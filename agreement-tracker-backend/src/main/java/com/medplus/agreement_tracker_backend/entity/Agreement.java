package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agreements", indexes = {
        @Index(name = "idx_ag_company_agreement_group_id", columnList = "company_agreement_group_id"),
        @Index(name = "idx_ag_owner_user_id", columnList = "owner_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agreement extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_agreement_group_id", nullable = false)
    private CompanyAgreementGroup companyAgreementGroup;

    @Column(name = "agreement_name", nullable = false, length = 255)
    private String agreementName;

    @Column(name = "current_version_id")
    private Long currentVersionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
