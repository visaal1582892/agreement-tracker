package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_company_assignments", indexes = {
        @Index(name = "idx_uca_user_id", columnList = "user_id"),
        @Index(name = "idx_uca_company_id", columnList = "company_id")
},
uniqueConstraints = @UniqueConstraint(name = "uk_user_company", columnNames = {"user_id", "company_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCompanyAssignment extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyMaster company;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;
}
