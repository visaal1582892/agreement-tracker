package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agreement_groups", indexes = {
        @Index(name = "idx_ag_agreement_number", columnList = "agreement_number"),
        @Index(name = "idx_ag_company_id", columnList = "company_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementGroup extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyMaster company;

    @Column(name = "agreement_number", nullable = false, unique = true, length = 50)
    private String agreementNumber;

    /**
     * Points to the currently active/approved version.
     * Nullable: null until first version is approved.
     * Uses plain FK (not bi-directional) to avoid circular JPA loading.
     */
    @Column(name = "current_version_id")
    private Long currentVersionId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}
