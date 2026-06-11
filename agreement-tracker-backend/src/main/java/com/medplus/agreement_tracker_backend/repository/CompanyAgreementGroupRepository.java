package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.CompanyAgreementGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyAgreementGroupRepository extends JpaRepository<CompanyAgreementGroup, Long>,
        JpaSpecificationExecutor<CompanyAgreementGroup> {

    List<CompanyAgreementGroup> findByCompanyIdAndIsActiveTrue(Long companyId);

    boolean existsByCompanyIdAndName(Long companyId, String name);

    boolean existsByCompanyIdAndNameAndIdNot(Long companyId, String name, Long excludeId);

    Optional<CompanyAgreementGroup> findByCompanyIdAndName(Long companyId, String name);
}
