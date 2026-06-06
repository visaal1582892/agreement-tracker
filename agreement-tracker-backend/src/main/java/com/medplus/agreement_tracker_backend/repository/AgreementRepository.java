package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.Agreement;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, Long>, JpaSpecificationExecutor<Agreement> {

    List<Agreement> findByAgreementGroupId(Long agreementGroupId);

    Optional<Agreement> findByAgreementGroupIdAndVersionNumber(Long groupId, Integer versionNumber);

    @Query("SELECT COALESCE(MAX(a.versionNumber), 0) FROM Agreement a WHERE a.agreementGroup.id = :groupId")
    Integer findMaxVersionByGroupId(@Param("groupId") Long groupId);

    Page<Agreement> findByApprovalStatus(ApprovalStatus status, Pageable pageable);

    Page<Agreement> findByOwnerIdAndApprovalStatus(Long ownerId, ApprovalStatus status, Pageable pageable);

    @Query("SELECT a FROM Agreement a WHERE a.owner.id = :ownerId")
    Page<Agreement> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("""
            SELECT a FROM Agreement a
            JOIN FETCH a.agreementGroup
            JOIN FETCH a.owner
            LEFT JOIN FETCH a.incomeType
            WHERE a.approvalStatus = 'PENDING_APPROVAL'
            """)
    Page<Agreement> findAllPendingApproval(Pageable pageable);

    @Query("""
            SELECT a FROM Agreement a
            WHERE a.approvalStatus = 'APPROVED'
            AND a.terminationDate IS NULL
            AND a.expiryDate BETWEEN :from AND :to
            """)
    List<Agreement> findApprovedExpiringSoon(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT a FROM Agreement a
            JOIN FETCH a.agreementGroup ag
            JOIN FETCH ag.company
            JOIN FETCH a.owner
            WHERE a.approvalStatus = 'APPROVED'
            AND a.terminationDate IS NULL
            AND a.expiryDate BETWEEN :today AND :limit
            AND a.id = ag.currentVersionId
            ORDER BY a.expiryDate ASC
            """)
    List<Agreement> findApprovedExpiringWithinDays(@Param("today") LocalDate today, @Param("limit") LocalDate limit);

    @Query("""
            SELECT a FROM Agreement a
            WHERE a.approvalStatus = 'APPROVED'
            AND a.terminationDate IS NULL
            AND a.expiryDate < :today
            AND a.inProgressFlag = false
            """)
    List<Agreement> findExpiredNotInProgress(@Param("today") LocalDate today);

    @Query("""
            SELECT COUNT(a) FROM Agreement a
            WHERE a.approvalStatus = 'APPROVED'
            AND a.terminationDate IS NULL
            AND a.expiryDate >= :today
            AND a.inProgressFlag = false
            """)
    long countActive(@Param("today") LocalDate today);

    @Query("""
            SELECT COUNT(a) FROM Agreement a
            WHERE a.approvalStatus = 'PENDING_APPROVAL'
            AND a.owner.id = :userId
            """)
    long countPendingByOwner(@Param("userId") Long userId);

    /**
     * Batch-fetches the highest-version Agreement for each group in the supplied ID list.
     * Uses a single correlated subquery to pick the max versionNumber, eagerly loading
     * owner and incomeType to prevent N+1 queries in the list view.
     */
    @Query("""
            SELECT a FROM Agreement a
            JOIN FETCH a.owner
            LEFT JOIN FETCH a.incomeType
            WHERE a.agreementGroup.id IN :groupIds
            AND a.versionNumber = (
                SELECT MAX(a2.versionNumber) FROM Agreement a2
                WHERE a2.agreementGroup.id = a.agreementGroup.id
            )
            """)
    List<Agreement> findLatestVersionsForGroupIds(@Param("groupIds") List<Long> groupIds);
}
