package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementManufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementManufacturerRepository extends JpaRepository<AgreementManufacturer, Long> {

    List<AgreementManufacturer> findByAgreementVersionId(Long agreementId);

    void deleteByAgreementVersionId(Long agreementId);
}
