package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementStoreMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgreementStoreMappingRepository extends JpaRepository<AgreementStoreMapping, Long> {

    List<AgreementStoreMapping> findByAgreementVersionIdOrderByStoreStoreCodeAsc(Long agreementVersionId);

    Optional<AgreementStoreMapping> findByAgreementVersionIdAndStoreId(Long agreementVersionId, Long storeId);

    boolean existsByAgreementVersionIdAndStoreId(Long agreementVersionId, Long storeId);

    void deleteByAgreementVersionId(Long agreementVersionId);

    void deleteByAgreementVersionIdAndIdIn(Long agreementVersionId, List<Long> mappingIds);
}
