package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.request.CreateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.TerminateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.response.AgreementGroupResponse;
import com.medplus.agreement_tracker_backend.dto.response.AgreementResponse;
import com.medplus.agreement_tracker_backend.dto.response.ApprovalTimelineResponse;
import com.medplus.agreement_tracker_backend.entity.*;
import com.medplus.agreement_tracker_backend.enums.ApprovalAction;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.exception.UnauthorizedException;
import com.medplus.agreement_tracker_backend.repository.*;
import com.medplus.agreement_tracker_backend.service.AgreementService;
import com.medplus.agreement_tracker_backend.util.AgreementNumberGenerator;
import com.medplus.agreement_tracker_backend.util.AgreementStatusResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgreementServiceImpl implements AgreementService {

    private final AgreementGroupRepository groupRepository;
    private final AgreementRepository agreementRepository;
    private final AgreementVendorRepository vendorRepository;
    private final AgreementProductRepository productRepository;
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
    public AgreementResponse createDraft(CreateAgreementRequest request, Long currentUserId) {
        if (request.expiryDate().isBefore(request.startDate())) {
            throw new BusinessException("Expiry date must be on or after start date");
        }

        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));
        CompanyMaster company = companyRepository.findById(request.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Company", request.companyId()));
        IncomeType incomeType = incomeTypeRepository.findById(request.incomeTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("IncomeType", request.incomeTypeId()));
        AgreementType agreementType = agreementTypeRepository.findById(request.agreementTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("AgreementType", request.agreementTypeId()));

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
                .commercialStructure(request.commercialStructure())
                .commercialValue(request.commercialValue())
                .calculationFormula(request.calculationFormula())
                .startDate(request.startDate())
                .expiryDate(request.expiryDate())
                .approvalStatus(ApprovalStatus.DRAFT)
                .notes(request.notes())
                .build();
        agreement.setCreatedByUserId(currentUserId);
        agreement = agreementRepository.save(agreement);

        saveVendors(agreement, request.vendorIds(), currentUserId);
        saveProducts(agreement, request.productIds(), currentUserId);

        recordAudit(group.getId(), agreement.getId(), "AGREEMENT_CREATED", null, agreementNumber, currentUserId);

        return toAgreementResponse(agreement);
    }

    @Override
    @Transactional
    public AgreementResponse createNewVersion(Long agreementGroupId, Long currentUserId) {
        AgreementGroup group = groupRepository.findById(agreementGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementGroup", agreementGroupId));

        if (group.getCurrentVersionId() == null) {
            throw new BusinessException("No active version exists to create a new version from");
        }

        Agreement current = agreementRepository.findById(group.getCurrentVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", group.getCurrentVersionId()));

        if (current.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException("Can only create new version from an APPROVED agreement");
        }

        Integer maxVersion = agreementRepository.findMaxVersionByGroupId(agreementGroupId);
        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        Agreement newVersion = Agreement.builder()
                .agreementGroup(group)
                .versionNumber(maxVersion + 1)
                .owner(owner)
                .incomeType(current.getIncomeType())
                .agreementType(current.getAgreementType())
                .commercialStructure(current.getCommercialStructure())
                .commercialValue(current.getCommercialValue())
                .calculationFormula(current.getCalculationFormula())
                .startDate(current.getStartDate())
                .expiryDate(current.getExpiryDate())
                .approvalStatus(ApprovalStatus.DRAFT)
                .notes(current.getNotes())
                .build();
        newVersion.setCreatedByUserId(currentUserId);
        newVersion = agreementRepository.save(newVersion);

        copyVendors(current.getId(), newVersion, currentUserId);
        copyProducts(current.getId(), newVersion, currentUserId);

        recordAudit(group.getId(), newVersion.getId(), "NEW_VERSION_CREATED",
                String.valueOf(current.getVersionNumber()), String.valueOf(newVersion.getVersionNumber()), currentUserId);

        return toAgreementResponse(newVersion);
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
    public Page<AgreementGroupResponse> getAllGroups(Pageable pageable, Long currentUserId, boolean isAdmin) {
        if (isAdmin) {
            return groupRepository.findAll(pageable).map(this::toGroupResponse);
        }
        List<Long> companyIds = companyAssignmentRepository.findCompanyIdsByUserId(currentUserId);
        if (companyIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return groupRepository.findByCompanyIds(companyIds, pageable).map(this::toGroupResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgreementResponse> getVersionsByGroup(Long groupId) {
        return agreementRepository.findByAgreementGroupId(groupId)
                .stream()
                .map(this::toAgreementResponse)
                .toList();
    }

    @Override
    @Transactional
    public AgreementResponse submitForApproval(Long agreementId, Long currentUserId) {
        Agreement agreement = loadAndValidateOwnership(agreementId, currentUserId);

        if (agreement.getApprovalStatus() != ApprovalStatus.DRAFT) {
            throw new BusinessException("Only DRAFT agreements can be submitted for approval");
        }

        ApprovalStatus before = agreement.getApprovalStatus();
        agreement.setApprovalStatus(ApprovalStatus.PENDING_APPROVAL);
        agreement.setUpdatedByUserId(currentUserId);
        agreement = agreementRepository.save(agreement);

        recordApproval(agreement, ApprovalAction.SUBMITTED, null, before, ApprovalStatus.PENDING_APPROVAL, currentUserId);
        recordAudit(agreement.getAgreementGroup().getId(), agreementId, "SUBMITTED_FOR_APPROVAL", before.name(), ApprovalStatus.PENDING_APPROVAL.name(), currentUserId);

        return toAgreementResponse(agreement);
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
            throw new UnauthorizedException("Agreement owner cannot approve their own agreement");
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
            throw new UnauthorizedException("Agreement owner cannot reject their own agreement");
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
    public Page<AgreementResponse> getPendingApprovals(Pageable pageable) {
        return agreementRepository.findAllPendingApproval(pageable)
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

        List<Long> activeVersionIds;
        if (groupIds != null && !groupIds.isEmpty()) {
            activeVersionIds = groupRepository.findAllById(groupIds)
                    .stream()
                    .filter(g -> g.getCurrentVersionId() != null)
                    .map(AgreementGroup::getCurrentVersionId)
                    .toList();
        } else {
            activeVersionIds = agreementRepository.findByOwnerId(fromUserId, Pageable.unpaged())
                    .stream()
                    .filter(a -> a.getApprovalStatus() == ApprovalStatus.APPROVED)
                    .map(Agreement::getId)
                    .toList();
        }

        for (Long versionId : activeVersionIds) {
            agreementRepository.findById(versionId).ifPresent(a -> {
                if (a.getOwner().getId().equals(fromUserId)) {
                    a.setOwner(toUser);
                    a.setUpdatedByUserId(performedByUserId);
                    agreementRepository.save(a);
                    recordAudit(a.getAgreementGroup().getId(), a.getId(), "OWNERSHIP_TRANSFERRED",
                            String.valueOf(fromUserId), String.valueOf(toUserId), performedByUserId);
                }
            });
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

    private void saveProducts(Agreement agreement, List<Long> productIds, Long userId) {
        for (Long productId : productIds) {
            productMasterRepository.findById(productId).ifPresent(product -> {
                AgreementProduct ap = AgreementProduct.builder()
                        .agreement(agreement)
                        .productId(product.getId())
                        .manufacturerId(product.getManufacturer().getId())
                        .divisionId(product.getDivision().getId())
                        .productNameSnapshot(product.getProductName())
                        .manufacturerNameSnapshot(product.getManufacturer().getManufacturerName())
                        .divisionNameSnapshot(product.getDivision().getDivisionName())
                        .build();
                ap.setCreatedByUserId(userId);
                productRepository.save(ap);
            });
        }
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

    private void copyProducts(Long sourceAgreementId, Agreement target, Long userId) {
        productRepository.findByAgreementId(sourceAgreementId).forEach(p -> {
            AgreementProduct copy = AgreementProduct.builder()
                    .agreement(target)
                    .productId(p.getProductId())
                    .manufacturerId(p.getManufacturerId())
                    .divisionId(p.getDivisionId())
                    .productNameSnapshot(p.getProductNameSnapshot())
                    .manufacturerNameSnapshot(p.getManufacturerNameSnapshot())
                    .divisionNameSnapshot(p.getDivisionNameSnapshot())
                    .build();
            copy.setCreatedByUserId(userId);
            productRepository.save(copy);
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

        List<AgreementResponse.ProductSummary> products = productRepository.findByAgreementId(a.getId())
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
                products,
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    private AgreementGroupResponse toGroupResponse(AgreementGroup g) {
        AgreementResponse currentVersionResponse = null;
        if (g.getCurrentVersionId() != null) {
            currentVersionResponse = agreementRepository.findById(g.getCurrentVersionId())
                    .map(this::toAgreementResponse)
                    .orElse(null);
        }

        return new AgreementGroupResponse(
                g.getId(),
                g.getAgreementNumber(),
                g.getCompany().getId(),
                g.getCompany().getCompanyName(),
                g.getCurrentVersionId(),
                currentVersionResponse != null ? currentVersionResponse.versionNumber() : null,
                currentVersionResponse != null ? currentVersionResponse.derivedStatus() : null,
                g.isActive(),
                g.getCreatedAt()
        );
    }
}
