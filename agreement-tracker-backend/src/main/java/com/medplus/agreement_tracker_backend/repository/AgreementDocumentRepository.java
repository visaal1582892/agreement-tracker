package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementDocument;
import com.medplus.agreement_tracker_backend.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementDocumentRepository extends JpaRepository<AgreementDocument, Long> {

    List<AgreementDocument> findByAgreementIdAndIsActiveTrue(Long agreementId);

    List<AgreementDocument> findByAgreementIdAndDocumentTypeAndIsActiveTrue(Long agreementId, DocumentType documentType);
}
