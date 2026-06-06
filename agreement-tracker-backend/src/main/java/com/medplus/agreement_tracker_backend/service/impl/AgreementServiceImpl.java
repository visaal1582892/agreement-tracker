package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.CreateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.DraftAgreementItemRequest;
import com.medplus.agreement_tracker_backend.dto.request.DraftCommercialsPayload;
import com.medplus.agreement_tracker_backend.dto.request.DraftDetailsPayload;
import com.medplus.agreement_tracker_backend.dto.request.EditAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.RuleDTO;
import com.medplus.agreement_tracker_backend.dto.request.TerminateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.UpdateDraftRequest;
import com.medplus.agreement_tracker_backend.dto.request.ProductRulesPayload;
import com.medplus.agreement_tracker_backend.dto.response.AgreementGroupResponse;
import com.medplus.agreement_tracker_backend.dto.response.AgreementResponse;
import com.medplus.agreement_tracker_backend.dto.response.ApprovalTimelineResponse;
import com.medplus.agreement_tracker_backend.dto.response.BulkAgreementCreateResponse;
import com.medplus.agreement_tracker_backend.entity.*;
import com.medplus.agreement_tracker_backend.enums.ApprovalAction;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.enums.CommercialStructure;
import com.medplus.agreement_tracker_backend.enums.RuleType;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.exception.UnauthorizedException;
import com.medplus.agreement_tracker_backend.repository.*;
import com.medplus.agreement_tracker_backend.repository.AgreementGroupSpec;
import org.springframework.security.access.AccessDeniedException;
import com.medplus.agreement_tracker_backend.service.AgreementService;
import com.medplus.agreement_tracker_backend.util.AgreementNumberGenerator;
import com.medplus.agreement_tracker_backend.util.AgreementStatusResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgreementServiceImpl implements AgreementService {

    private final AgreementGroupRepository groupRepository;
    private final AgreementRepository agreementRepository;
    private final AgreementVendorRepository vendorRepository;
    private final AgreementManufacturerRepository manufacturerRuleRepository;
    private final AgreementDivisionRuleRepository divisionRuleRepository;
    private final AgreementProductRuleRepository productRuleRepository;
    private final AgreementComputedProductRepository computedProductRepository;
    private final AgreementApprovalRepository approvalRepository;
    private final AgreementAuditRepository auditRepository;
    private final UserRepository userRepository;
    private final CompanyMasterRepository companyRepository;
    private final IncomeTypeRepository incomeTypeRepository;
    private final AgreementTypeRepository agreementTypeRepository;
    private final VendorMasterRepository vendorMasterRepository;
    private final ProductMasterRepository productMasterRepository;
    private final UserCompanyAssignmentRepository companyAssignmentRepository;
    private final AgreementNumberGenerator numberGenerator;
    private final AgreementStatusResolver statusResolver;

    @Override
    @Transactional
    public BulkAgreementCreateResponse createDraft(CreateAgreementRequest request, Long currentUserId) {
        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));
        CompanyMaster company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", request.companyId()));

        List<Long> vendorIds = request.vendorIds() != null ? request.vendorIds() : List.of();
        ProductRulesPayload rulesPayload = request.productRules() != null
                ? request.productRules() : new ProductRulesPayload(null, null, null);
        List<Long> manufacturerIds = rulesPayload.manufacturers() != null ? rulesPayload.manufacturers() : List.of();
        List<RuleDTO> divisionRules = rulesPayload.divisionRules() != null ? rulesPayload.divisionRules() : List.of();
        List<RuleDTO> productRules = rulesPayload.productRules() != null ? rulesPayload.productRules() : List.of();

        List<DraftAgreementItemRequest> items = request.agreements() != null && !request.agreements().isEmpty()
                ? request.agreements()
                : List.of(new DraftAgreementItemRequest(null, null));

        List<AgreementResponse> created = new ArrayList<>();
        Long primaryGroupId = null;

        for (DraftAgreementItemRequest item : items) {
            String agreementNumber = numberGenerator.generate();
            AgreementGroup group = AgreementGroup.builder()
                    .company(company)
                    .agreementNumber(agreementNumber)
                    .isActive(true)
                    .build();
            group.setCreatedByUserId(currentUserId);
            group = groupRepository.save(group);

            Agreement agreement = buildDraftAgreement(item, owner, group, currentUserId);
            agreement = agreementRepository.save(agreement);

            replaceVendors(agreement, vendorIds, currentUserId);
            replaceRulesAndComputeProducts(agreement, manufacturerIds, divisionRules, productRules, currentUserId);

            recordAudit(group.getId(), agreement.getId(), "AGREEMENT_CREATED", null, agreementNumber, currentUserId);

            if (primaryGroupId == null) {
                primaryGroupId = group.getId();
            }
            created.add(toAgreementResponse(agreement));
        }

        return new BulkAgreementCreateResponse(created, primaryGroupId);
    }

    @Override
    @Transactional
    public AgreementResponse createNewVersion(Long agreementGroupId, Long currentUserId) {
        AgreementGroup group = groupRepository.findById(agreementGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementGroup", agreementGroupId));

        Agreement source = resolveNewVersionSource(group);
        loadAndValidateOwnership(source.getId(), currentUserId);

        Integer maxVersion = agreementRepository.findMaxVersionByGroupId(agreementGroupId);
        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        Agreement newVersion = Agreement.builder()
                .agreementGroup(group)
                .versionNumber(maxVersion + 1)
                .owner(owner)
                .incomeType(source.getIncomeType())
                .agreementType(source.getAgreementType())
                .commercialStructure(source.getCommercialStructure())
                .commercialValue(source.getCommercialValue())
                .calculationFormula(source.getCalculationFormula())
                .startDate(source.getStartDate())
                .expiryDate(source.getExpiryDate())
                .approvalStatus(ApprovalStatus.DRAFT)
                .notes(source.getNotes())
                .build();
        newVersion.setCreatedByUserId(currentUserId);
        newVersion = agreementRepository.save(newVersion);

        copyVendors(source.getId(), newVersion, currentUserId);
        copyRulesAndComputed(source.getId(), newVersion, currentUserId);

        recordAudit(group.getId(), newVersion.getId(), "NEW_VERSION_CREATED",
                String.valueOf(source.getVersionNumber()), String.valueOf(newVersion.getVersionNumber()), currentUserId);

        return toAgreementResponse(newVersion);
    }

    @Override
    @Transactional
    public AgreementResponse createVersionedEdit(Long sourceAgreementId, EditAgreementRequest request, Long currentUserId) {
        Agreement source = agreementRepository.findById(sourceAgreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", sourceAgreementId));

        if (source.getApprovalStatus() != ApprovalStatus.APPROVED
                && source.getApprovalStatus() != ApprovalStatus.REJECTED) {
            throw new BusinessException("Can only create a versioned edit from APPROVED or REJECTED agreements");
        }

        loadAndValidateOwnership(sourceAgreementId, currentUserId);

        AgreementGroup group = source.getAgreementGroup();
        Integer maxVersion = agreementRepository.findMaxVersionByGroupId(group.getId());
        Agreement latest = agreementRepository.findByAgreementGroupIdAndVersionNumber(group.getId(), maxVersion)
                .orElseThrow(() -> new BusinessException("No agreement version exists for this group"));

        if (latest.getApprovalStatus() == ApprovalStatus.DRAFT
                && latest.getOwner().getId().equals(currentUserId)) {
            throw new BusinessException("A draft version already exists — update it in place");
        }

        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        DraftAgreementItemRequest item = new DraftAgreementItemRequest(request.details(), request.commercials());
        Agreement newVersion = buildDraftAgreement(item, owner, group, maxVersion + 1, currentUserId);
        newVersion = agreementRepository.save(newVersion);

        List<Long> vendorIds = request.vendorIds() != null ? request.vendorIds() : List.of();
        ProductRulesPayload rulesPayload = request.productRules() != null
                ? request.productRules() : new ProductRulesPayload(null, null, null);
        List<Long> manufacturerIds = rulesPayload.manufacturers() != null ? rulesPayload.manufacturers() : List.of();
        List<RuleDTO> divisionRules = rulesPayload.divisionRules() != null ? rulesPayload.divisionRules() : List.of();
        List<RuleDTO> productRules = rulesPayload.productRules() != null ? rulesPayload.productRules() : List.of();

        replaceVendors(newVersion, vendorIds, currentUserId);
        replaceRulesAndComputeProducts(newVersion, manufacturerIds, divisionRules, productRules, currentUserId);

        recordAudit(group.getId(), newVersion.getId(), "VERSIONED_EDIT_CREATED",
                String.valueOf(source.getVersionNumber()), String.valueOf(newVersion.getVersionNumber()), currentUserId);

        return toAgreementResponse(newVersion);
    }

    @Override
    @Transactional
    public AgreementResponse updateDraft(Long agreementId, UpdateDraftRequest request, Long currentUserId) {
        Agreement agreement = loadAndValidateOwnership(agreementId, currentUserId);
        if (agreement.getApprovalStatus() != ApprovalStatus.DRAFT) {
            throw new BusinessException("Only DRAFT agreements can be updated via draft save");
        }

        AgreementGroup group = agreement.getAgreementGroup();
        if (request.companyId() != null) {
            CompanyMaster company = companyRepository.findById(request.companyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Company", request.companyId()));
            group.setCompany(company);
            group.setUpdatedByUserId(currentUserId);
            groupRepository.save(group);
        }

        applyDraftFields(agreement, request.details(), request.commercials());
        agreement.setUpdatedByUserId(currentUserId);
        agreement = agreementRepository.save(agreement);

        if (request.vendorIds() != null) {
            replaceVendors(agreement, request.vendorIds(), currentUserId);
        }

        if (request.productRules() != null) {
            ProductRulesPayload rulesPayload = request.productRules();
            List<Long> manufacturerIds = rulesPayload.manufacturers() != null ? rulesPayload.manufacturers() : List.of();
            List<RuleDTO> divisionRules = rulesPayload.divisionRules() != null ? rulesPayload.divisionRules() : List.of();
            List<RuleDTO> productRules = rulesPayload.productRules() != null ? rulesPayload.productRules() : List.of();
            replaceRulesAndComputeProducts(agreement, manufacturerIds, divisionRules, productRules, currentUserId);
        }

        recordAudit(group.getId(), agreement.getId(), "DRAFT_UPDATED", null, null, currentUserId);
        return toAgreementResponse(agreement);
    }

    /**
     * APPROVED amendments copy the active approved version; rejected revisions copy the latest rejected version.
     */
    private Agreement resolveNewVersionSource(AgreementGroup group) {
        Integer maxVersion = agreementRepository.findMaxVersionByGroupId(group.getId());
        Agreement latest = agreementRepository.findByAgreementGroupIdAndVersionNumber(group.getId(), maxVersion)
                .orElseThrow(() -> new BusinessException("No agreement version exists for this group"));

        if (latest.getApprovalStatus() == ApprovalStatus.REJECTED) {
            return latest;
        }

        if (group.getCurrentVersionId() == null) {
            throw new BusinessException("No active version exists to create a new version from");
        }

        Agreement current = agreementRepository.findById(group.getCurrentVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", group.getCurrentVersionId()));

        if (current.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException("Can only create new version from an APPROVED or REJECTED agreement");
        }

        return current;
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementResponse getAgreementById(Long agreementId, Long currentUserId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));
        enforceDraftVisibility(agreement, currentUserId);
        return toAgreementResponse(agreement);
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementGroupResponse getGroupById(Long groupId, Long currentUserId) {
        AgreementGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementGroup", groupId));
        enforceGroupDraftVisibility(group, currentUserId);
        return toGroupResponse(group, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgreementGroupResponse> getAllGroups(Pageable pageable, Long currentUserId, String scope,
                                                     boolean canViewAll, String agreementNumber,
                                                     String companyName, String status, String ownerName,
                                                     Long vendorId, Long incomeTypeId) {
        Pageable mappedPageable = mapGroupPageable(pageable);
        var filterSpec = AgreementGroupSpec.withFilters(agreementNumber, companyName, status, ownerName, vendorId, incomeTypeId)
                .and(AgreementGroupSpec.draftVisibleTo(currentUserId));

        var scopeSpec = "ALL".equalsIgnoreCase(scope) && canViewAll
                ? filterSpec
                : filterSpec.and(AgreementGroupSpec.ownedBy(currentUserId));

        Page<AgreementGroup> groupPage = groupRepository.findAll(scopeSpec, mappedPageable);

        List<Long> groupIds = groupPage.getContent().stream().map(AgreementGroup::getId).toList();
        if (groupIds.isEmpty()) {
            return groupPage.map(this::toGroupResponseEmpty);
        }

        // Single batch query for latest agreements — eliminates N+1 per-group lookup.
        Map<Long, Agreement> latestByGroupId = agreementRepository.findLatestVersionsForGroupIds(groupIds)
                .stream()
                .collect(Collectors.toMap(a -> a.getAgreementGroup().getId(), a -> a, (a, b) -> a));

        Map<Long, Agreement> visibleByGroupId = groupPage.getContent().stream()
                .collect(Collectors.toMap(
                        AgreementGroup::getId,
                        g -> resolveVisibleLatest(g, latestByGroupId.get(g.getId()), currentUserId),
                        (a, b) -> a
                ));

        List<Long> agreementIds = visibleByGroupId.values().stream()
                .filter(a -> a != null)
                .map(Agreement::getId)
                .toList();
        Map<Long, List<AgreementVendor>> vendorsByAgreementId = agreementIds.isEmpty()
                ? Map.of()
                : vendorRepository.findByAgreementIdIn(agreementIds)
                        .stream()
                        .collect(Collectors.groupingBy(v -> v.getAgreement().getId()));

        return groupPage.map(g -> {
            Agreement visible = visibleByGroupId.get(g.getId());
            List<AgreementVendor> vendors = visible != null
                    ? vendorsByAgreementId.getOrDefault(visible.getId(), List.of())
                    : List.of();
            return toGroupResponseFromBatch(g, visible, vendors);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgreementResponse> getVersionsByGroup(Long groupId, Long currentUserId) {
        AgreementGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementGroup", groupId));
        enforceGroupDraftVisibility(group, currentUserId);
        return agreementRepository.findByAgreementGroupId(groupId)
                .stream()
                .filter(a -> a.getApprovalStatus() != ApprovalStatus.DRAFT
                        || a.getOwner().getId().equals(currentUserId))
                .sorted((a, b) -> Integer.compare(a.getVersionNumber(), b.getVersionNumber()))
                .map(this::toAgreementResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgreementResponse> getVersionsByAgreementId(Long agreementId, Long currentUserId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));
        enforceDraftVisibility(agreement, currentUserId);
        return getVersionsByGroup(agreement.getAgreementGroup().getId(), currentUserId);
    }

    @Override
    @Transactional
    public AgreementResponse transferOwnership(Long agreementId, Long newOwnerUserId,
                                               Long performedByUserId, boolean isAdmin) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));

        Long currentOwnerId = agreement.getOwner().getId();
        if (!isAdmin && !currentOwnerId.equals(performedByUserId)) {
            throw new UnauthorizedException("Only the owner or an admin can transfer ownership");
        }

        User newOwner = userRepository.findById(newOwnerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", newOwnerUserId));

        if (newOwnerUserId.equals(currentOwnerId)) {
            throw new BusinessException("Agreement is already owned by this user");
        }

        Long groupId = agreement.getAgreementGroup().getId();
        agreementRepository.findByAgreementGroupId(groupId).stream()
                .filter(a -> a.getOwner().getId().equals(currentOwnerId))
                .forEach(a -> {
                    a.setOwner(newOwner);
                    a.setUpdatedByUserId(performedByUserId);
                    agreementRepository.save(a);
                    recordAudit(groupId, a.getId(), "OWNERSHIP_TRANSFERRED",
                            String.valueOf(currentOwnerId), String.valueOf(newOwnerUserId), performedByUserId);
                });

        return toAgreementResponse(agreementRepository.findById(agreementId).orElseThrow());
    }

    @Override
    @Transactional
    public AgreementResponse submitForApproval(Long agreementId, Long currentUserId) {
        Agreement agreement = loadAndValidateOwnership(agreementId, currentUserId);
        agreement = applySubmitForApproval(agreement, currentUserId);
        return toAgreementResponse(agreement);
    }

    private Agreement applySubmitForApproval(Agreement agreement, Long userId) {
        if (agreement.getApprovalStatus() != ApprovalStatus.DRAFT) {
            throw new BusinessException("Only DRAFT agreements can be submitted for approval");
        }
        validateReadyForSubmission(agreement);

        ApprovalStatus before = agreement.getApprovalStatus();
        agreement.setApprovalStatus(ApprovalStatus.PENDING_APPROVAL);
        agreement.setUpdatedByUserId(userId);
        agreement = agreementRepository.save(agreement);

        recordApproval(agreement, ApprovalAction.SUBMITTED, null, before, ApprovalStatus.PENDING_APPROVAL, userId);
        recordAudit(agreement.getAgreementGroup().getId(), agreement.getId(), "SUBMITTED_FOR_APPROVAL",
                before.name(), ApprovalStatus.PENDING_APPROVAL.name(), userId);

        return agreement;
    }

    @Override
    @Transactional
    public AgreementResponse approve(Long agreementId, String remarks, Long approverId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));

        if (agreement.getApprovalStatus() != ApprovalStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only PENDING_APPROVAL agreements can be approved");
        }
        if (agreement.getOwner().getId().equals(approverId)) {
            throw new AccessDeniedException("Separation of Duties violation: Cannot approve your own agreement.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("User", approverId));

        ApprovalStatus before = agreement.getApprovalStatus();
        agreement.setApprovalStatus(ApprovalStatus.APPROVED);
        agreement.setApprovedBy(approver);
        agreement.setApprovalDate(LocalDateTime.now());
        agreement.setUpdatedByUserId(approverId);
        agreement = agreementRepository.save(agreement);

        AgreementGroup group = agreement.getAgreementGroup();
        group.setCurrentVersionId(agreement.getId());
        group.setUpdatedByUserId(approverId);
        groupRepository.save(group);

        recordApproval(agreement, ApprovalAction.APPROVED, remarks, before, ApprovalStatus.APPROVED, approverId);
        recordAudit(group.getId(), agreementId, "APPROVED", before.name(), ApprovalStatus.APPROVED.name(), approverId);

        return toAgreementResponse(agreement);
    }

    @Override
    @Transactional
    public AgreementResponse reject(Long agreementId, String remarks, Long approverId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));

        if (agreement.getApprovalStatus() != ApprovalStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only PENDING_APPROVAL agreements can be rejected");
        }
        if (agreement.getOwner().getId().equals(approverId)) {
            throw new AccessDeniedException("Separation of Duties violation: Cannot approve your own agreement.");
        }

        ApprovalStatus before = agreement.getApprovalStatus();
        agreement.setApprovalStatus(ApprovalStatus.REJECTED);
        agreement.setUpdatedByUserId(approverId);
        agreement = agreementRepository.save(agreement);

        recordApproval(agreement, ApprovalAction.REJECTED, remarks, before, ApprovalStatus.REJECTED, approverId);
        recordAudit(agreement.getAgreementGroup().getId(), agreementId, "REJECTED", before.name(), ApprovalStatus.REJECTED.name(), approverId);

        return toAgreementResponse(agreement);
    }

    @Override
    @Transactional
    public AgreementResponse terminate(Long agreementId, TerminateAgreementRequest request, Long currentUserId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));

        if (agreement.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException("Only APPROVED agreements can be terminated");
        }

        agreement.setTerminationDate(request.terminationDate());
        agreement.setTerminationReason(request.terminationReason());
        agreement.setUpdatedByUserId(currentUserId);
        agreement = agreementRepository.save(agreement);

        recordAudit(agreement.getAgreementGroup().getId(), agreementId, "TERMINATED", null, request.terminationReason(), currentUserId);

        return toAgreementResponse(agreement);
    }

    @Override
    @Transactional
    public AgreementResponse toggleInProgress(Long agreementId, boolean inProgress, Long currentUserId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));

        agreement.setInProgressFlag(inProgress);
        agreement.setUpdatedByUserId(currentUserId);
        agreement = agreementRepository.save(agreement);

        return toAgreementResponse(agreement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgreementResponse> getPendingApprovals(String search, Pageable pageable) {
        String term = (search != null && !search.isBlank()) ? search.trim() : null;
        return agreementRepository.findAllPendingApproval(term, pageable)
                .map(this::toAgreementResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalTimelineResponse> getApprovalTimeline(Long agreementId) {
        return approvalRepository.findByAgreementIdOrderByCreatedAtAsc(agreementId)
                .stream()
                .map(a -> new ApprovalTimelineResponse(
                        a.getId(), a.getAction(), a.getRemarks(),
                        a.getApprovalStatusBefore(), a.getApprovalStatusAfter(),
                        a.getCreatedByUserId(),
                        null,
                        a.getCreatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void bulkTransferOwnership(Long fromUserId, Long toUserId, List<Long> groupIds, Long performedByUserId) {
        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", toUserId));

        List<Agreement> toTransfer;
        if (groupIds != null && !groupIds.isEmpty()) {
            toTransfer = groupIds.stream()
                    .flatMap(gid -> agreementRepository.findByAgreementGroupId(gid).stream())
                    .filter(a -> a.getOwner().getId().equals(fromUserId))
                    .filter(a -> a.getTerminationDate() == null)
                    .toList();
        } else {
            toTransfer = agreementRepository.findByOwnerId(fromUserId, Pageable.unpaged())
                    .stream()
                    .filter(a -> a.getTerminationDate() == null)
                    .toList();
        }

        for (Agreement a : toTransfer) {
            a.setOwner(toUser);
            a.setUpdatedByUserId(performedByUserId);
            agreementRepository.save(a);
            recordAudit(a.getAgreementGroup().getId(), a.getId(), "OWNERSHIP_TRANSFERRED",
                    String.valueOf(fromUserId), String.valueOf(toUserId), performedByUserId);
        }
    }

    private Agreement loadAndValidateOwnership(Long agreementId, Long userId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));
        if (!agreement.getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("You are not the owner of this agreement");
        }
        return agreement;
    }

    private void enforceDraftVisibility(Agreement agreement, Long currentUserId) {
        if (agreement.getApprovalStatus() == ApprovalStatus.DRAFT
                && !agreement.getOwner().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("Agreement", agreement.getId());
        }
    }

    private void enforceGroupDraftVisibility(AgreementGroup group, Long currentUserId) {
        List<Agreement> latestBatch = agreementRepository.findLatestVersionsForGroupIds(List.of(group.getId()));
        Agreement latest = latestBatch.isEmpty() ? null : latestBatch.get(0);
        if (latest != null
                && latest.getApprovalStatus() == ApprovalStatus.DRAFT
                && !latest.getOwner().getId().equals(currentUserId)
                && group.getCurrentVersionId() == null) {
            throw new ResourceNotFoundException("AgreementGroup", group.getId());
        }
    }

    private Agreement resolveVisibleLatest(AgreementGroup group, Agreement latest, Long currentUserId) {
        if (latest == null) {
            return null;
        }
        if (latest.getApprovalStatus() != ApprovalStatus.DRAFT
                || latest.getOwner().getId().equals(currentUserId)) {
            return latest;
        }
        if (group.getCurrentVersionId() != null) {
            return agreementRepository.findById(group.getCurrentVersionId()).orElse(latest);
        }
        return agreementRepository.findByAgreementGroupId(group.getId()).stream()
                .filter(a -> a.getApprovalStatus() != ApprovalStatus.DRAFT
                        || a.getOwner().getId().equals(currentUserId))
                .max((a, b) -> Integer.compare(a.getVersionNumber(), b.getVersionNumber()))
                .orElse(null);
    }

    private Agreement buildDraftAgreement(DraftAgreementItemRequest item, User owner,
                                            AgreementGroup group, Long userId) {
        return buildDraftAgreement(item, owner, group, 1, userId);
    }

    private Agreement buildDraftAgreement(DraftAgreementItemRequest item, User owner,
                                            AgreementGroup group, int versionNumber, Long userId) {
        DraftDetailsPayload details = item != null ? item.details() : null;
        DraftCommercialsPayload commercials = item != null ? item.commercials() : null;

        if (details != null && details.startDate() != null && details.expiryDate() != null
                && details.expiryDate().isBefore(details.startDate())) {
            throw new BusinessException("Expiry date must be on or after start date");
        }

        IncomeType incomeType = null;
        if (details != null && details.incomeTypeId() != null) {
            incomeType = incomeTypeRepository.findById(details.incomeTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("IncomeType", details.incomeTypeId()));
        }
        AgreementType agreementType = null;
        if (details != null && details.agreementTypeId() != null) {
            agreementType = agreementTypeRepository.findById(details.agreementTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("AgreementType", details.agreementTypeId()));
        }

        Agreement agreement = Agreement.builder()
                .agreementGroup(group)
                .versionNumber(versionNumber)
                .owner(owner)
                .incomeType(incomeType)
                .agreementType(agreementType)
                .commercialStructure(commercials != null ? commercials.commercialStructure() : null)
                .commercialValue(commercials != null ? commercials.commercialValue() : null)
                .calculationFormula(commercials != null ? commercials.calculationFormula() : null)
                .startDate(details != null ? details.startDate() : null)
                .expiryDate(details != null ? details.expiryDate() : null)
                .approvalStatus(ApprovalStatus.DRAFT)
                .notes(details != null ? details.notes() : null)
                .build();
        agreement.setCreatedByUserId(userId);
        return agreement;
    }

    private void applyDraftFields(Agreement agreement, DraftDetailsPayload details,
                                  DraftCommercialsPayload commercials) {
        if (details != null) {
            if (details.startDate() != null && details.expiryDate() != null
                    && details.expiryDate().isBefore(details.startDate())) {
                throw new BusinessException("Expiry date must be on or after start date");
            }
            if (details.incomeTypeId() != null) {
                agreement.setIncomeType(incomeTypeRepository.findById(details.incomeTypeId())
                        .orElseThrow(() -> new ResourceNotFoundException("IncomeType", details.incomeTypeId())));
            }
            if (details.agreementTypeId() != null) {
                agreement.setAgreementType(agreementTypeRepository.findById(details.agreementTypeId())
                        .orElseThrow(() -> new ResourceNotFoundException("AgreementType", details.agreementTypeId())));
            }
            if (details.startDate() != null) {
                agreement.setStartDate(details.startDate());
            }
            if (details.expiryDate() != null) {
                agreement.setExpiryDate(details.expiryDate());
            }
            if (details.notes() != null) {
                agreement.setNotes(details.notes());
            }
        }
        if (commercials != null) {
            if (commercials.commercialStructure() != null) {
                agreement.setCommercialStructure(commercials.commercialStructure());
            }
            if (commercials.commercialValue() != null) {
                agreement.setCommercialValue(commercials.commercialValue());
            }
            if (commercials.calculationFormula() != null) {
                agreement.setCalculationFormula(commercials.calculationFormula());
            }
        }
    }

    private void validateReadyForSubmission(Agreement agreement) {
        if (agreement.getIncomeType() == null) {
            throw new BusinessException("Income type is required before submission");
        }
        if (agreement.getAgreementType() == null) {
            throw new BusinessException("Agreement type is required before submission");
        }
        if (agreement.getStartDate() == null || agreement.getExpiryDate() == null) {
            throw new BusinessException("Start and expiry dates are required before submission");
        }
        if (agreement.getExpiryDate().isBefore(agreement.getStartDate())) {
            throw new BusinessException("Expiry date must be on or after start date");
        }
        if (agreement.getCommercialStructure() == null) {
            throw new BusinessException("Commercial structure is required before submission");
        }
        if (agreement.getCommercialStructure() == CommercialStructure.FLAT
                && agreement.getCommercialValue() == null) {
            throw new BusinessException("Commercial value is required for FLAT structure before submission");
        }
        if (vendorRepository.findByAgreementId(agreement.getId()).isEmpty()) {
            throw new BusinessException("At least one vendor is required before submission");
        }
        if (productRuleRepository.findByAgreementId(agreement.getId()).isEmpty()) {
            throw new BusinessException("At least one product rule is required before submission");
        }
    }

    private void replaceVendors(Agreement agreement, List<Long> vendorIds, Long userId) {
        vendorRepository.deleteByAgreementId(agreement.getId());
        if (vendorIds != null && !vendorIds.isEmpty()) {
            saveVendors(agreement, vendorIds, userId);
        }
    }

    private void replaceRulesAndComputeProducts(Agreement agreement, List<Long> manufacturerIds,
                                                List<RuleDTO> divisionRules, List<RuleDTO> productRules,
                                                Long userId) {
        manufacturerRuleRepository.deleteByAgreementId(agreement.getId());
        divisionRuleRepository.deleteByAgreementId(agreement.getId());
        productRuleRepository.deleteByAgreementId(agreement.getId());
        computedProductRepository.deleteByAgreementId(agreement.getId());
        saveRulesAndComputeProducts(agreement, manufacturerIds, divisionRules, productRules, userId);
    }

    private void saveVendors(Agreement agreement, List<Long> vendorIds, Long userId) {
        List<VendorMaster> vendors = vendorMasterRepository.findByIdIn(vendorIds);
        for (VendorMaster vendor : vendors) {
            AgreementVendor av = AgreementVendor.builder()
                    .agreement(agreement)
                    .vendorId(vendor.getId())
                    .vendorNameSnapshot(vendor.getVendorName())
                    .build();
            av.setCreatedByUserId(userId);
            vendorRepository.save(av);
        }
    }

    private void saveRulesAndComputeProducts(Agreement agreement, List<Long> manufacturerIds,
                                              List<RuleDTO> divisionRules, List<RuleDTO> productRules, Long userId) {
        List<Long> safeManufacturerIds = manufacturerIds != null ? manufacturerIds : List.of();
        for (Long mfrId : safeManufacturerIds) {
            AgreementManufacturer am = AgreementManufacturer.builder()
                    .agreement(agreement).manufacturerId(mfrId).build();
            am.setCreatedByUserId(userId);
            manufacturerRuleRepository.save(am);
        }

        List<RuleDTO> safeDivisionRules = divisionRules != null ? divisionRules : List.of();
        for (RuleDTO dr : safeDivisionRules) {
            AgreementDivisionRule adr = AgreementDivisionRule.builder()
                    .agreement(agreement).divisionId(dr.id())
                    .ruleType(RuleType.valueOf(dr.ruleType())).build();
            adr.setCreatedByUserId(userId);
            divisionRuleRepository.save(adr);
        }

        List<RuleDTO> safeProductRules = productRules != null ? productRules : List.of();
        for (RuleDTO pr : safeProductRules) {
            AgreementProductRule apr = AgreementProductRule.builder()
                    .agreement(agreement).productId(pr.id())
                    .ruleType(RuleType.valueOf(pr.ruleType())).build();
            apr.setCreatedByUserId(userId);
            productRuleRepository.save(apr);
        }

        List<Long> vendorIds = vendorRepository.findByAgreementId(agreement.getId())
                .stream().map(AgreementVendor::getVendorId).toList();
        if (vendorIds.isEmpty()) return;

        List<ProductMaster> baseProducts = safeManufacturerIds.isEmpty()
                ? productMasterRepository.findByVendorIds(vendorIds)
                : productMasterRepository.findByVendorIdsAndManufacturerIds(vendorIds, safeManufacturerIds);

        List<ProductMaster> afterDivisionFilter = applyDivisionRules(baseProducts, safeDivisionRules);
        List<ProductMaster> finalProducts = applyProductRules(afterDivisionFilter, safeProductRules);

        for (ProductMaster p : finalProducts) {
            AgreementComputedProduct acp = AgreementComputedProduct.builder()
                    .agreement(agreement)
                    .productId(p.getId())
                    .productNameSnapshot(p.getProductName())
                    .divisionNameSnapshot(p.getDivision().getDivisionName())
                    .manufacturerNameSnapshot(p.getManufacturer().getManufacturerName())
                    .build();
            acp.setCreatedByUserId(userId);
            computedProductRepository.save(acp);
        }
    }

    private List<ProductMaster> applyDivisionRules(List<ProductMaster> products, List<RuleDTO> rules) {
        if (rules.isEmpty()) return products;
        RuleType ruleType = RuleType.valueOf(rules.get(0).ruleType());
        Set<Long> divisionIds = new HashSet<>();
        rules.forEach(r -> divisionIds.add(r.id()));
        if (ruleType == RuleType.INCLUDE) {
            return products.stream().filter(p -> divisionIds.contains(p.getDivision().getId())).toList();
        }
        return products.stream().filter(p -> !divisionIds.contains(p.getDivision().getId())).toList();
    }

    private List<ProductMaster> applyProductRules(List<ProductMaster> products, List<RuleDTO> rules) {
        if (rules.isEmpty()) return products;
        RuleType ruleType = RuleType.valueOf(rules.get(0).ruleType());
        Set<Long> productIds = new HashSet<>();
        rules.forEach(r -> productIds.add(r.id()));
        if (ruleType == RuleType.INCLUDE) {
            return products.stream().filter(p -> productIds.contains(p.getId())).toList();
        }
        return products.stream().filter(p -> !productIds.contains(p.getId())).toList();
    }

    private void copyVendors(Long sourceAgreementId, Agreement target, Long userId) {
        vendorRepository.findByAgreementId(sourceAgreementId).forEach(v -> {
            AgreementVendor copy = AgreementVendor.builder()
                    .agreement(target)
                    .vendorId(v.getVendorId())
                    .vendorNameSnapshot(v.getVendorNameSnapshot())
                    .build();
            copy.setCreatedByUserId(userId);
            vendorRepository.save(copy);
        });
    }

    private void copyRulesAndComputed(Long sourceAgreementId, Agreement target, Long userId) {
        manufacturerRuleRepository.findByAgreementId(sourceAgreementId).forEach(m -> {
            AgreementManufacturer copy = AgreementManufacturer.builder()
                    .agreement(target).manufacturerId(m.getManufacturerId()).build();
            copy.setCreatedByUserId(userId);
            manufacturerRuleRepository.save(copy);
        });

        divisionRuleRepository.findByAgreementId(sourceAgreementId).forEach(dr -> {
            AgreementDivisionRule copy = AgreementDivisionRule.builder()
                    .agreement(target).divisionId(dr.getDivisionId()).ruleType(dr.getRuleType()).build();
            copy.setCreatedByUserId(userId);
            divisionRuleRepository.save(copy);
        });

        productRuleRepository.findByAgreementId(sourceAgreementId).forEach(pr -> {
            AgreementProductRule copy = AgreementProductRule.builder()
                    .agreement(target).productId(pr.getProductId()).ruleType(pr.getRuleType()).build();
            copy.setCreatedByUserId(userId);
            productRuleRepository.save(copy);
        });

        computedProductRepository.findByAgreementId(sourceAgreementId).forEach(cp -> {
            AgreementComputedProduct copy = AgreementComputedProduct.builder()
                    .agreement(target)
                    .productId(cp.getProductId())
                    .productNameSnapshot(cp.getProductNameSnapshot())
                    .divisionNameSnapshot(cp.getDivisionNameSnapshot())
                    .manufacturerNameSnapshot(cp.getManufacturerNameSnapshot())
                    .build();
            copy.setCreatedByUserId(userId);
            computedProductRepository.save(copy);
        });
    }

    private void recordApproval(Agreement agreement, ApprovalAction action, String remarks,
                                 ApprovalStatus before, ApprovalStatus after, Long userId) {
        AgreementApproval approval = AgreementApproval.builder()
                .agreement(agreement)
                .action(action)
                .remarks(remarks)
                .approvalStatusBefore(before)
                .approvalStatusAfter(after)
                .build();
        approval.setCreatedByUserId(userId);
        approvalRepository.save(approval);
    }

    private void recordAudit(Long groupId, Long agreementId, String action, String oldVal, String newVal, Long userId) {
        AgreementAudit audit = AgreementAudit.builder()
                .agreementGroupId(groupId)
                .agreementId(agreementId)
                .entityType("Agreement")
                .action(action)
                .oldValueJson(oldVal)
                .newValueJson(newVal)
                .createdByUserId(userId)
                .build();
        auditRepository.save(audit);
    }

    private AgreementResponse toAgreementResponse(Agreement a) {
        List<AgreementResponse.VendorSummary> vendors = vendorRepository.findByAgreementId(a.getId())
                .stream()
                .map(v -> new AgreementResponse.VendorSummary(v.getVendorId(), v.getVendorNameSnapshot()))
                .toList();

        List<Long> manufacturerIds = manufacturerRuleRepository.findByAgreementId(a.getId())
                .stream().map(AgreementManufacturer::getManufacturerId).toList();

        List<AgreementResponse.RuleSummary> divisionRules = divisionRuleRepository.findByAgreementId(a.getId())
                .stream()
                .map(dr -> new AgreementResponse.RuleSummary(dr.getDivisionId(), dr.getRuleType().name()))
                .toList();

        List<AgreementResponse.RuleSummary> productRules = productRuleRepository.findByAgreementId(a.getId())
                .stream()
                .map(pr -> new AgreementResponse.RuleSummary(pr.getProductId(), pr.getRuleType().name()))
                .toList();

        List<AgreementResponse.ProductSummary> products = computedProductRepository.findByAgreementId(a.getId())
                .stream()
                .map(p -> new AgreementResponse.ProductSummary(
                        p.getProductId(), p.getProductNameSnapshot(),
                        p.getManufacturerNameSnapshot(), p.getDivisionNameSnapshot()))
                .toList();

        return new AgreementResponse(
                a.getId(),
                a.getAgreementGroup().getId(),
                a.getAgreementGroup().getAgreementNumber(),
                a.getVersionNumber(),
                a.getAgreementGroup().getCompany().getId(),
                a.getAgreementGroup().getCompany().getCompanyName(),
                a.getOwner().getId(),
                a.getOwner().getFullName(),
                a.getIncomeType() != null ? a.getIncomeType().getId() : null,
                a.getIncomeType() != null ? a.getIncomeType().getName() : null,
                a.getAgreementType() != null ? a.getAgreementType().getId() : null,
                a.getAgreementType() != null ? a.getAgreementType().getName() : null,
                a.getCommercialStructure(),
                a.getCommercialValue(),
                a.getCalculationFormula(),
                a.getStartDate(),
                a.getExpiryDate(),
                a.getApprovalStatus(),
                statusResolver.resolve(a),
                a.isInProgressFlag(),
                a.getTerminationDate(),
                a.getTerminationReason(),
                a.getNotes(),
                vendors,
                manufacturerIds,
                divisionRules,
                productRules,
                products,
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    /**
     * Builds AgreementGroupResponse from pre-fetched batch data — no additional DB calls.
     * Used by getAllGroups() to eliminate N+1 queries.
     */
    private AgreementGroupResponse toGroupResponseFromBatch(AgreementGroup g, Agreement latest,
                                                            List<AgreementVendor> vendors) {
        if (latest == null) {
            return toGroupResponseEmpty(g);
        }
        List<AgreementResponse.VendorSummary> vendorSummaries = vendors.stream()
                .map(v -> new AgreementResponse.VendorSummary(v.getVendorId(), v.getVendorNameSnapshot()))
                .toList();
        return new AgreementGroupResponse(
                g.getId(),
                g.getAgreementNumber(),
                g.getCompany().getId(),
                g.getCompany().getCompanyName(),
                g.getCurrentVersionId(),
                latest.getId(),
                latest.getVersionNumber(),
                statusResolver.resolve(latest),
                g.isActive(),
                g.getCreatedAt(),
                latest.getIncomeType() != null ? latest.getIncomeType().getName() : null,
                latest.getStartDate(),
                latest.getExpiryDate(),
                latest.getOwner().getFullName(),
                latest.getOwner().getId(),
                vendorSummaries
        );
    }

    /** Used for single-group detail fetch — still calls toAgreementResponse (acceptable for single record). */
    private AgreementGroupResponse toGroupResponse(AgreementGroup g, Long currentUserId) {
        List<Agreement> latestBatch = agreementRepository.findLatestVersionsForGroupIds(List.of(g.getId()));
        Agreement latest = latestBatch.isEmpty() ? null : latestBatch.get(0);
        Agreement displayVersion = resolveVisibleLatest(g, latest, currentUserId);
        if (displayVersion == null) {
            return toGroupResponseEmpty(g);
        }
        AgreementResponse vr = toAgreementResponse(displayVersion);
        return new AgreementGroupResponse(
                g.getId(), g.getAgreementNumber(), g.getCompany().getId(), g.getCompany().getCompanyName(),
                g.getCurrentVersionId(), displayVersion.getId(), vr.versionNumber(), vr.derivedStatus(), g.isActive(), g.getCreatedAt(),
                vr.incomeTypeName(), vr.startDate(), vr.expiryDate(), vr.ownerName(), vr.ownerId(), vr.vendors()
        );
    }

    private AgreementGroupResponse toGroupResponseEmpty(AgreementGroup g) {
        return new AgreementGroupResponse(
                g.getId(), g.getAgreementNumber(), g.getCompany().getId(), g.getCompany().getCompanyName(),
                null, null, null, null, g.isActive(), g.getCreatedAt(),
                null, null, null, null, null, List.of()
        );
    }

    private Pageable mapGroupPageable(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }
        List<Sort.Order> orders = pageable.getSort().stream()
                .map(order -> {
                    String property = switch (order.getProperty()) {
                        case "companyName" -> "company.companyName";
                        case "agreementNumber" -> "agreementNumber";
                        case "createdAt" -> "createdAt";
                        default -> order.getProperty();
                    };
                    return new Sort.Order(order.getDirection(), property);
                })
                .toList();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }
}
