package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.BulkAgreementItemRequest;
import com.medplus.agreement_tracker_backend.dto.request.CreateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.EditAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.RuleDTO;
import com.medplus.agreement_tracker_backend.dto.request.TerminateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.response.AgreementGroupResponse;
import com.medplus.agreement_tracker_backend.dto.response.AgreementResponse;
import com.medplus.agreement_tracker_backend.dto.response.ApprovalTimelineResponse;
import com.medplus.agreement_tracker_backend.dto.response.BulkAgreementCreateResponse;
import com.medplus.agreement_tracker_backend.entity.*;
import com.medplus.agreement_tracker_backend.enums.ApprovalAction;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
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

        List<Long> manufacturerIds = request.productRules().manufacturers() != null
                ? request.productRules().manufacturers()
                : List.of();
        List<RuleDTO> divisionRules = request.productRules().divisionRules();
        List<RuleDTO> productRules = request.productRules().productRules();

        List<AgreementResponse> created = new ArrayList<>();
        Long primaryGroupId = null;

        for (BulkAgreementItemRequest item : request.agreements()) {
            var details = item.details();
            var commercials = item.commercials();

            if (details.expiryDate().isBefore(details.startDate())) {
                throw new BusinessException("Expiry date must be on or after start date");
            }

            IncomeType incomeType = incomeTypeRepository.findById(details.incomeTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("IncomeType", details.incomeTypeId()));
            AgreementType agreementType = agreementTypeRepository.findById(details.agreementTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("AgreementType", details.agreementTypeId()));

            String agreementNumber = numberGenerator.generate();
            AgreementGroup group = AgreementGroup.builder()
                    .company(company)
                    .agreementNumber(agreementNumber)
                    .isActive(true)
                    .build();
            group.setCreatedByUserId(currentUserId);
            group = groupRepository.save(group);

            Agreement agreement = Agreement.builder()
                    .agreementGroup(group)
                    .versionNumber(1)
                    .owner(owner)
                    .incomeType(incomeType)
                    .agreementType(agreementType)
                    .commercialStructure(commercials.commercialStructure())
                    .commercialValue(commercials.commercialValue())
                    .calculationFormula(commercials.calculationFormula())
                    .startDate(details.startDate())
                    .expiryDate(details.expiryDate())
                    .approvalStatus(ApprovalStatus.DRAFT)
                    .notes(details.notes())
                    .build();
            agreement.setCreatedByUserId(currentUserId);
            agreement = agreementRepository.save(agreement);

            saveVendors(agreement, request.vendorIds(), currentUserId);
            saveRulesAndComputeProducts(agreement, manufacturerIds, divisionRules, productRules, currentUserId);

            recordAudit(group.getId(), agreement.getId(), "AGREEMENT_CREATED", null, agreementNumber, currentUserId);
            agreement = applySubmitForApproval(agreement, currentUserId);

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

        newVersion = applySubmitForApproval(newVersion, currentUserId);
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
        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        IncomeType incomeType = incomeTypeRepository.findById(request.details().incomeTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("IncomeType", request.details().incomeTypeId()));
        AgreementType agreementType = agreementTypeRepository.findById(request.details().agreementTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("AgreementType", request.details().agreementTypeId()));

        if (request.details().expiryDate().isBefore(request.details().startDate())) {
            throw new BusinessException("Expiry date must be on or after start date");
        }

        Agreement newVersion = Agreement.builder()
                .agreementGroup(group)
                .versionNumber(maxVersion + 1)
                .owner(owner)
                .incomeType(incomeType)
                .agreementType(agreementType)
                .commercialStructure(request.commercials().commercialStructure())
                .commercialValue(request.commercials().commercialValue())
                .calculationFormula(request.commercials().calculationFormula())
                .startDate(request.details().startDate())
                .expiryDate(request.details().expiryDate())
                .approvalStatus(ApprovalStatus.DRAFT)
                .notes(request.details().notes())
                .build();
        newVersion.setCreatedByUserId(currentUserId);
        newVersion = agreementRepository.save(newVersion);

        List<Long> manufacturerIds = request.productRules().manufacturers() != null
                ? request.productRules().manufacturers() : List.of();
        saveVendors(newVersion, request.vendorIds(), currentUserId);
        saveRulesAndComputeProducts(newVersion, manufacturerIds,
                request.productRules().divisionRules(), request.productRules().productRules(), currentUserId);

        recordAudit(group.getId(), newVersion.getId(), "VERSIONED_EDIT_CREATED",
                String.valueOf(source.getVersionNumber()), String.valueOf(newVersion.getVersionNumber()), currentUserId);

        // group.currentVersionId intentionally NOT updated here — stays on approved version until this new version is approved
        newVersion = applySubmitForApproval(newVersion, currentUserId);
        return toAgreementResponse(newVersion);
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
    public AgreementResponse getAgreementById(Long agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));
        return toAgreementResponse(agreement);
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementGroupResponse getGroupById(Long groupId) {
        AgreementGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementGroup", groupId));
        return toGroupResponse(group);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgreementGroupResponse> getAllGroups(Pageable pageable, Long currentUserId, String scope,
                                                     boolean canViewAll, String agreementNumber,
                                                     String companyName, String status, String ownerName,
                                                     Long vendorId, Long incomeTypeId) {
        Pageable mappedPageable = mapGroupPageable(pageable);
        var scopeSpec = "ALL".equalsIgnoreCase(scope) && canViewAll
                ? AgreementGroupSpec.withFilters(agreementNumber, companyName, status, ownerName, vendorId, incomeTypeId)
                // TODO: Future Portfolio Scoping — replace ownedBy() with company-assignment filter:
                //   AgreementGroupSpec.assignedToCompanies(companyAssignmentRepository.findCompanyIdsByUserId(currentUserId))
                //   This joins user_company_assignments where user_id = currentUserId.
                : AgreementGroupSpec.withFilters(agreementNumber, companyName, status, ownerName, vendorId, incomeTypeId)
                        .and(AgreementGroupSpec.ownedBy(currentUserId));

        Page<AgreementGroup> groupPage = groupRepository.findAll(scopeSpec, mappedPageable);

        List<Long> groupIds = groupPage.getContent().stream().map(AgreementGroup::getId).toList();
        if (groupIds.isEmpty()) {
            return groupPage.map(this::toGroupResponseEmpty);
        }

        // Single batch query for latest agreements — eliminates N+1 per-group lookup.
        Map<Long, Agreement> latestByGroupId = agreementRepository.findLatestVersionsForGroupIds(groupIds)
                .stream()
                .collect(Collectors.toMap(a -> a.getAgreementGroup().getId(), a -> a, (a, b) -> a));

        // Single batch query for all vendors in this page — eliminates N+1 vendor lookup.
        List<Long> agreementIds = latestByGroupId.values().stream().map(Agreement::getId).toList();
        Map<Long, List<AgreementVendor>> vendorsByAgreementId = agreementIds.isEmpty()
                ? Map.of()
                : vendorRepository.findByAgreementIdIn(agreementIds)
                        .stream()
                        .collect(Collectors.groupingBy(v -> v.getAgreement().getId()));

        return groupPage.map(g -> {
            Agreement latest = latestByGroupId.get(g.getId());
            List<AgreementVendor> vendors = latest != null
                    ? vendorsByAgreementId.getOrDefault(latest.getId(), List.of())
                    : List.of();
            return toGroupResponseFromBatch(g, latest, vendors);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgreementResponse> getVersionsByGroup(Long groupId) {
        return agreementRepository.findByAgreementGroupId(groupId)
                .stream()
                .sorted((a, b) -> Integer.compare(a.getVersionNumber(), b.getVersionNumber()))
                .map(this::toAgreementResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgreementResponse> getVersionsByAgreementId(Long agreementId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));
        return getVersionsByGroup(agreement.getAgreementGroup().getId());
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
    private AgreementGroupResponse toGroupResponse(AgreementGroup g) {
        Agreement displayVersion = null;
        if (g.getCurrentVersionId() != null) {
            displayVersion = agreementRepository.findById(g.getCurrentVersionId()).orElse(null);
        }
        if (displayVersion == null) {
            List<Long> singleId = List.of(g.getId());
            List<Agreement> batch = agreementRepository.findLatestVersionsForGroupIds(singleId);
            displayVersion = batch.isEmpty() ? null : batch.get(0);
        }
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
