package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementDivisionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementDivisionRuleRepository extends JpaRepository<AgreementDivisionRule, Long> {

    List<AgreementDivisionRule> findByAgreementVersionId(Long agreementId);

    void deleteByAgreementVersionId(Long agreementId);
}
