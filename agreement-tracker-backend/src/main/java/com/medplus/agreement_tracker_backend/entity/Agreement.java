package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.enums.CommercialStructure;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "agreements", indexes = {
        @Index(name = "idx_agr_group_id", columnList = "agreement_group_id"),
        @Index(name = "idx_agr_approval_status", columnList = "approval_status"),
        @Index(name = "idx_agr_owner_user_id", columnList = "owner_user_id"),
        @Index(name = "idx_agr_expiry_date", columnList = "expiry_date")
},
uniqueConstraints = @UniqueConstraint(name = "uk_group_version", columnNames = {"agreement_group_id", "version_number"}))
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
    @JoinColumn(name = "agreement_group_id", nullable = false)
    private AgreementGroup agreementGroup;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "income_type_id")
    private IncomeType incomeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_type_id")
    private AgreementType agreementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "commercial_structure", nullable = false, length = 10)
    private CommercialStructure commercialStructure;

    @Column(name = "commercial_value", precision = 15, scale = 2)
    private BigDecimal commercialValue;

    @Column(name = "calculation_formula", length = 500)
    private String calculationFormula;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.DRAFT;

    @Column(name = "in_progress_flag", nullable = false)
    @Builder.Default
    private boolean inProgressFlag = false;

    @Column(name = "termination_reason", length = 500)
    private String terminationReason;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "notes", length = 1000)
    private String notes;
}
