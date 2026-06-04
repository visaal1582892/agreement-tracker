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

    @Query("SELECT a FROM Agreement a WHERE a.approvalStatus = 'PENDING_APPROVAL'")
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
}
