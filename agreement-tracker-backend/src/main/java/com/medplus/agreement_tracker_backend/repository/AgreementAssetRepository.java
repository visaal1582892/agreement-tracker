package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgreementAssetRepository extends JpaRepository<AgreementAsset, Long> {

    Optional<AgreementAsset> findByAgreementVersionId(Long agreementVersionId);

    void deleteByAgreementVersionId(Long agreementVersionId);
}
