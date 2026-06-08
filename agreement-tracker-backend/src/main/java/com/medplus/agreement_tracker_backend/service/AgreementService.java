package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.CreateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.EditAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.TerminateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.UpdateDraftRequest;
import com.medplus.agreement_tracker_backend.dto.response.AgreementGroupResponse;
import com.medplus.agreement_tracker_backend.dto.response.AgreementResponse;
import com.medplus.agreement_tracker_backend.dto.response.ApprovalTimelineResponse;
import com.medplus.agreement_tracker_backend.dto.response.BulkAgreementCreateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AgreementService {

    BulkAgreementCreateResponse createDraft(CreateAgreementRequest request, Long currentUserId);

    AgreementResponse createNewVersion(Long agreementGroupId, Long currentUserId);

    AgreementResponse createVersionedEdit(Long sourceAgreementId, EditAgreementRequest request, Long currentUserId);

    AgreementResponse updateDraft(Long agreementId, UpdateDraftRequest request, Long currentUserId, boolean validateStep1);

    AgreementResponse getAgreementById(Long agreementId, Long currentUserId);

    AgreementGroupResponse getGroupById(Long groupId, Long currentUserId);

    Page<AgreementGroupResponse> getAllGroups(Pageable pageable, Long currentUserId, String scope, boolean canViewAll,
                                              String agreementNumber, String companyName, String status, String ownerName,
                                              Long vendorId, Long incomeTypeId);

    List<AgreementResponse> getVersionsByGroup(Long groupId, Long currentUserId);

    List<AgreementResponse> getVersionsByAgreementId(Long agreementId, Long currentUserId);

    AgreementResponse transferOwnership(Long agreementId, Long newOwnerUserId, Long performedByUserId,
                                        boolean isAdmin, String comments);

    AgreementResponse submitForApproval(Long agreementId, String comments, Long currentUserId);

    AgreementResponse approve(Long agreementId, String remarks, Long approverId);

    AgreementResponse reject(Long agreementId, String remarks, Long approverId);

    AgreementResponse terminate(Long agreementId, TerminateAgreementRequest request, Long currentUserId);

    AgreementResponse toggleInProgress(Long agreementId, boolean inProgress, Long currentUserId);

    Page<AgreementResponse> getPendingApprovals(String search, Pageable pageable);

    List<ApprovalTimelineResponse> getApprovalTimeline(Long agreementId);

    void bulkTransferOwnership(Long fromUserId, Long toUserId, List<Long> groupIds, Long performedByUserId);
}
