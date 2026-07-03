package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementSlab;
import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AgreementSlabRepository extends JpaRepository<AgreementSlab, Long> {

    List<AgreementSlab> findByAgreementVersionIdOrderByMinCapAsc(Long agreementVersionId);

    List<AgreementSlab> findByAgreementVersionIdAndSlabTypeOrderByMinCapAsc(
            Long agreementVersionId, CommercialSlabType slabType);

    @Modifying
    @Query("DELETE FROM AgreementSlab s WHERE s.agreementVersion.id = :agreementVersionId")
    void deleteByAgreementVersionId(@Param("agreementVersionId") Long agreementVersionId);
}
