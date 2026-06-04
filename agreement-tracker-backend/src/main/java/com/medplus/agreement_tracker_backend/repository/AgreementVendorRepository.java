package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementVendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementVendorRepository extends JpaRepository<AgreementVendor, Long> {

    List<AgreementVendor> findByAgreementId(Long agreementId);

    void deleteByAgreementId(Long agreementId);
}
