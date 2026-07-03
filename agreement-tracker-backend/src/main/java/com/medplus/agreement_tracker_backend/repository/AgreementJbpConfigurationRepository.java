package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementJbpConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementJbpConfigurationRepository extends JpaRepository<AgreementJbpConfiguration, Long> {

    List<AgreementJbpConfiguration> findByAgreementVersionId(Long agreementVersionId);

    @Query("""
            SELECT DISTINCT c FROM AgreementJbpConfiguration c
            LEFT JOIN FETCH c.selectedPeriods
            WHERE c.agreementVersion.id = :agreementVersionId
            ORDER BY c.id
            """)
    List<AgreementJbpConfiguration> findHydratedByAgreementVersionId(
            @Param("agreementVersionId") Long agreementVersionId);

    boolean existsByAgreementVersionId(Long agreementVersionId);

    void deleteByAgreementVersionId(Long agreementVersionId);
}
