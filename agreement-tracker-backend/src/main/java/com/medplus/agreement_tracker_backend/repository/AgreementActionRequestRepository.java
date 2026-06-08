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

    Optional<AgreementActionRequest> findFirstByAgreement_AgreementGroup_IdAndStatus(
            Long agreementGroupId, ActionRequestStatus status);

    List<AgreementActionRequest> findByAgreement_IdOrderByCreatedAtAsc(Long agreementId);

    @Query("""
            SELECT r FROM AgreementActionRequest r
            JOIN FETCH r.agreement a
            JOIN FETCH a.agreementGroup g
            JOIN FETCH r.requestedBy
            LEFT JOIN FETCH r.targetUser
            WHERE r.status = :status
            AND (:search IS NULL OR :search = '' OR
                 LOWER(g.agreementNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
                 LOWER(a.agreementName) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY r.createdAt ASC
            """)
    Page<AgreementActionRequest> findAllPending(
            @Param("status") ActionRequestStatus status,
            @Param("search") String search,
            Pageable pageable);
}
