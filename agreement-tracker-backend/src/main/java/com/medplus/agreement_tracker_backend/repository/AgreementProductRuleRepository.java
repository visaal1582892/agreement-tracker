package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementProductRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementProductRuleRepository extends JpaRepository<AgreementProductRule, Long> {

    List<AgreementProductRule> findByAgreementId(Long agreementId);

    void deleteByAgreementId(Long agreementId);
}
