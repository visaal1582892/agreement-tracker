package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementActionRequest;
import com.medplus.agreement_tracker_backend.enums.ActionRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgreementActionRequestRepository extends JpaRepository<AgreementActionRequest, Long> {

    Optional<AgreementActionRequest> findFirstByAgreementVersion_Agreement_IdAndStatus(
            Long agreementId, ActionRequestStatus status);

    Optional<AgreementActionRequest> findFirstByCompanyAgreementGroup_IdAndStatus(
            Long groupId, ActionRequestStatus status);

    List<AgreementActionRequest> findByAgreementVersion_IdOrderByCreatedAtAsc(Long agreementVersionId);

    void deleteByAgreementVersionId(Long agreementVersionId);

    @Query("""
            SELECT r FROM AgreementActionRequest r
            LEFT JOIN FETCH r.agreementVersion av
            LEFT JOIN FETCH av.agreement ag
            LEFT JOIN FETCH r.companyAgreementGroup cag
            LEFT JOIN FETCH cag.company
            JOIN FETCH r.requestedBy
            LEFT JOIN FETCH r.targetUser
            WHERE r.status = :status
            AND (:search IS NULL OR :search = '' OR
                 LOWER(COALESCE(ag.agreementName, cag.name, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(COALESCE(ag.companyAgreementGroup.company.companyName, cag.company.companyName, ''))
                    LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY r.createdAt ASC
            """)
    Page<AgreementActionRequest> findAllPending(
            @Param("status") ActionRequestStatus status,
            @Param("search") String search,
            Pageable pageable);
}
