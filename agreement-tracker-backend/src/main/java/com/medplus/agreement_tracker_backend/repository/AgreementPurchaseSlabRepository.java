package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementPurchaseSlab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AgreementPurchaseSlabRepository extends JpaRepository<AgreementPurchaseSlab, Long> {

    List<AgreementPurchaseSlab> findByAgreementVersionIdOrderByFromValueAsc(Long agreementVersionId);

    @Modifying
    @Query("DELETE FROM AgreementPurchaseSlab s WHERE s.agreementVersion.id = :agreementVersionId")
    void deleteByAgreementVersionId(@Param("agreementVersionId") Long agreementVersionId);
}
