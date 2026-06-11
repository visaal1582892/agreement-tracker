package com.medplus.agreement_tracker_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "company_master", indexes = {
        @Index(name = "idx_company_name", columnList = "company_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyMaster extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, unique = true, length = 255)
    private String companyName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @JsonIgnore
    @OneToMany(mappedBy = "company")
    @Builder.Default
    private List<CompanyAgreementGroup> agreementGroups = new ArrayList<>();
}
