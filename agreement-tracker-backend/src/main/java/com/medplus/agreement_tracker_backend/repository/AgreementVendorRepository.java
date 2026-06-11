package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementVendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementVendorRepository extends JpaRepository<AgreementVendor, Long> {

    List<AgreementVendor> findByAgreementVersionId(Long agreementVersionId);

    @Query("SELECT v FROM AgreementVendor v WHERE v.agreementVersion.id IN :agreementVersionIds")
    List<AgreementVendor> findByAgreementVersionIdIn(@Param("agreementVersionIds") List<Long> agreementVersionIds);

    void deleteByAgreementVersionId(Long agreementVersionId);
}
