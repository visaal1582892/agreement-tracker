package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementTimePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementTimePeriodRepository extends JpaRepository<AgreementTimePeriod, Long> {

    List<AgreementTimePeriod> findByAgreementIdOrderByDisplayOrder(Long agreementId);

    void deleteByAgreementId(Long agreementId);
}
