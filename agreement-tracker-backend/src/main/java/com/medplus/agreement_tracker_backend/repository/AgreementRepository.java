package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.Agreement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, Long>, JpaSpecificationExecutor<Agreement> {

    @Query("SELECT a FROM Agreement a WHERE a.owner.id = :ownerId")
    Page<Agreement> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    List<Agreement> findByCompanyAgreementGroupId(Long companyAgreementGroupId);

    boolean existsByCompanyAgreementGroupIdAndIsActiveTrue(Long companyAgreementGroupId);

    long countByCompanyAgreementGroupId(Long companyAgreementGroupId);

    boolean existsByCompanyAgreementGroupIdAndOwner_Id(Long groupId, Long ownerUserId);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM Agreement a
            WHERE a.companyAgreementGroup.id = :groupId
            AND (
                a.currentVersionId IS NOT NULL
                OR EXISTS (
                    SELECT 1 FROM AgreementVersion av
                    WHERE av.agreement.id = a.id
                    AND av.approvalStatus <> com.medplus.agreement_tracker_backend.enums.ApprovalStatus.DRAFT
                )
            )
            """)
    boolean hasActiveAgreementsInGroup(@Param("groupId") Long groupId);
}
