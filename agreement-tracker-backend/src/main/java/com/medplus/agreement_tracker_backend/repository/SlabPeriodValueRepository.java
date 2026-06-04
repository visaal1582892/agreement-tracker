package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.SlabPeriodValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SlabPeriodValueRepository extends JpaRepository<SlabPeriodValue, Long> {

    List<SlabPeriodValue> findByAgreementSlabId(Long slabId);

    @Query("SELECT spv FROM SlabPeriodValue spv WHERE spv.agreementSlab.agreement.id = :agreementId")
    List<SlabPeriodValue> findByAgreementId(@Param("agreementId") Long agreementId);

    @Query("DELETE FROM SlabPeriodValue spv WHERE spv.agreementSlab.agreement.id = :agreementId")
    void deleteByAgreementId(@Param("agreementId") Long agreementId);
}
