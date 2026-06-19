package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
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
public interface AgreementVersionRepository extends JpaRepository<AgreementVersion, Long>, JpaSpecificationExecutor<AgreementVersion> {

    List<AgreementVersion> findByAgreementId(Long agreementId);

    Optional<AgreementVersion> findByAgreementIdAndVersionNumber(Long agreementId, Integer versionNumber);

    @Query("SELECT COALESCE(MAX(a.versionNumber), 0) FROM AgreementVersion a WHERE a.agreement.id = :agreementId")
    Integer findMaxVersionByAgreementId(@Param("agreementId") Long agreementId);

    Page<AgreementVersion> findByApprovalStatus(ApprovalStatus status, Pageable pageable);

    Page<AgreementVersion> findByOwnerIdAndApprovalStatus(Long ownerId, ApprovalStatus status, Pageable pageable);

    @Query("SELECT a FROM AgreementVersion a WHERE a.owner.id = :ownerId")
    Page<AgreementVersion> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("""
            SELECT a FROM AgreementVersion a
            JOIN FETCH a.agreement ag
            JOIN FETCH ag.companyAgreementGroup cag
            JOIN FETCH cag.company
            JOIN FETCH a.owner
            LEFT JOIN FETCH a.incomeType
            WHERE a.approvalStatus = 'PENDING_APPROVAL'
            AND (:search IS NULL OR :search = ''
                 OR LOWER(ag.agreementName) LIKE LOWER(CONCAT('%', :search, '%'))
                 OR LOWER(cag.company.companyName) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<AgreementVersion> findAllPendingApproval(@Param("search") String search, Pageable pageable);

    @Query("""
            SELECT a FROM AgreementVersion a
            WHERE a.approvalStatus = 'APPROVED'
            AND a.terminationDate IS NULL
            AND a.expiryDate BETWEEN :from AND :to
            """)
    List<AgreementVersion> findApprovedExpiringSoon(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            SELECT a FROM AgreementVersion a
            JOIN FETCH a.agreement ag
            JOIN FETCH ag.companyAgreementGroup cag
            JOIN FETCH cag.company
            JOIN FETCH ag.owner
            WHERE a.approvalStatus = 'APPROVED'
            AND a.terminationDate IS NULL
            AND a.expiryDate BETWEEN :today AND :limit
            AND a.id = ag.currentVersionId
            ORDER BY a.expiryDate ASC
            """)
    List<AgreementVersion> findApprovedExpiringWithinDays(@Param("today") LocalDate today, @Param("limit") LocalDate limit);

    @Query("""
            SELECT a FROM AgreementVersion a
            WHERE a.approvalStatus = 'APPROVED'
            AND a.terminationDate IS NULL
            AND a.expiryDate < :today
            AND a.inProgressFlag = false
            """)
    List<AgreementVersion> findExpiredNotInProgress(@Param("today") LocalDate today);

    @Query("""
            SELECT COUNT(a) FROM AgreementVersion a
            WHERE a.approvalStatus = 'APPROVED'
            AND a.terminationDate IS NULL
            AND a.expiryDate >= :today
            AND a.inProgressFlag = false
            """)
    long countActive(@Param("today") LocalDate today);

    @Query("""
            SELECT COUNT(a) FROM AgreementVersion a
            JOIN a.agreement ag
            WHERE a.approvalStatus = 'PENDING_APPROVAL'
            AND ag.owner.id = :userId
            """)
    long countPendingByOwner(@Param("userId") Long userId);

    @Query("""
            SELECT a FROM AgreementVersion a
            JOIN FETCH a.owner
            LEFT JOIN FETCH a.incomeType
            LEFT JOIN FETCH a.agreementType
            JOIN FETCH a.agreement
            WHERE a.agreement.id IN :agreementIds
            AND a.versionNumber = (
                SELECT MAX(a2.versionNumber) FROM AgreementVersion a2
                WHERE a2.agreement.id = a.agreement.id
            )
            """)
    List<AgreementVersion> findLatestVersionsForAgreementIds(@Param("agreementIds") List<Long> agreementIds);

    @Query("""
            SELECT a FROM AgreementVersion a
            JOIN FETCH a.owner
            LEFT JOIN FETCH a.incomeType
            LEFT JOIN FETCH a.agreementType
            JOIN FETCH a.agreement ag
            JOIN FETCH ag.companyAgreementGroup cag
            JOIN FETCH cag.company
            WHERE a.id IN :ids
            """)
    List<AgreementVersion> findByIdInWithDetails(@Param("ids") List<Long> ids);

    @Query("""
            SELECT av FROM AgreementVersion av
            JOIN FETCH av.agreement ag
            JOIN FETCH ag.owner
            JOIN FETCH ag.companyAgreementGroup cag
            JOIN FETCH cag.company
            JOIN FETCH av.owner
            LEFT JOIN FETCH av.incomeType
            LEFT JOIN FETCH av.agreementType
            WHERE ag.companyAgreementGroup.id = :groupId
            AND ag.isActive = true
            AND av.approvalStatus = com.medplus.agreement_tracker_backend.enums.ApprovalStatus.DRAFT
            AND av.versionNumber = (
                SELECT MAX(av2.versionNumber) FROM AgreementVersion av2
                WHERE av2.agreement.id = ag.id
            )
            ORDER BY ag.id ASC
            """)
    List<AgreementVersion> findLatestDraftVersionsByGroupId(@Param("groupId") Long groupId);

    @Query("""
            SELECT av FROM AgreementVersion av
            JOIN FETCH av.agreement ag
            JOIN FETCH ag.owner
            LEFT JOIN FETCH av.owner
            WHERE av.id = :id
            """)
    Optional<AgreementVersion> findByIdWithAgreementOwner(@Param("id") Long id);

    @Query("""
            SELECT av FROM AgreementVersion av
            JOIN FETCH av.agreement ag
            JOIN FETCH ag.owner
            WHERE av.approvalStatus = com.medplus.agreement_tracker_backend.enums.ApprovalStatus.APPROVED
            AND av.terminationDate IS NULL
            AND av.id = ag.currentVersionId
            AND ag.isActive = true
            """)
    List<AgreementVersion> findCurrentApprovedVersions();

    @Query("""
            SELECT av FROM AgreementVersion av
            JOIN FETCH av.agreement ag
            WHERE av.inProgressFlag = true
            AND av.inProgressSince IS NOT NULL
            AND av.inProgressSince < :cutoff
            AND av.id = ag.currentVersionId
            AND av.approvalStatus = com.medplus.agreement_tracker_backend.enums.ApprovalStatus.APPROVED
            """)
    List<AgreementVersion> findStaleInProgressCurrentVersions(@Param("cutoff") java.time.LocalDateTime cutoff);
}

