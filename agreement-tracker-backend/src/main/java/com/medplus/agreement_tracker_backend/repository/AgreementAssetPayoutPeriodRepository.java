package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementAssetPayoutPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementAssetPayoutPeriodRepository extends JpaRepository<AgreementAssetPayoutPeriod, Long> {

    List<AgreementAssetPayoutPeriod> findByAgreementVersionIdOrderByPeriodMonthsAscIdAsc(Long agreementVersionId);

    void deleteByAgreementVersionId(Long agreementVersionId);
}
