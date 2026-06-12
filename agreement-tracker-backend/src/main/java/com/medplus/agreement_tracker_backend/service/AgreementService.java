package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.CreateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.EditAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.TerminateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.UpdateDraftRequest;
import com.medplus.agreement_tracker_backend.dto.response.AgreementResponse;
import com.medplus.agreement_tracker_backend.dto.response.AgreementVersionResponse;
import com.medplus.agreement_tracker_backend.dto.response.ApprovalTimelineResponse;
import com.medplus.agreement_tracker_backend.dto.response.BulkAgreementCreateResponse;
import com.medplus.agreement_tracker_backend.dto.response.BulkGroupSubmitResponse;
import com.medplus.agreement_tracker_backend.dto.response.RenewAgreementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AgreementService {

    BulkAgreementCreateResponse createDraft(CreateAgreementRequest request, Long currentUserId);

    AgreementVersionResponse createNewVersion(Long agreementId, Long currentUserId);

    AgreementVersionResponse createVersionedEdit(Long sourceAgreementVersionId, EditAgreementRequest request, Long currentUserId);

    AgreementVersionResponse updateDraft(Long agreementVersionId, UpdateDraftRequest request, Long currentUserId,
                                         boolean validateStep1, boolean validateStep2);

    AgreementVersionResponse cloneAgreement(Long sourceAgreementVersionId, Long currentUserId);

    AgreementVersionResponse getAgreementVersionById(Long agreementVersionId, Long currentUserId);

    AgreementResponse getAgreementById(Long agreementId, Long currentUserId);

    Page<AgreementResponse> getAllAgreements(Pageable pageable, Long currentUserId, String scope, boolean canViewAll,
                                           Long companyId, Long companyAgreementGroupId, String companyAgreementGroupName,
                                           String agreementName, String status, String ownerName,
                                           Long vendorId, Long incomeTypeId,
                                           LocalDate startDateFrom, LocalDate startDateTo,
                                           LocalDate endDateFrom, LocalDate endDateTo);

    List<AgreementVersionResponse> getVersionsByAgreementId(Long agreementId, Long currentUserId);

    AgreementVersionResponse transferOwnership(Long agreementVersionId, Long newOwnerUserId, Long performedByUserId,
                                               boolean isAdmin, String comments);

    AgreementVersionResponse completeApprovedTransfer(Long agreementVersionId, Long newOwnerUserId, Long approverId);

    AgreementVersionResponse submitForApproval(Long agreementVersionId, String comments, Long currentUserId);

    AgreementVersionResponse approve(Long agreementVersionId, String remarks, Long approverId);

    AgreementVersionResponse reject(Long agreementVersionId, String remarks, Long approverId);

    AgreementVersionResponse terminate(Long agreementVersionId, TerminateAgreementRequest request, Long currentUserId);

    AgreementVersionResponse toggleAgreementInProgress(Long agreementId, Long currentUserId);

    RenewAgreementResponse renewAgreement(Long agreementId, Long currentUserId);

    Page<AgreementVersionResponse> getPendingApprovals(String search, Pageable pageable);

    List<ApprovalTimelineResponse> getApprovalTimeline(Long agreementVersionId);

    void bulkTransferOwnership(Long fromUserId, Long toUserId, List<Long> agreementIds, Long performedByUserId);

    BulkGroupSubmitResponse submitGroupDraftsForApproval(Long groupId, Long currentUserId);

    void deleteDraftAgreement(Long agreementId, Long currentUserId);
}
