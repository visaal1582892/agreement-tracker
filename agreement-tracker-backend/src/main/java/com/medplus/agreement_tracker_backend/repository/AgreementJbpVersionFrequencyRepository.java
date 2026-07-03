package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementJbpVersionFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementJbpVersionFrequencyRepository extends JpaRepository<AgreementJbpVersionFrequency, Long> {

    List<AgreementJbpVersionFrequency> findByAgreementVersionIdOrderByFrequencyAsc(Long agreementVersionId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AgreementJbpVersionFrequency f WHERE f.agreementVersion.id = :agreementVersionId")
    void deleteByAgreementVersionId(@Param("agreementVersionId") Long agreementVersionId);
}
