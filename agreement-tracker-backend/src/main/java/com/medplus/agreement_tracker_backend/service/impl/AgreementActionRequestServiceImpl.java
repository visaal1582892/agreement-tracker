package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.InitiateTerminateRequest;
import com.medplus.agreement_tracker_backend.dto.request.InitiateTransferRequest;
import com.medplus.agreement_tracker_backend.dto.request.ResolveActionRequest;
import com.medplus.agreement_tracker_backend.dto.request.TerminateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.response.AgreementActionRequestResponse;
import com.medplus.agreement_tracker_backend.entity.AgreementActionRequest;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.entity.User;
import com.medplus.agreement_tracker_backend.enums.ActionRequestStatus;
import com.medplus.agreement_tracker_backend.enums.ActionRequestType;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.exception.UnauthorizedException;
import com.medplus.agreement_tracker_backend.repository.AgreementActionRequestRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementVersionRepository;
import com.medplus.agreement_tracker_backend.repository.UserRepository;
import com.medplus.agreement_tracker_backend.service.AgreementActionRequestService;
import com.medplus.agreement_tracker_backend.service.AgreementService;
import com.medplus.agreement_tracker_backend.service.CompanyAgreementGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AgreementActionRequestServiceImpl implements AgreementActionRequestService {

    private final AgreementActionRequestRepository actionRequestRepository;
    private final AgreementVersionRepository agreementVersionRepository;
    private final UserRepository userRepository;
    private final AgreementService agreementService;
    private final CompanyAgreementGroupService companyAgreementGroupService;

    @Override
    @Transactional
    public AgreementActionRequestResponse initiateTransfer(Long agreementVersionId, InitiateTransferRequest request,
                                                           Long currentUserId, boolean isAdmin) {
        if (isAdmin) {
            throw new BusinessException("Admin transfers must use the direct transfer endpoint");
        }

        AgreementVersion version = loadVersion(agreementVersionId);
        validateNoPendingRequest(version.getAgreement().getId());

        Long currentOwnerId = version.getAgreement().getOwner().getId();
        if (!currentOwnerId.equals(currentUserId)) {
            throw new UnauthorizedException("Only the owner can request ownership transfer");
        }

        if (request.newOwnerId().equals(currentOwnerId)) {
            throw new BusinessException("Agreement is already owned by this user");
        }

        User newOwner = userRepository.findById(request.newOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.newOwnerId()));

        User requestedBy = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        AgreementActionRequest actionRequest = AgreementActionRequest.builder()
                .agreementVersion(version)
                .actionType(ActionRequestType.TRANSFER)
                .status(ActionRequestStatus.PENDING)
                .requestedBy(requestedBy)
                .targetUser(newOwner)
                .reasonComments(request.comments().trim())
                .build();

        return toResponse(actionRequestRepository.save(actionRequest));
    }

    @Override
    @Transactional
    public AgreementActionRequestResponse initiateTerminate(Long agreementVersionId, InitiateTerminateRequest request,
                                                            Long currentUserId) {
        AgreementVersion version = loadVersion(agreementVersionId);
        validateNoPendingRequest(version.getAgreement().getId());

        if (version.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException("Only APPROVED agreements can be terminated");
        }
        if (version.getTerminationDate() != null) {
            throw new BusinessException("Agreement is already terminated");
        }

        User requestedBy = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        AgreementActionRequest actionRequest = AgreementActionRequest.builder()
                .agreementVersion(version)
                .actionType(ActionRequestType.TERMINATE)
                .status(ActionRequestStatus.PENDING)
                .requestedBy(requestedBy)
                .reasonComments(request.comments().trim())
                .requestedTerminationDate(request.requestedTerminationDate())
                .build();

        return toResponse(actionRequestRepository.save(actionRequest));
    }

    @Override
    @Transactional
    public AgreementActionRequestResponse resolveRequest(Long requestId, ResolveActionRequest request,
                                                         Long approverId) {
        AgreementActionRequest actionRequest = actionRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementActionRequest", requestId));

        if (actionRequest.getStatus() != ActionRequestStatus.PENDING) {
            throw new BusinessException("Request has already been resolved");
        }

        if (actionRequest.getRequestedBy().getId().equals(approverId)) {
            throw new BusinessException("You cannot approve or reject your own request");
        }

        if (Boolean.FALSE.equals(request.approved())
                && !StringUtils.hasText(request.approverComments())) {
            throw new BusinessException("Approver comments are required when rejecting a request");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("User", approverId));

        actionRequest.setResolvedBy(approver);
        actionRequest.setResolvedAt(LocalDateTime.now());
        if (StringUtils.hasText(request.approverComments())) {
            actionRequest.setApproverComments(request.approverComments().trim());
        }

        if (Boolean.TRUE.equals(request.approved())) {
            actionRequest.setStatus(ActionRequestStatus.APPROVED);
            actionRequestRepository.save(actionRequest);

            switch (actionRequest.getActionType()) {
                case TRANSFER -> agreementService.transferOwnership(
                        actionRequest.getAgreementVersion().getId(),
                        actionRequest.getTargetUser().getId(),
                        approverId,
                        false,
                        null);
                case TERMINATE -> {
                    AgreementVersion version = actionRequest.getAgreementVersion();
                    LocalDate terminationDate = actionRequest.getRequestedTerminationDate() != null
                            ? actionRequest.getRequestedTerminationDate()
                            : LocalDate.now();
                    agreementService.terminate(
                            version.getId(),
                            new TerminateAgreementRequest(terminationDate, actionRequest.getReasonComments()),
                            approverId);
                }
                case DELETE_GROUP -> companyAgreementGroupService.executeApprovedGroupDeletion(
                        actionRequest.getCompanyAgreementGroup().getId(),
                        approverId,
                        actionRequest.getReasonComments());
            }
        } else {
            actionRequest.setStatus(ActionRequestStatus.REJECTED);
            actionRequestRepository.save(actionRequest);
        }

        return toResponse(actionRequestRepository.findById(requestId).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgreementActionRequestResponse> getPendingRequests(String search, Pageable pageable) {
        String term = (search != null && !search.isBlank()) ? search.trim() : null;
        return actionRequestRepository.findAllPending(ActionRequestStatus.PENDING, term, pageable)
                .map(this::toResponse);
    }

    private AgreementVersion loadVersion(Long agreementVersionId) {
        return agreementVersionRepository.findById(agreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreementVersionId));
    }

    private void validateNoPendingRequest(Long agreementId) {
        actionRequestRepository
                .findFirstByAgreementVersion_Agreement_IdAndStatus(agreementId, ActionRequestStatus.PENDING)
                .ifPresent(existing -> {
                    throw new BusinessException(
                            "A pending " + existing.getActionType().name().toLowerCase()
                                    + " request already exists for this agreement");
                });
    }

    private AgreementActionRequestResponse toResponse(AgreementActionRequest r) {
        if (r.getActionType() == ActionRequestType.DELETE_GROUP) {
            var group = r.getCompanyAgreementGroup();
            return new AgreementActionRequestResponse(
                    r.getId(),
                    null,
                    null,
                    group.getId(),
                    group.getName(),
                    group.getCompany().getCompanyName(),
                    r.getActionType(),
                    r.getStatus(),
                    r.getRequestedBy().getId(),
                    r.getRequestedBy().getFullName(),
                    null,
                    null,
                    r.getReasonComments(),
                    null,
                    r.getApproverComments(),
                    r.getResolvedBy() != null ? r.getResolvedBy().getId() : null,
                    r.getResolvedBy() != null ? r.getResolvedBy().getFullName() : null,
                    r.getCreatedAt(),
                    r.getResolvedAt()
            );
        }

        AgreementVersion version = r.getAgreementVersion();
        var parent = version.getAgreement();
        var cag = parent.getCompanyAgreementGroup();
        return new AgreementActionRequestResponse(
                r.getId(),
                parent.getId(),
                version.getId(),
                null,
                parent.getAgreementName(),
                cag.getCompany().getCompanyName(),
                r.getActionType(),
                r.getStatus(),
                r.getRequestedBy().getId(),
                r.getRequestedBy().getFullName(),
                r.getTargetUser() != null ? r.getTargetUser().getId() : null,
                r.getTargetUser() != null ? r.getTargetUser().getFullName() : null,
                r.getReasonComments(),
                r.getRequestedTerminationDate(),
                r.getApproverComments(),
                r.getResolvedBy() != null ? r.getResolvedBy().getId() : null,
                r.getResolvedBy() != null ? r.getResolvedBy().getFullName() : null,
                r.getCreatedAt(),
                r.getResolvedAt()
        );
    }
}
