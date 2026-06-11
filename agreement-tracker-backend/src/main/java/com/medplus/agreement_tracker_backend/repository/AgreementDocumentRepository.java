package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementDocument;
import com.medplus.agreement_tracker_backend.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementDocumentRepository extends JpaRepository<AgreementDocument, Long> {

    List<AgreementDocument> findByAgreementVersionIdAndIsActiveTrue(Long agreementId);

    List<AgreementDocument> findByAgreementVersionIdAndDocumentTypeAndIsActiveTrue(Long agreementId, DocumentType documentType);

    @Modifying
    @Query("DELETE FROM AgreementDocument d WHERE d.agreementVersion.id = :agreementVersionId")
    void deleteByAgreementVersionId(@Param("agreementVersionId") Long agreementVersionId);
}
