package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgreementGroupRepository extends JpaRepository<AgreementGroup, Long>, JpaSpecificationExecutor<AgreementGroup> {

    Optional<AgreementGroup> findByAgreementNumber(String agreementNumber);

    boolean existsByAgreementNumber(String agreementNumber);

    Page<AgreementGroup> findByCompanyId(Long companyId, Pageable pageable);

    @Query("SELECT COUNT(ag) FROM AgreementGroup ag WHERE ag.agreementNumber LIKE :prefix%")
    long countByAgreementNumberStartingWith(@Param("prefix") String prefix);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(ag.agreementNumber, LENGTH(:prefix) + 2) AS int)), 0) FROM AgreementGroup ag WHERE ag.agreementNumber LIKE :prefix%")
    Integer findMaxSequenceForPrefix(@Param("prefix") String prefix);

    @Query("SELECT ag FROM AgreementGroup ag WHERE ag.company.id IN :companyIds AND ag.isActive = true")
    Page<AgreementGroup> findByCompanyIds(@Param("companyIds") List<Long> companyIds, Pageable pageable);
}
