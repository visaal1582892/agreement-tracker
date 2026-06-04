package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgreementAuditRepository extends JpaRepository<AgreementAudit, Long> {

    Page<AgreementAudit> findByAgreementGroupIdOrderByCreatedAtDesc(Long agreementGroupId, Pageable pageable);

    Page<AgreementAudit> findByAgreementIdOrderByCreatedAtDesc(Long agreementId, Pageable pageable);
}
