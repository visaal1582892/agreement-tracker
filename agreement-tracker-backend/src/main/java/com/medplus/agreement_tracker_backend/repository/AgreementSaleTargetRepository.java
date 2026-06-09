package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementSaleTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgreementSaleTargetRepository extends JpaRepository<AgreementSaleTarget, Long> {

    List<AgreementSaleTarget> findByAgreementId(Long agreementId);

    Optional<AgreementSaleTarget> findByAgreementIdAndTimePeriodIdAndSlabId(
            Long agreementId, Long timePeriodId, Long slabId);

    @Modifying
    @Query("DELETE FROM AgreementSaleTarget t WHERE t.slab.id = :slabId")
    void deleteBySlabId(@Param("slabId") Long slabId);

    @Modifying
    @Query("DELETE FROM AgreementSaleTarget t WHERE t.agreement.id = :agreementId")
    void deleteByAgreementId(@Param("agreementId") Long agreementId);

    @Modifying
    @Query("DELETE FROM AgreementSaleTarget t WHERE t.agreement.id = :agreementId "
            + "AND t.timePeriod.id = :timePeriodId AND t.slab.id = :slabId")
    void deleteByAgreementIdAndTimePeriodIdAndSlabId(
            @Param("agreementId") Long agreementId,
            @Param("timePeriodId") Long timePeriodId,
            @Param("slabId") Long slabId);
}

