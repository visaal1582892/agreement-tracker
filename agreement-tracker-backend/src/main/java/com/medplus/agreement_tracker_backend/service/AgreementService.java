package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.CreateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.TerminateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.response.AgreementGroupResponse;
import com.medplus.agreement_tracker_backend.dto.response.AgreementResponse;
import com.medplus.agreement_tracker_backend.dto.response.ApprovalTimelineResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AgreementService {

    AgreementResponse createDraft(CreateAgreementRequest request, Long currentUserId);

    AgreementResponse createNewVersion(Long agreementGroupId, Long currentUserId);

    AgreementResponse getAgreementById(Long agreementId);

    AgreementGroupResponse getGroupById(Long groupId);

    Page<AgreementGroupResponse> getAllGroups(Pageable pageable, Long currentUserId, boolean isAdmin);

    List<AgreementResponse> getVersionsByGroup(Long groupId);

    AgreementResponse submitForApproval(Long agreementId, Long currentUserId);

    AgreementResponse approve(Long agreementId, String remarks, Long approverId);

    AgreementResponse reject(Long agreementId, String remarks, Long approverId);

    AgreementResponse terminate(Long agreementId, TerminateAgreementRequest request, Long currentUserId);

    AgreementResponse toggleInProgress(Long agreementId, boolean inProgress, Long currentUserId);

    Page<AgreementResponse> getPendingApprovals(Pageable pageable);

    List<ApprovalTimelineResponse> getApprovalTimeline(Long agreementId);

    void bulkTransferOwnership(Long fromUserId, Long toUserId, List<Long> groupIds, Long performedByUserId);
}
