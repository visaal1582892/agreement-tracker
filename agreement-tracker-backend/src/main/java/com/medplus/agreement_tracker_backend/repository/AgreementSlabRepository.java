package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementSlab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementSlabRepository extends JpaRepository<AgreementSlab, Long> {

    List<AgreementSlab> findByAgreementIdOrderByDisplayOrder(Long agreementId);

    void deleteByAgreementId(Long agreementId);
}
