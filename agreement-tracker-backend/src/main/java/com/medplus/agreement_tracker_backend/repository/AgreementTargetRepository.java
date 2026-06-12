package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementTarget;
import com.medplus.agreement_tracker_backend.enums.CommercialSlabType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgreementTargetRepository extends JpaRepository<AgreementTarget, Long> {

    List<AgreementTarget> findByAgreementVersionId(Long agreementVersionId);

    Optional<AgreementTarget> findByAgreementVersionIdAndTimePeriodIdAndSlabId(
            Long agreementVersionId, Long timePeriodId, Long slabId);

    @Modifying
    @Query("DELETE FROM AgreementTarget t WHERE t.slab.id = :slabId")
    void deleteBySlabId(@Param("slabId") Long slabId);

    @Modifying
    @Query("DELETE FROM AgreementTarget t WHERE t.agreementVersion.id = :agreementVersionId")
    void deleteByAgreementVersionId(@Param("agreementVersionId") Long agreementVersionId);

    @Modifying
    @Query("DELETE FROM AgreementTarget t WHERE t.agreementVersion.id = :agreementVersionId "
            + "AND t.targetType = :targetType")
    void deleteByAgreementVersionIdAndTargetType(
            @Param("agreementVersionId") Long agreementVersionId,
            @Param("targetType") CommercialSlabType targetType);

    @Modifying
    @Query("DELETE FROM AgreementTarget t WHERE t.agreementVersion.id = :agreementVersionId "
            + "AND t.timePeriod.id = :timePeriodId AND t.slab.id = :slabId")
    void deleteByAgreementVersionIdAndTimePeriodIdAndSlabId(
            @Param("agreementVersionId") Long agreementVersionId,
            @Param("timePeriodId") Long timePeriodId,
            @Param("slabId") Long slabId);
}
