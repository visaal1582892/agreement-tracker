package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementJbpCommercialPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementJbpCommercialPeriodRepository extends JpaRepository<AgreementJbpCommercialPeriod, Long> {

    List<AgreementJbpCommercialPeriod> findByAgreementVersionId(Long agreementVersionId);

    @Query("""
            SELECT DISTINCT p FROM AgreementJbpCommercialPeriod p
            JOIN FETCH p.timePeriod
            LEFT JOIN FETCH p.parentTimePeriod
            JOIN FETCH p.jbpConfiguration
            WHERE p.agreementVersion.id = :agreementVersionId
            """)
    List<AgreementJbpCommercialPeriod> findHydrationRowsByAgreementVersionId(
            @Param("agreementVersionId") Long agreementVersionId);

    boolean existsByAgreementVersionId(Long agreementVersionId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AgreementJbpCommercialPeriod p WHERE p.agreementVersion.id = :agreementVersionId")
    void deleteByAgreementVersionId(@Param("agreementVersionId") Long agreementVersionId);
}
