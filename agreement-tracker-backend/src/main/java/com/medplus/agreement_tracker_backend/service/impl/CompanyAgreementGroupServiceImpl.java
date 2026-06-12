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

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyAgreementGroupResponse> listAll(
            Pageable pageable, Long companyId, Boolean isActive, String groupName,
            String lastModifiedBy, String createdBy, Long currentUserId, boolean canViewAll,
            boolean isApprover, boolean isAccountManager) {
        Specification<CompanyAgreementGroup> spec = CompanyAgreementGroupSpec.withFilters(
                companyId, isActive, groupName, lastModifiedBy, createdBy);
        if (!canViewAll) {
            spec = spec.and(CompanyAgreementGroupSpec.visibleTo(currentUserId));
        }
        return companyAgreementGroupRepository.findAll(spec, pageable)
                .map(group -> toResponse(group, currentUserId, isApprover, isAccountManager));
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyAgreementGroupResponse getById(Long groupId, Long currentUserId, boolean canViewAll,
                                                 boolean isApprover, boolean isAccountManager) {
        CompanyAgreementGroup group = loadGroup(groupId);
        if (!canViewAll) {
            enforceGroupVisibility(group, currentUserId);
        }
        return toResponse(group, currentUserId, isApprover, isAccountManager);
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
                .map(group -> toResponse(group, currentUserId, false, false))
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
            return toResponse(existing, userId, false, false);
        }

        if (!StringUtils.hasText(newName)) {
            throw new BusinessException("Either companyAgreementGroupId or newCompanyAgreementGroupName is required");
        }

        String trimmedName = newName.trim();
        return companyAgreementGroupRepository.findByCompanyIdAndName(companyId, trimmedName)
                .map(group -> toResponse(group, userId, false, false))
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
                    return toResponse(companyAgreementGroupRepository.save(created), userId, false, false);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public GroupDeletionStatusResponse getDeletionStatus(Long groupId, Long currentUserId,
                                                         boolean isApprover, boolean isAccountManager) {
        CompanyAgreementGroup group = loadGroup(groupId);
        enforceDeleteAuthorization(group, currentUserId, isApprover, isAccountManager);

        if (!areAllAgreementsDeletable(groupId)) {
            return new GroupDeletionStatusResponse(GroupDeletionStatus.HAS_ACTIVE);
        }

        if (isApprover) {
            return new GroupDeletionStatusResponse(GroupDeletionStatus.READY);
        }
        if (isAccountManager && isCreator(group, currentUserId)) {
            return new GroupDeletionStatusResponse(GroupDeletionStatus.REQUIRES_APPROVAL);
        }

        throw new AccessDeniedException(
                "Only an approver or the account manager who created this group can delete it");
    }

    @Override
    @Transactional
    public void deleteGroupImmediately(Long groupId, String reason, Long currentUserId,
                                       boolean isApprover, boolean isAccountManager) {
        CompanyAgreementGroup group = loadGroup(groupId);
        enforceDeleteAuthorization(group, currentUserId, isApprover, isAccountManager);

        if (!computeCanDelete(group, currentUserId, isApprover, isAccountManager)) {
            throw new BusinessException("Cannot delete group: Active agreements exist.");
        }

        GroupDeletionStatus status = getDeletionStatus(groupId, currentUserId, isApprover, isAccountManager).status();
        if (status == GroupDeletionStatus.REQUIRES_APPROVAL) {
            throw new BusinessException("Group deletion requires approval. Submit a deletion request instead.");
        }
        if (status == GroupDeletionStatus.HAS_ACTIVE) {
            throw new BusinessException("Cannot delete group: Active agreements exist.");
        }

        validateReason(reason);
        performGroupDeletion(group, reason.trim(), currentUserId);
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
        if (!areAllAgreementsDeletable(groupId)) {
            throw new BusinessException("Cannot delete group: Active agreements exist.");
        }

        String auditReason = StringUtils.hasText(reason) ? reason.trim() : "Approved group deletion";
        performGroupDeletion(group, auditReason, approverId);
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
        return toResponse(companyAgreementGroupRepository.save(group), currentUserId, false, false);
    }

    private void performGroupDeletion(CompanyAgreementGroup group, String reason, Long performedByUserId) {
        Long groupId = group.getId();
        if (!areAllAgreementsDeletable(groupId)) {
            throw new BusinessException("Cannot delete group: Active agreements exist.");
        }

        group.setActive(false);
        group.setUpdatedByUserId(performedByUserId);
        companyAgreementGroupRepository.save(group);

        recordGroupAudit(groupId, reason, performedByUserId);
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

    private boolean computeCanDelete(CompanyAgreementGroup group, Long currentUserId,
                                     boolean isApprover, boolean isAccountManager) {
        if (!hasDeleteRole(group, currentUserId, isApprover, isAccountManager)) {
            return false;
        }
        return areAllAgreementsDeletable(group.getId());
    }

    private boolean hasDeleteRole(CompanyAgreementGroup group, Long currentUserId,
                                  boolean isApprover, boolean isAccountManager) {
        if (isApprover) {
            return true;
        }
        return isAccountManager && isCreator(group, currentUserId);
    }

    private boolean areAllAgreementsDeletable(Long groupId) {
        List<Agreement> agreements = agreementRepository.findByCompanyAgreementGroupId(groupId);
        if (agreements.isEmpty()) {
            return true;
        }
        for (Agreement agreement : agreements) {
            if (!isAgreementDeletable(agreement)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAgreementDeletable(Agreement agreement) {
        AgreementVersion version = resolveAgreementVersionForStatus(agreement);
        if (version == null) {
            return true;
        }
        AgreementStatus status = agreementStatusResolver.resolve(version);
        return status == AgreementStatus.EXPIRED || status == AgreementStatus.TERMINATED;
    }

    private AgreementVersion resolveAgreementVersionForStatus(Agreement agreement) {
        if (agreement.getCurrentVersionId() != null) {
            return agreementVersionRepository.findById(agreement.getCurrentVersionId()).orElse(null);
        }
        List<AgreementVersion> versions = agreementVersionRepository.findByAgreementId(agreement.getId());
        if (versions.isEmpty()) {
            return null;
        }
        return versions.stream()
                .max((left, right) -> Integer.compare(left.getVersionNumber(), right.getVersionNumber()))
                .orElse(null);
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
                                            boolean isApprover, boolean isAccountManager) {
        if (!hasDeleteRole(group, currentUserId, isApprover, isAccountManager)) {
            throw new AccessDeniedException(
                    "Only an approver or the account manager who created this group can delete it");
        }
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

    private CompanyAgreementGroupResponse toResponse(CompanyAgreementGroup group, Long currentUserId,
                                                     boolean isApprover, boolean isAccountManager) {
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
                modifierName,
                computeCanDelete(group, currentUserId, isApprover, isAccountManager)
        );
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).map(User::getFullName).orElse(null);
    }
}
