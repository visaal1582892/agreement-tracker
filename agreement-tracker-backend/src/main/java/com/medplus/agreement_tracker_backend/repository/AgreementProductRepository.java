package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementProductRepository extends JpaRepository<AgreementProduct, Long> {

    List<AgreementProduct> findByAgreementId(Long agreementId);

    void deleteByAgreementId(Long agreementId);

    @Query("SELECT ap.productId FROM AgreementProduct ap WHERE ap.agreement.id = :agreementId")
    List<Long> findProductIdsByAgreementId(@Param("agreementId") Long agreementId);
}
