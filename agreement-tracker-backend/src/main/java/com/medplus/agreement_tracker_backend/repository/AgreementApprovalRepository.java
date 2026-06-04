package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementApprovalRepository extends JpaRepository<AgreementApproval, Long> {

    List<AgreementApproval> findByAgreementIdOrderByCreatedAtAsc(Long agreementId);
}
