package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.GroupDeletionRequest;
import com.medplus.agreement_tracker_backend.dto.request.UpdateCompanyAgreementGroupRequest;
import com.medplus.agreement_tracker_backend.dto.response.CompanyAgreementGroupResponse;
import com.medplus.agreement_tracker_backend.dto.response.GroupDeletionStatusResponse;
import com.medplus.agreement_tracker_backend.entity.Agreement;
import com.medplus.agreement_tracker_backend.entity.AgreementActionRequest;
import com.medplus.agreement_tracker_backend.entity.AgreementAudit;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.entity.CompanyAgreementGroup;
import com.medplus.agreement_tracker_backend.entity.CompanyMaster;
import com.medplus.agreement_tracker_backend.entity.User;
import com.medplus.agreement_tracker_backend.enums.ActionRequestStatus;
import com.medplus.agreement_tracker_backend.enums.ActionRequestType;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.enums.GroupDeletionStatus;
import com.medplus.agreement_tracker_backend.enums.AgreementStatus;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.AgreementActionRequestRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementApprovalRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementAuditRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementComputedProductRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementDivisionRuleRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementDocumentRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementManufacturerRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementProductRuleRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementPurchaseSlabRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementReminderRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementSaleTargetRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementVendorRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementVersionRepository;
import com.medplus.agreement_tracker_backend.repository.CompanyAgreementGroupRepository;
import com.medplus.agreement_tracker_backend.repository.CompanyAgreementGroupSpec;
import com.medplus.agreement_tracker_backend.repository.CompanyMasterRepository;
import com.medplus.agreement_tracker_backend.repository.UserRepository;
import com.medplus.agreement_tracker_backend.service.CompanyAgreementGroupService;
import com.medplus.agreement_tracker_backend.util.AgreementStatusResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyAgreementGroupServiceImpl implements CompanyAgreementGroupService {

    private final CompanyAgreementGroupRepository companyAgreementGroupRepository;
    private final CompanyMasterRepository companyMasterRepository;
    private final AgreementRepository agreementRepository;
    private final AgreementVersionRepository agreementVersionRepository;
    private final AgreementActionRequestRepository actionRequestRepository;
    private final AgreementApprovalRepository approvalRepository;
    private final AgreementAuditRepository auditRepository;
    private final AgreementVendorRepository vendorRepository;
    private final AgreementManufacturerRepository manufacturerRuleRepository;
    private final AgreementDivisionRuleRepository divisionRuleRepository;
    private final AgreementProductRuleRepository productRuleRepository;
    private final AgreementPurchaseSlabRepository purchaseSlabRepository;
    private final AgreementSaleTargetRepository saleTargetRepository;
    private final AgreementComputedProductRepository computedProductRepository;
    private final AgreementReminderRepository reminderRepository;
    private final AgreementDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final AgreementStatusResolver agreementStatusResolver;

    private enum GroupAgreementState {
        BLOCKING,
        DRAFT_ONLY,
        DELETABLE
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyAgreementGroupResponse> listAll(
            Pageable pageable, Long companyId, Boolean isActive, String groupName,
            String lastModifiedBy, String createdBy, Long currentUserId, boolean canViewAll) {
        Specification<CompanyAgreementGroup> spec = CompanyAgreementGroupSpec.withFilters(
                companyId, isActive, groupName, lastModifiedBy, createdBy);
        if (!canViewAll) {
            spec = spec.and(CompanyAgreementGroupSpec.visibleTo(currentUserId));
        }
        return companyAgreementGroupRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyAgreementGroupResponse getById(Long groupId, Long currentUserId, boolean canViewAll) {
        CompanyAgreementGroup group = loadGroup(groupId);
        if (!canViewAll) {
            enforceGroupVisibility(group, currentUserId);
        }
        return toResponse(group);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyAgreementGroupResponse> listByCompanyId(Long companyId, Long currentUserId,
                                                               boolean canViewAll) {
        Specification<CompanyAgreementGroup> spec = Specification
                .<CompanyAgreementGroup>where((root, query, cb) -> cb.equal(root.get("company").get("id"), companyId))
                .and((root, query, cb) -> cb.equal(root.get("isActive"), true));
        if (!canViewAll) {
            spec = spec.and(CompanyAgreementGroupSpec.visibleTo(currentUserId));
        }
        return companyAgreementGroupRepository.findAll(spec).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CompanyAgreementGroupResponse resolveOrCreate(Long companyId, Long groupId, String newName, Long userId) {
        CompanyMaster company = companyMasterRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company", companyId));

        if (groupId != null) {
            CompanyAgreementGroup existing = loadGroup(groupId);
            if (!existing.getCompany().getId().equals(companyId)) {
                throw new BusinessException("Company agreement group does not belong to the specified company");
            }
            if (!existing.isActive()) {
                throw new BusinessException("Cannot add agreements to an inactive group.");
            }
            return toResponse(existing);
        }

        if (!StringUtils.hasText(newName)) {
            throw new BusinessException("Either companyAgreementGroupId or newCompanyAgreementGroupName is required");
        }

        String trimmedName = newName.trim();
        return companyAgreementGroupRepository.findByCompanyIdAndName(companyId, trimmedName)
                .map(this::toResponse)
                .orElseGet(() -> {
                    if (companyAgreementGroupRepository.existsByCompanyIdAndName(companyId, trimmedName)) {
                        throw new BusinessException("Company agreement group name already exists");
                    }
                    CompanyAgreementGroup created = CompanyAgreementGroup.builder()
                            .company(company)
                            .name(trimmedName)
                            .isActive(true)
                            .build();
                    created.setCreatedByUserId(userId);
                    return toResponse(companyAgreementGroupRepository.save(created));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public GroupDeletionStatusResponse getDeletionStatus(Long groupId, Long currentUserId,
                                                         boolean isAdmin, boolean isApprover) {
        CompanyAgreementGroup group = loadGroup(groupId);
        enforceDeleteAuthorization(group, currentUserId, isAdmin, isApprover);

        return switch (resolveGroupAgreementState(groupId)) {
            case BLOCKING -> new GroupDeletionStatusResponse(GroupDeletionStatus.HAS_ACTIVE);
            case DRAFT_ONLY -> {
                if (isAdmin || isApprover) {
                    yield new GroupDeletionStatusResponse(GroupDeletionStatus.ONLY_DRAFTS);
                }
                if (isCreator(group, currentUserId)) {
                    yield new GroupDeletionStatusResponse(GroupDeletionStatus.REQUIRES_APPROVAL);
                }
                throw new AccessDeniedException(
                        "Only the group creator, an approver, or an administrator can delete this group");
            }
            case DELETABLE -> {
                if (isAdmin || isApprover) {
                    yield new GroupDeletionStatusResponse(GroupDeletionStatus.READY);
                }
                if (isCreator(group, currentUserId)) {
                    yield new GroupDeletionStatusResponse(GroupDeletionStatus.REQUIRES_APPROVAL);
                }
                throw new AccessDeniedException(
                        "Only the group creator, an approver, or an administrator can delete this group");
            }
        };
    }

    @Override
    @Transactional
    public void deleteGroupImmediately(Long groupId, String reason, Long currentUserId,
                                       boolean isAdmin, boolean isApprover) {
        CompanyAgreementGroup group = loadGroup(groupId);
        enforceDeleteAuthorization(group, currentUserId, isAdmin, isApprover);

        GroupAgreementState agreementState = resolveGroupAgreementState(groupId);
        if (agreementState == GroupAgreementState.BLOCKING) {
            throw new BusinessException("Cannot delete group: Active agreements exist.");
        }

        GroupDeletionStatus status = getDeletionStatus(groupId, currentUserId, isAdmin, isApprover).status();
        if (status == GroupDeletionStatus.REQUIRES_APPROVAL) {
            throw new BusinessException("Group deletion requires approval. Submit a deletion request instead.");
        }
        if (status == GroupDeletionStatus.HAS_ACTIVE) {
            throw new BusinessException("Cannot delete group: Active agreements exist.");
        }

        validateReason(reason);
        performGroupDeletion(group, reason.trim(), currentUserId, agreementState);
    }

    @Override
    @Transactional
    public void submitDeletionRequest(Long groupId, GroupDeletionRequest request, Long currentUserId) {
        CompanyAgreementGroup group = loadGroup(groupId);
        enforceCreatorForDeletionRequest(group, currentUserId);

        GroupDeletionStatus status = getDeletionStatus(groupId, currentUserId, false, false).status();
        if (status != GroupDeletionStatus.REQUIRES_APPROVAL) {
            throw new BusinessException("This group does not require a deletion approval request");
        }

        validateNoPendingGroupDeletionRequest(groupId);
        validateReason(request.reason());

        User requestedBy = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        AgreementActionRequest actionRequest = AgreementActionRequest.builder()
                .companyAgreementGroup(group)
                .actionType(ActionRequestType.DELETE_GROUP)
                .status(ActionRequestStatus.PENDING)
                .requestedBy(requestedBy)
                .reasonComments(request.reason().trim())
                .build();
        actionRequestRepository.save(actionRequest);
    }

    @Override
    @Transactional
    public void executeApprovedGroupDeletion(Long groupId, Long approverId, String reason) {
        CompanyAgreementGroup group = loadGroup(groupId);
        GroupAgreementState agreementState = resolveGroupAgreementState(groupId);
        if (agreementState == GroupAgreementState.BLOCKING) {
            throw new BusinessException("Cannot delete group: Active agreements exist.");
        }

        String auditReason = StringUtils.hasText(reason) ? reason.trim() : "Approved group deletion";
        performGroupDeletion(group, auditReason, approverId, agreementState);
    }

    @Override
    @Transactional
    public CompanyAgreementGroupResponse renameGroup(Long groupId, UpdateCompanyAgreementGroupRequest request,
                                                     Long currentUserId) {
        CompanyAgreementGroup group = loadGroup(groupId);
        String trimmedName = request.name().trim();

        if (companyAgreementGroupRepository.existsByCompanyIdAndName(group.getCompany().getId(), trimmedName)
                && !group.getName().equalsIgnoreCase(trimmedName)) {
            throw new BusinessException("Company agreement group name already exists");
        }

        group.setName(trimmedName);
        group.setUpdatedByUserId(currentUserId);
        return toResponse(companyAgreementGroupRepository.save(group));
    }

    private void performGroupDeletion(CompanyAgreementGroup group, String reason, Long performedByUserId,
                                      GroupAgreementState agreementState) {
        Long groupId = group.getId();
        if (resolveGroupAgreementState(groupId) == GroupAgreementState.BLOCKING) {
            throw new BusinessException("Cannot delete group: Active agreements exist.");
        }

        if (agreementState == GroupAgreementState.DRAFT_ONLY) {
            List<Agreement> agreements = agreementRepository.findByCompanyAgreementGroupId(groupId);
            for (Agreement agreement : agreements) {
                hardDeleteAgreement(agreement);
            }
        }

        group.setActive(false);
        group.setUpdatedByUserId(performedByUserId);
        companyAgreementGroupRepository.save(group);

        recordGroupAudit(groupId, reason, performedByUserId);
    }

    private void hardDeleteAgreement(Agreement agreement) {
        List<AgreementVersion> versions = agreementVersionRepository.findByAgreementId(agreement.getId());
        for (AgreementVersion version : versions) {
            hardDeleteAgreementVersion(version.getId());
        }
        auditRepository.deleteByAgreementId(agreement.getId());
        agreementRepository.delete(agreement);
    }

    private void hardDeleteAgreementVersion(Long versionId) {
        saleTargetRepository.deleteByAgreementVersionId(versionId);
        purchaseSlabRepository.deleteByAgreementVersionId(versionId);
        vendorRepository.deleteByAgreementVersionId(versionId);
        manufacturerRuleRepository.deleteByAgreementVersionId(versionId);
        divisionRuleRepository.deleteByAgreementVersionId(versionId);
        productRuleRepository.deleteByAgreementVersionId(versionId);
        computedProductRepository.deleteByAgreementVersionId(versionId);
        approvalRepository.deleteByAgreementVersionId(versionId);
        reminderRepository.deleteByAgreementVersionId(versionId);
        documentRepository.deleteByAgreementVersionId(versionId);
        actionRequestRepository.deleteByAgreementVersionId(versionId);
        auditRepository.deleteByAgreementVersionId(versionId);
        agreementVersionRepository.deleteById(versionId);
    }

    private void recordGroupAudit(Long groupId, String reason, Long userId) {
        AgreementAudit audit = AgreementAudit.builder()
                .entityType("CompanyAgreementGroup")
                .entityId(groupId)
                .action("GROUP_DELETED")
                .newValueJson(reason)
                .createdByUserId(userId)
                .build();
        auditRepository.save(audit);
    }

    private void validateNoPendingGroupDeletionRequest(Long groupId) {
        actionRequestRepository.findFirstByCompanyAgreementGroup_IdAndStatus(groupId, ActionRequestStatus.PENDING)
                .ifPresent(existing -> {
                    throw new BusinessException("A pending group deletion request already exists");
                });
    }

    private void validateReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("Reason for deletion is required");
        }
    }

    private GroupAgreementState resolveGroupAgreementState(Long groupId) {
        List<Agreement> agreements = agreementRepository.findByCompanyAgreementGroupId(groupId);
        if (agreements.isEmpty()) {
            return GroupAgreementState.DELETABLE;
        }

        boolean allDraftOnly = true;
        for (Agreement agreement : agreements) {
            GroupAgreementState agreementState = classifyAgreement(agreement);
            if (agreementState == GroupAgreementState.BLOCKING) {
                return GroupAgreementState.BLOCKING;
            }
            if (agreementState != GroupAgreementState.DRAFT_ONLY) {
                allDraftOnly = false;
            }
        }

        return allDraftOnly ? GroupAgreementState.DRAFT_ONLY : GroupAgreementState.DELETABLE;
    }

    private GroupAgreementState classifyAgreement(Agreement agreement) {
        if (agreement.getCurrentVersionId() != null) {
            AgreementVersion current = agreementVersionRepository.findById(agreement.getCurrentVersionId())
                    .orElse(null);
            if (current != null) {
                return mapStatusToGroupState(agreementStatusResolver.resolve(current));
            }
        }

        List<AgreementVersion> versions = agreementVersionRepository.findByAgreementId(agreement.getId());
        if (versions.isEmpty()) {
            return GroupAgreementState.DELETABLE;
        }

        boolean allDraft = versions.stream()
                .allMatch(version -> version.getApprovalStatus() == ApprovalStatus.DRAFT);
        if (allDraft && agreement.getCurrentVersionId() == null) {
            return GroupAgreementState.DRAFT_ONLY;
        }

        boolean hasNonDraftHistory = versions.stream()
                .anyMatch(version -> version.getApprovalStatus() != ApprovalStatus.DRAFT);
        if (hasNonDraftHistory && agreement.getCurrentVersionId() == null) {
            return GroupAgreementState.BLOCKING;
        }

        AgreementVersion latest = versions.stream()
                .max((left, right) -> Integer.compare(left.getVersionNumber(), right.getVersionNumber()))
                .orElse(null);
        if (latest != null && latest.getApprovalStatus() == ApprovalStatus.DRAFT) {
            return GroupAgreementState.DRAFT_ONLY;
        }

        return GroupAgreementState.BLOCKING;
    }

    private GroupAgreementState mapStatusToGroupState(AgreementStatus status) {
        return switch (status) {
            case TERMINATED, EXPIRED -> GroupAgreementState.DELETABLE;
            case DRAFT -> GroupAgreementState.DRAFT_ONLY;
            default -> GroupAgreementState.BLOCKING;
        };
    }

    private void enforceGroupVisibility(CompanyAgreementGroup group, Long currentUserId) {
        if (isCreator(group, currentUserId)) {
            return;
        }
        if (agreementRepository.existsByCompanyAgreementGroupIdAndOwner_Id(group.getId(), currentUserId)) {
            return;
        }
        throw new AccessDeniedException("You do not have access to this group");
    }

    private void enforceDeleteAuthorization(CompanyAgreementGroup group, Long currentUserId,
                                            boolean isAdmin, boolean isApprover) {
        if (isAdmin || isApprover) {
            return;
        }
        if (isCreator(group, currentUserId)) {
            return;
        }
        throw new AccessDeniedException("Only the group creator, an approver, or an administrator can delete this group");
    }

    private void enforceCreatorForDeletionRequest(CompanyAgreementGroup group, Long currentUserId) {
        if (!isCreator(group, currentUserId)) {
            throw new AccessDeniedException("Only the group creator can submit a deletion request");
        }
    }

    private boolean isCreator(CompanyAgreementGroup group, Long userId) {
        return group.getCreatedByUserId() != null && group.getCreatedByUserId().equals(userId);
    }

    private CompanyAgreementGroup loadGroup(Long groupId) {
        return companyAgreementGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyAgreementGroup", groupId));
    }

    private CompanyAgreementGroupResponse toResponse(CompanyAgreementGroup group) {
        Long modifierUserId = group.getUpdatedByUserId() != null
                ? group.getUpdatedByUserId()
                : group.getCreatedByUserId();
        String modifierName = resolveUserName(modifierUserId);

        return new CompanyAgreementGroupResponse(
                group.getId(),
                group.getCompany().getId(),
                group.getCompany().getCompanyName(),
                group.getName(),
                group.isActive(),
                group.getCreatedByUserId(),
                resolveUserName(group.getCreatedByUserId()),
                group.getUpdatedAt(),
                modifierName
        );
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).map(User::getFullName).orElse(null);
    }
}
