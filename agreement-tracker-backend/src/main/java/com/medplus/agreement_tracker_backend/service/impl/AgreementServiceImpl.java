package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.constants.IncomeTypeNames;
import com.medplus.agreement_tracker_backend.dto.request.CreateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.DraftAgreementItemRequest;
import com.medplus.agreement_tracker_backend.dto.request.AssetPayoutPeriodDto;
import com.medplus.agreement_tracker_backend.dto.request.DraftAssetPayload;
import com.medplus.agreement_tracker_backend.dto.request.DraftCommercialsPayload;
import com.medplus.agreement_tracker_backend.dto.request.DraftDetailsPayload;
import com.medplus.agreement_tracker_backend.dto.request.EditAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.ProductRulesPayload;
import com.medplus.agreement_tracker_backend.dto.request.RuleDTO;
import com.medplus.agreement_tracker_backend.dto.request.TerminateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.UpdateDraftRequest;
import com.medplus.agreement_tracker_backend.dto.response.AgreementResponse;
import com.medplus.agreement_tracker_backend.dto.response.AgreementVersionResponse;
import com.medplus.agreement_tracker_backend.dto.response.ApprovalTimelineResponse;
import com.medplus.agreement_tracker_backend.dto.response.BulkAgreementCreateResponse;
import com.medplus.agreement_tracker_backend.dto.response.BulkGroupSubmitResponse;
import com.medplus.agreement_tracker_backend.dto.response.RenewAgreementResponse;
import com.medplus.agreement_tracker_backend.dto.response.CompanyAgreementGroupResponse;
import com.medplus.agreement_tracker_backend.dto.response.PendingActionRequestInfo;
import com.medplus.agreement_tracker_backend.entity.Agreement;
import com.medplus.agreement_tracker_backend.entity.AgreementActionRequest;
import com.medplus.agreement_tracker_backend.entity.AgreementApproval;
import com.medplus.agreement_tracker_backend.entity.AgreementAsset;
import com.medplus.agreement_tracker_backend.entity.AgreementAssetPayoutPeriod;
import com.medplus.agreement_tracker_backend.entity.AgreementAudit;
import com.medplus.agreement_tracker_backend.entity.AgreementComputedProduct;
import com.medplus.agreement_tracker_backend.entity.AgreementDivisionRule;
import com.medplus.agreement_tracker_backend.entity.AgreementJbpCommercialPeriod;
import com.medplus.agreement_tracker_backend.entity.AgreementJbpConfiguration;
import com.medplus.agreement_tracker_backend.entity.AgreementJbpVersionFrequency;
import com.medplus.agreement_tracker_backend.entity.AgreementManufacturer;
import com.medplus.agreement_tracker_backend.entity.AgreementProductRule;
import com.medplus.agreement_tracker_backend.entity.AgreementSlab;
import com.medplus.agreement_tracker_backend.entity.AgreementType;
import com.medplus.agreement_tracker_backend.entity.AgreementVendor;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.entity.CompanyAgreementGroup;
import com.medplus.agreement_tracker_backend.entity.CompanyMaster;
import com.medplus.agreement_tracker_backend.entity.IncomeType;
import com.medplus.agreement_tracker_backend.entity.ProductMaster;
import com.medplus.agreement_tracker_backend.entity.StateMaster;
import com.medplus.agreement_tracker_backend.entity.User;
import com.medplus.agreement_tracker_backend.entity.VendorMaster;
import com.medplus.agreement_tracker_backend.enums.ActionRequestStatus;
import com.medplus.agreement_tracker_backend.enums.AdHocSubType;
import com.medplus.agreement_tracker_backend.enums.ApprovalAction;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.enums.AssetCategory;
import com.medplus.agreement_tracker_backend.enums.CalculationBasis;
import com.medplus.agreement_tracker_backend.enums.CommercialStructure;
import com.medplus.agreement_tracker_backend.enums.LeadTimeBasis;
import com.medplus.agreement_tracker_backend.enums.PaymentRealizationType;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import com.medplus.agreement_tracker_backend.enums.RuleType;
import com.medplus.agreement_tracker_backend.exception.BusinessException;
import com.medplus.agreement_tracker_backend.exception.IncompleteAgreementException;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.exception.UnauthorizedException;
import com.medplus.agreement_tracker_backend.repository.AgreementActionRequestRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementApprovalRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementAssetRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementAssetPayoutPeriodRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementAuditRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementComputedProductRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementDivisionRuleRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementJbpCommercialPeriodRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementJbpConfigurationRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementJbpVersionFrequencyRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementManufacturerRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementProductRuleRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementDocumentRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementReminderRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementSlabRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementStoreMappingRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementSpec;
import com.medplus.agreement_tracker_backend.repository.AgreementTypeRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementVendorRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementVersionRepository;
import com.medplus.agreement_tracker_backend.repository.CompanyAgreementGroupRepository;
import com.medplus.agreement_tracker_backend.repository.IncomeTypeRepository;
import com.medplus.agreement_tracker_backend.repository.ProductMasterRepository;
import com.medplus.agreement_tracker_backend.repository.StateMasterRepository;
import com.medplus.agreement_tracker_backend.repository.UserRepository;
import com.medplus.agreement_tracker_backend.repository.VendorMasterRepository;
import com.medplus.agreement_tracker_backend.service.AgreementService;
import com.medplus.agreement_tracker_backend.service.CompanyAgreementGroupService;
import com.medplus.agreement_tracker_backend.service.StoreMappingService;
import com.medplus.agreement_tracker_backend.util.AgreementStatusResolver;
import com.medplus.agreement_tracker_backend.validation.Step1Validation;
import com.medplus.agreement_tracker_backend.validation.Step2Validation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgreementServiceImpl implements AgreementService {

    private final AgreementRepository agreementRepository;
    private final AgreementVersionRepository agreementVersionRepository;
    private final CompanyAgreementGroupRepository companyAgreementGroupRepository;
    private final CompanyAgreementGroupService companyAgreementGroupService;
    private final AgreementVendorRepository vendorRepository;
    private final AgreementManufacturerRepository manufacturerRuleRepository;
    private final AgreementDivisionRuleRepository divisionRuleRepository;
    private final AgreementProductRuleRepository productRuleRepository;
    private final AgreementSlabRepository slabRepository;
    private final AgreementJbpCommercialPeriodRepository jbpCommercialPeriodRepository;
    private final AgreementJbpConfigurationRepository jbpConfigurationRepository;
    private final AgreementJbpVersionFrequencyRepository jbpVersionFrequencyRepository;
    private final AgreementComputedProductRepository computedProductRepository;
    private final AgreementApprovalRepository approvalRepository;
    private final AgreementAuditRepository auditRepository;
    private final AgreementActionRequestRepository actionRequestRepository;
    private final AgreementReminderRepository reminderRepository;
    private final AgreementDocumentRepository documentRepository;
    private final AgreementAssetRepository assetRepository;
    private final AgreementAssetPayoutPeriodRepository assetPayoutPeriodRepository;
    private final AgreementStoreMappingRepository storeMappingRepository;
    private final StoreMappingService storeMappingService;
    private final UserRepository userRepository;
    private final IncomeTypeRepository incomeTypeRepository;
    private final AgreementTypeRepository agreementTypeRepository;
    private final VendorMasterRepository vendorMasterRepository;
    private final StateMasterRepository stateMasterRepository;
    private final ProductMasterRepository productMasterRepository;
    private final AgreementStatusResolver statusResolver;
    private final TransactionTemplate groupSubmitTransactionTemplate;
    private final Validator validator;

    private static final DateTimeFormatter AGREEMENT_NAME_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String DRAFT_AGREEMENT_NAME_PLACEHOLDER = "Draft - Pending Details";
    private static final String TERMINATED_RENEW_MSG =
            "Terminated agreements cannot be renewed. Please create a new agreement instead.";
    private static final String TERMINATED_EDIT_MSG =
            "Terminated agreements cannot be edited. Please create a new agreement instead.";

    public AgreementServiceImpl(
            AgreementRepository agreementRepository,
            AgreementVersionRepository agreementVersionRepository,
            CompanyAgreementGroupRepository companyAgreementGroupRepository,
            CompanyAgreementGroupService companyAgreementGroupService,
            AgreementVendorRepository vendorRepository,
            AgreementManufacturerRepository manufacturerRuleRepository,
            AgreementDivisionRuleRepository divisionRuleRepository,
            AgreementProductRuleRepository productRuleRepository,
            AgreementSlabRepository slabRepository,
            AgreementJbpCommercialPeriodRepository jbpCommercialPeriodRepository,
            AgreementJbpConfigurationRepository jbpConfigurationRepository,
            AgreementJbpVersionFrequencyRepository jbpVersionFrequencyRepository,
            AgreementComputedProductRepository computedProductRepository,
            AgreementApprovalRepository approvalRepository,
            AgreementAuditRepository auditRepository,
            AgreementActionRequestRepository actionRequestRepository,
            AgreementReminderRepository reminderRepository,
            AgreementDocumentRepository documentRepository,
            AgreementAssetRepository assetRepository,
            AgreementAssetPayoutPeriodRepository assetPayoutPeriodRepository,
            AgreementStoreMappingRepository storeMappingRepository,
            StoreMappingService storeMappingService,
            UserRepository userRepository,
            IncomeTypeRepository incomeTypeRepository,
            AgreementTypeRepository agreementTypeRepository,
            VendorMasterRepository vendorMasterRepository,
            StateMasterRepository stateMasterRepository,
            ProductMasterRepository productMasterRepository,
            AgreementStatusResolver statusResolver,
            PlatformTransactionManager transactionManager,
            Validator validator) {
        this.agreementRepository = agreementRepository;
        this.agreementVersionRepository = agreementVersionRepository;
        this.companyAgreementGroupRepository = companyAgreementGroupRepository;
        this.companyAgreementGroupService = companyAgreementGroupService;
        this.vendorRepository = vendorRepository;
        this.manufacturerRuleRepository = manufacturerRuleRepository;
        this.divisionRuleRepository = divisionRuleRepository;
        this.productRuleRepository = productRuleRepository;
        this.slabRepository = slabRepository;
        this.jbpCommercialPeriodRepository = jbpCommercialPeriodRepository;
        this.jbpConfigurationRepository = jbpConfigurationRepository;
        this.jbpVersionFrequencyRepository = jbpVersionFrequencyRepository;
        this.computedProductRepository = computedProductRepository;
        this.approvalRepository = approvalRepository;
        this.auditRepository = auditRepository;
        this.actionRequestRepository = actionRequestRepository;
        this.reminderRepository = reminderRepository;
        this.documentRepository = documentRepository;
        this.assetRepository = assetRepository;
        this.assetPayoutPeriodRepository = assetPayoutPeriodRepository;
        this.storeMappingRepository = storeMappingRepository;
        this.storeMappingService = storeMappingService;
        this.userRepository = userRepository;
        this.incomeTypeRepository = incomeTypeRepository;
        this.agreementTypeRepository = agreementTypeRepository;
        this.vendorMasterRepository = vendorMasterRepository;
        this.stateMasterRepository = stateMasterRepository;
        this.productMasterRepository = productMasterRepository;
        this.statusResolver = statusResolver;
        this.groupSubmitTransactionTemplate = new TransactionTemplate(transactionManager);
        this.validator = validator;
    }

    @Override
    @Transactional
    public BulkAgreementCreateResponse createDraft(CreateAgreementRequest request, Long currentUserId) {
        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        CompanyAgreementGroup cag = resolveCompanyAgreementGroup(
                request.companyId(),
                request.companyAgreementGroupId(),
                request.newCompanyAgreementGroupName(),
                currentUserId);

        if (!cag.isActive()) {
            throw new BusinessException("Cannot add agreements to an inactive group.");
        }

        List<Long> vendorIds = request.vendorIds() != null ? request.vendorIds() : List.of();
        ProductRulesPayload rulesPayload = request.productRules() != null
                ? request.productRules() : new ProductRulesPayload(null, null, null);
        List<Long> manufacturerIds = rulesPayload.manufacturers() != null ? rulesPayload.manufacturers() : List.of();
        List<RuleDTO> divisionRules = rulesPayload.divisionRules() != null ? rulesPayload.divisionRules() : List.of();
        List<RuleDTO> productRules = rulesPayload.productRules() != null ? rulesPayload.productRules() : List.of();

        List<DraftAgreementItemRequest> items = request.agreements() != null && !request.agreements().isEmpty()
                ? request.agreements()
                : List.of(new DraftAgreementItemRequest(null, null));

        List<AgreementVersionResponse> created = new ArrayList<>();
        Long primaryAgreementId = null;

        for (DraftAgreementItemRequest item : items) {
            Agreement parent = Agreement.builder()
                    .companyAgreementGroup(cag)
                    .agreementName(DRAFT_AGREEMENT_NAME_PLACEHOLDER)
                    .owner(owner)
                    .isActive(true)
                    .build();
            parent.setCreatedByUserId(currentUserId);
            parent = agreementRepository.save(parent);

            AgreementVersion version = buildDraftVersion(item, owner, parent, 1, currentUserId);
            version = agreementVersionRepository.save(version);
            DraftDetailsPayload itemDetails = item != null ? item.details() : null;
            if (itemDetails != null && itemDetails.stateIds() != null) {
                replaceAgreementStates(parent, itemDetails.stateIds(), currentUserId);
            }
            syncAgreementName(parent, version, currentUserId);

            replaceVendors(version, vendorIds, currentUserId);
            replaceRulesAndComputeProducts(version, manufacturerIds, divisionRules, productRules, currentUserId);

            recordAudit(parent.getId(), version.getId(), "AGREEMENT_CREATED", null, null, currentUserId);

            if (primaryAgreementId == null) {
                primaryAgreementId = parent.getId();
            }
            created.add(toVersionResponse(version));
        }

        return new BulkAgreementCreateResponse(created, primaryAgreementId);
    }

    @Override
    @Transactional
    public AgreementVersionResponse createNewVersion(Long agreementId, Long currentUserId) {
        Agreement parent = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));

        AgreementVersion source = resolveNewVersionSource(parent);
        assertNotTerminated(source, TERMINATED_EDIT_MSG);
        loadAndValidateOwnership(source.getId(), currentUserId);

        Integer maxVersion = agreementVersionRepository.findMaxVersionByAgreementId(agreementId);
        User owner = parent.getOwner();

        AgreementVersion newVersion = AgreementVersion.builder()
                .agreement(parent)
                .versionNumber(maxVersion + 1)
                .owner(owner)
                .incomeType(source.getIncomeType())
                .agreementType(source.getAgreementType())
                .commercialStructure(source.getCommercialStructure())
                .commercialValue(source.getCommercialValue())
                .calculationFormula(source.getCalculationFormula())
                .startDate(source.getStartDate())
                .expiryDate(source.getExpiryDate())
                .financialYearStartMonth(source.getFinancialYearStartMonth())
                .approvalStatus(ApprovalStatus.DRAFT)
                .notes(source.getNotes())
                .build();
        newVersion.setCreatedByUserId(currentUserId);
        newVersion = agreementVersionRepository.save(newVersion);

        copyVendors(source.getId(), newVersion, currentUserId);
        copyRulesAndComputed(source.getId(), newVersion, currentUserId);

        recordAudit(parent.getId(), newVersion.getId(), "NEW_VERSION_CREATED",
                String.valueOf(source.getVersionNumber()), String.valueOf(newVersion.getVersionNumber()), currentUserId);

        return toVersionResponse(newVersion);
    }

    @Override
    @Transactional
    public AgreementVersionResponse createVersionedEdit(Long sourceAgreementVersionId, EditAgreementRequest request,
                                                        Long currentUserId) {
        AgreementVersion source = agreementVersionRepository.findById(sourceAgreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", sourceAgreementVersionId));

        assertNotTerminated(source, TERMINATED_EDIT_MSG);

        if (source.getApprovalStatus() != ApprovalStatus.APPROVED
                && source.getApprovalStatus() != ApprovalStatus.REJECTED) {
            throw new BusinessException("Can only create a versioned edit from APPROVED or REJECTED agreements");
        }

        loadAndValidateOwnership(sourceAgreementVersionId, currentUserId);

        Agreement parent = source.getAgreement();
        Integer maxVersion = agreementVersionRepository.findMaxVersionByAgreementId(parent.getId());
        AgreementVersion latest = agreementVersionRepository
                .findByAgreementIdAndVersionNumber(parent.getId(), maxVersion)
                .orElseThrow(() -> new BusinessException("No agreement version exists for this agreement"));

        if (latest.getApprovalStatus() == ApprovalStatus.DRAFT
                && parent.getOwner().getId().equals(currentUserId)) {
            throw new BusinessException("A draft version already exists — update it in place");
        }

        User owner = parent.getOwner();

        DraftAgreementItemRequest item = new DraftAgreementItemRequest(request.details(), request.commercials());
        AgreementVersion newVersion = buildDraftVersion(item, owner, parent, maxVersion + 1, currentUserId);
        newVersion = agreementVersionRepository.save(newVersion);
        applyDraftFields(newVersion, request.details(), request.commercials());

        List<Long> vendorIds = request.vendorIds() != null ? request.vendorIds() : List.of();
        replaceVendors(newVersion, vendorIds, currentUserId);

        Long incomeTypeId = resolveIncomeTypeId(newVersion, request.details());
        syncIncomeTypeSpecificData(
                newVersion,
                incomeTypeId,
                currentUserId,
                request.productRules(),
                request.asset(),
                request.details(),
                true);

        if (request.details() != null && request.details().stateIds() != null) {
            replaceAgreementStates(parent, request.details().stateIds(), currentUserId);
        }
        syncAgreementName(parent, newVersion, currentUserId);

        newVersion.setApprovedBy(null);
        newVersion.setApprovalDate(null);
        newVersion.setUpdatedByUserId(currentUserId);
        newVersion = agreementVersionRepository.save(newVersion);

        storeMappingService.copyMappings(source.getId(), newVersion.getId());
        copyAssetPayoutPeriods(source.getId(), newVersion.getId());
        copyJbpVersionFrequencies(source.getId(), newVersion);
        copySlabs(source.getId(), newVersion, currentUserId);
        Map<Long, Long> jbpConfigurationIdMap = copyJbpConfigurations(source.getId(), newVersion);
        copyJbpCommercialPeriods(source.getId(), newVersion, jbpConfigurationIdMap);

        if (Boolean.TRUE.equals(request.requiresReapproval())
                && source.getApprovalStatus() == ApprovalStatus.APPROVED) {
            parent.setCurrentVersionId(newVersion.getId());
            parent.setUpdatedByUserId(currentUserId);
            agreementRepository.save(parent);
        }

        recordAudit(parent.getId(), newVersion.getId(), "VERSIONED_EDIT_CREATED",
                String.valueOf(source.getVersionNumber()), String.valueOf(newVersion.getVersionNumber()), currentUserId);

        return toVersionResponse(newVersion);
    }

    @Override
    @Transactional
    public AgreementVersionResponse updateDraft(Long agreementVersionId, UpdateDraftRequest request, Long currentUserId,
                                                boolean validateStep1, boolean validateStep2,
                                                boolean validateCommercialStructure) {
        if (validateStep1) {
            var violations = validator.validate(request, Step1Validation.class);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
            validateStep1Fields(request);
        }
        if (validateStep2) {
            var violations = validator.validate(request, Step2Validation.class);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
            validateStep2Fields(request);
        }
        if (validateCommercialStructure) {
            validateCommercialStructureFields(agreementVersionId, request);
        }

        AgreementVersion version = loadAndValidateOwnership(agreementVersionId, currentUserId);
        if (version.getApprovalStatus() == ApprovalStatus.APPROVED
                && Boolean.TRUE.equals(request.requiresReapproval())) {
            return createReapprovalDraft(version, request, currentUserId);
        }
        if (version.getApprovalStatus() != ApprovalStatus.DRAFT) {
            throw new BusinessException("Only DRAFT agreements can be updated via draft save");
        }

        Agreement parent = version.getAgreement();

        Long previousIncomeTypeId = version.getIncomeType() != null ? version.getIncomeType().getId() : null;

        if (request.companyId() != null) {
            Long currentCompanyId = parent.getCompanyAgreementGroup().getCompany().getId();
            if (!request.companyId().equals(currentCompanyId)) {
                throw new BusinessException("Company cannot be changed after the agreement draft is created");
            }
        }

        Long incomeTypeId = resolveIncomeTypeId(version, request.details());
        UpdateDraftRequest scrubbed = scrubRequestForIncomeType(request, incomeTypeId);

        applyDraftFields(version, scrubbed.details(), scrubbed.commercials());
        version.setUpdatedByUserId(currentUserId);
        version = agreementVersionRepository.save(version);

        Long newIncomeTypeId = resolveIncomeTypeId(version, scrubbed.details());
        if (previousIncomeTypeId != null && newIncomeTypeId != null
                && !Objects.equals(previousIncomeTypeId, newIncomeTypeId)) {
            storeMappingRepository.deleteByAgreementVersionId(version.getId());
            assetPayoutPeriodRepository.deleteByAgreementVersionId(version.getId());
        }

        if (scrubbed.details() != null && scrubbed.details().stateIds() != null) {
            replaceAgreementStates(parent, scrubbed.details().stateIds(), currentUserId);
        }
        syncAgreementName(parent, version, currentUserId);

        if (scrubbed.vendorIds() != null) {
            replaceVendors(version, scrubbed.vendorIds(), currentUserId);
        }

        syncIncomeTypeSpecificData(
                version,
                incomeTypeId,
                currentUserId,
                scrubbed.productRules(),
                scrubbed.asset(),
                scrubbed.details(),
                validateStep2);
        version = agreementVersionRepository.save(version);

        recordAudit(parent.getId(), version.getId(), "DRAFT_UPDATED", null, null, currentUserId);
        return toVersionResponse(version);
    }

    private AgreementVersionResponse createReapprovalDraft(AgreementVersion approvedVersion,
                                                            UpdateDraftRequest request,
                                                            Long currentUserId) {
        Agreement parent = approvedVersion.getAgreement();
        Integer maxVersion = agreementVersionRepository.findMaxVersionByAgreementId(parent.getId());
        AgreementVersion latest = agreementVersionRepository
                .findByAgreementIdAndVersionNumber(parent.getId(), maxVersion)
                .orElseThrow(() -> new BusinessException("No agreement version exists for this agreement"));

        if (latest.getApprovalStatus() == ApprovalStatus.DRAFT
                && parent.getOwner().getId().equals(currentUserId)) {
            throw new BusinessException("A draft version already exists — update it in place");
        }

        AgreementVersion newVersion = AgreementVersion.builder()
                .agreement(parent)
                .versionNumber(maxVersion + 1)
                .owner(parent.getOwner())
                .approvalStatus(ApprovalStatus.DRAFT)
                .build();
        newVersion.setCreatedByUserId(currentUserId);
        newVersion = agreementVersionRepository.save(newVersion);

        applyDraftFields(newVersion, request.details(), request.commercials());
        if (request.details() != null && request.details().stateIds() != null) {
            replaceAgreementStates(parent, request.details().stateIds(), currentUserId);
        }
        syncAgreementName(parent, newVersion, currentUserId);

        if (request.vendorIds() != null) {
            replaceVendors(newVersion, request.vendorIds(), currentUserId);
        }

        Long incomeTypeId = resolveIncomeTypeId(newVersion, request.details());
        syncIncomeTypeSpecificData(
                newVersion,
                incomeTypeId,
                currentUserId,
                request.productRules(),
                request.asset(),
                request.details(),
                true);
        newVersion.setApprovedBy(null);
        newVersion.setApprovalDate(null);
        newVersion.setUpdatedByUserId(currentUserId);
        newVersion = agreementVersionRepository.save(newVersion);

        storeMappingService.copyMappings(approvedVersion.getId(), newVersion.getId());
        copyAssetPayoutPeriods(approvedVersion.getId(), newVersion.getId());

        parent.setCurrentVersionId(newVersion.getId());
        parent.setUpdatedByUserId(currentUserId);
        agreementRepository.save(parent);

        recordAudit(parent.getId(), newVersion.getId(), "REAPPROVAL_DRAFT_CREATED",
                String.valueOf(approvedVersion.getVersionNumber()),
                String.valueOf(newVersion.getVersionNumber()), currentUserId);

        return toVersionResponse(newVersion);
    }

    private Long resolveIncomeTypeId(AgreementVersion version, DraftDetailsPayload details) {
        if (version.getIncomeType() != null) {
            return version.getIncomeType().getId();
        }
        return details != null ? details.incomeTypeId() : null;
    }

    private void syncIncomeTypeSpecificData(AgreementVersion version,
                                            Long incomeTypeId,
                                            Long currentUserId,
                                            ProductRulesPayload productRulesPayload,
                                            DraftAssetPayload assetPayload,
                                            DraftDetailsPayload detailsPayload,
                                            boolean validateStep2) {
        if (isAssetRentalIncomeType(incomeTypeId)) {
            clearProductRulesForVersion(version);
            version.setAdhocSubType(null);
            version.setQuantityCap(null);
            if (shouldPersistAsset(assetPayload, validateStep2)) {
                replaceAsset(version, assetPayload, currentUserId);
            }
            return;
        }

        clearAssetForVersion(version);
        if (!isAdHocIncomeType(incomeTypeId)) {
            version.setAdhocSubType(null);
            version.setQuantityCap(null);
        } else if (detailsPayload != null && "QPS".equals(detailsPayload.adhocSubType())) {
            version.setQuantityCap(null);
        }

        if (productRulesPayload != null) {
            List<Long> manufacturerIds = productRulesPayload.manufacturers() != null
                    ? productRulesPayload.manufacturers() : List.of();
            List<RuleDTO> divisionRules = productRulesPayload.divisionRules() != null
                    ? productRulesPayload.divisionRules() : List.of();
            List<RuleDTO> productRules = productRulesPayload.productRules() != null
                    ? productRulesPayload.productRules() : List.of();
            replaceRulesAndComputeProducts(version, manufacturerIds, divisionRules, productRules, currentUserId);
        }
    }

    private void clearAssetForVersion(AgreementVersion version) {
        storeMappingRepository.deleteByAgreementVersionId(version.getId());
        assetPayoutPeriodRepository.deleteByAgreementVersionId(version.getId());
        assetRepository.deleteByAgreementVersionId(version.getId());
        version.setAsset(null);
    }

    private void clearProductRulesForVersion(AgreementVersion version) {
        manufacturerRuleRepository.deleteByAgreementVersionId(version.getId());
        divisionRuleRepository.deleteByAgreementVersionId(version.getId());
        productRuleRepository.deleteByAgreementVersionId(version.getId());
        computedProductRepository.deleteByAgreementVersionId(version.getId());
    }

    @Override
    @Transactional
    public AgreementVersionResponse cloneAgreement(Long sourceAgreementVersionId, Long currentUserId) {
        AgreementVersion source = loadAndValidateOwnership(sourceAgreementVersionId, currentUserId);
        Agreement sourceParent = source.getAgreement();
        CompanyAgreementGroup cag = sourceParent.getCompanyAgreementGroup();

        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        Agreement parent = Agreement.builder()
                .companyAgreementGroup(cag)
                .agreementName(DRAFT_AGREEMENT_NAME_PLACEHOLDER)
                .owner(owner)
                .isActive(true)
                .build();
        parent.setCreatedByUserId(currentUserId);
        parent = agreementRepository.save(parent);
        copyAgreementStates(sourceParent, parent, currentUserId);

        AgreementVersion clone = AgreementVersion.builder()
                .agreement(parent)
                .versionNumber(1)
                .owner(owner)
                .approvalStatus(ApprovalStatus.DRAFT)
                .build();
        clone.setCreatedByUserId(currentUserId);
        clone = agreementVersionRepository.save(clone);

        copyVendors(source.getId(), clone, currentUserId);
        copyRulesAndComputed(source.getId(), clone, currentUserId);
        storeMappingService.copyMappings(source.getId(), clone.getId());
        copyAssetPayoutPeriods(source.getId(), clone.getId());

        recordAudit(parent.getId(), clone.getId(), "AGREEMENT_CLONED",
                String.valueOf(sourceAgreementVersionId), null, currentUserId);

        return toVersionResponse(clone);
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementVersionResponse getAgreementVersionById(Long agreementVersionId, Long currentUserId) {
        AgreementVersion version = agreementVersionRepository.findById(agreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreementVersionId));
        enforceDraftVisibility(version, currentUserId);
        return toVersionResponse(version);
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementResponse getAgreementById(Long agreementId, Long currentUserId) {
        Agreement parent = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));
        enforceAgreementDraftVisibility(parent, currentUserId);

        List<AgreementVersion> latestBatch =
                agreementVersionRepository.findLatestVersionsForAgreementIds(List.of(agreementId));
        AgreementVersion latest = latestBatch.isEmpty() ? null : latestBatch.get(0);
        AgreementVersion displayVersion = resolveVisibleLatest(parent, latest, currentUserId);

        if (displayVersion == null) {
            return toParentResponseEmpty(parent);
        }

        List<AgreementVendor> vendors = vendorRepository.findByAgreementVersionId(displayVersion.getId());
        return toParentResponse(parent, displayVersion, vendors);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgreementResponse> getAllAgreements(Pageable pageable, Long currentUserId, String scope,
                                                    boolean canViewAll, Long companyId, Long companyAgreementGroupId,
                                                    String companyAgreementGroupName, String agreementName,
                                                    String status, String ownerName, Long vendorId, Long incomeTypeId,
                                                    LocalDate startDateFrom, LocalDate startDateTo,
                                                    LocalDate endDateFrom, LocalDate endDateTo) {
        Pageable mappedPageable = mapAgreementPageable(pageable);
        var filterSpec = AgreementSpec.withFilters(
                        companyId, companyAgreementGroupId, companyAgreementGroupName,
                        agreementName, status, ownerName, vendorId, incomeTypeId,
                        startDateFrom, startDateTo, endDateFrom, endDateTo)
                .and(AgreementSpec.draftVisibleTo(currentUserId));

        var scopeSpec = "ALL".equalsIgnoreCase(scope) && canViewAll
                ? filterSpec
                : filterSpec.and(AgreementSpec.ownedBy(currentUserId));

        Page<Agreement> parentPage = agreementRepository.findAll(scopeSpec, mappedPageable);

        List<Long> agreementIds = parentPage.getContent().stream().map(Agreement::getId).toList();
        if (agreementIds.isEmpty()) {
            return parentPage.map(this::toParentResponseEmpty);
        }

        Map<Long, AgreementVersion> latestByAgreementId = agreementVersionRepository
                .findLatestVersionsForAgreementIds(agreementIds)
                .stream()
                .collect(Collectors.toMap(v -> v.getAgreement().getId(), v -> v, (a, b) -> a));

        List<Long> currentVersionIds = parentPage.getContent().stream()
                .map(Agreement::getCurrentVersionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, AgreementVersion> currentVersionById = currentVersionIds.isEmpty()
                ? Map.of()
                : agreementVersionRepository.findByIdInWithDetails(currentVersionIds).stream()
                        .collect(Collectors.toMap(AgreementVersion::getId, v -> v, (a, b) -> a));

        Map<Long, AgreementVersion> visibleByAgreementId = parentPage.getContent().stream()
                .collect(Collectors.toMap(
                        Agreement::getId,
                        p -> resolveListDisplayVersion(
                                p, latestByAgreementId.get(p.getId()), currentVersionById, currentUserId),
                        (a, b) -> a
                ));

        List<Long> visibleVersionIds = visibleByAgreementId.values().stream()
                .filter(Objects::nonNull)
                .map(AgreementVersion::getId)
                .toList();
        Map<Long, List<AgreementVendor>> vendorsByVersionId = visibleVersionIds.isEmpty()
                ? Map.of()
                : vendorRepository.findByAgreementVersionIdIn(visibleVersionIds)
                        .stream()
                        .collect(Collectors.groupingBy(v -> v.getAgreementVersion().getId()));

        return parentPage.map(p -> {
            AgreementVersion visible = visibleByAgreementId.get(p.getId());
            List<AgreementVendor> vendors = visible != null
                    ? vendorsByVersionId.getOrDefault(visible.getId(), List.of())
                    : List.of();
            return visible != null ? toParentResponse(p, visible, vendors) : toParentResponseEmpty(p);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgreementVersionResponse> getVersionsByAgreementId(Long agreementId, Long currentUserId) {
        Agreement parent = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));
        enforceAgreementDraftVisibility(parent, currentUserId);

        return agreementVersionRepository.findByAgreementId(agreementId)
                .stream()
                .filter(v -> v.getApprovalStatus() != ApprovalStatus.DRAFT
                        || v.getAgreement().getOwner().getId().equals(currentUserId))
                .sorted((a, b) -> Integer.compare(a.getVersionNumber(), b.getVersionNumber()))
                .map(this::toVersionResponse)
                .toList();
    }

    @Override
    @Transactional
    public AgreementVersionResponse transferOwnership(Long agreementVersionId, Long newOwnerUserId,
                                                      Long performedByUserId, boolean isAdmin, String comments) {
        return executeOwnershipTransfer(agreementVersionId, newOwnerUserId, performedByUserId, isAdmin, comments, false);
    }

    @Override
    @Transactional
    public AgreementVersionResponse completeApprovedTransfer(Long agreementVersionId, Long newOwnerUserId,
                                                             Long approverId) {
        return executeOwnershipTransfer(agreementVersionId, newOwnerUserId, approverId, false, null, true);
    }

    private AgreementVersionResponse executeOwnershipTransfer(Long agreementVersionId, Long newOwnerUserId,
                                                              Long performedByUserId, boolean isAdmin, String comments,
                                                              boolean fromApprovedRequest) {
        AgreementVersion version = agreementVersionRepository.findById(agreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreementVersionId));

        Agreement parent = version.getAgreement();
        Long currentOwnerId = parent.getOwner().getId();
        if (!fromApprovedRequest && !isAdmin && !currentOwnerId.equals(performedByUserId)) {
            throw new UnauthorizedException("Only the owner or an admin can transfer ownership");
        }

        User newOwner = userRepository.findById(newOwnerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", newOwnerUserId));

        if (newOwnerUserId.equals(currentOwnerId)) {
            throw new BusinessException("Agreement is already owned by this user");
        }

        String auditNewValue = isAdmin
                ? buildAdminTransferAuditNote(newOwnerUserId, comments)
                : String.valueOf(newOwnerUserId);

        applyOwnershipTransfer(parent, newOwner, performedByUserId, auditNewValue, currentOwnerId);

        AgreementVersion operationalVersion = resolveOperationalVersion(parent);
        Long responseVersionId = operationalVersion != null ? operationalVersion.getId() : agreementVersionId;
        return toVersionResponse(agreementVersionRepository.findById(responseVersionId).orElseThrow());
    }

    @Override
    @Transactional
    public BulkGroupSubmitResponse submitGroupDraftsForApproval(Long groupId, Long currentUserId) {
        List<AgreementVersion> drafts = loadGroupDraftsForSubmit(groupId, currentUserId);
        return groupSubmitTransactionTemplate.execute(status -> {
            List<String> submittedNames = new ArrayList<>();
            for (AgreementVersion version : drafts) {
                AgreementVersion current = agreementVersionRepository.findById(version.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", version.getId()));
                commitDraftSubmit(current, null, currentUserId);
                Agreement parent = current.getAgreement();
                submittedNames.add(resolveAgreementDisplayName(parent));
            }
            return new BulkGroupSubmitResponse(submittedNames.size(), submittedNames);
        });
    }

    /**
     * Read-only validation pass — no draft fields are modified.
     * Throws {@link IncompleteAgreementException} (HTTP 400) on first failure.
     */
    private List<AgreementVersion> loadGroupDraftsForSubmit(Long groupId, Long currentUserId) {
        companyAgreementGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("CompanyAgreementGroup", groupId));

        List<AgreementVersion> drafts = agreementVersionRepository.findLatestDraftVersionsByGroupId(groupId);
        if (drafts.isEmpty()) {
            throw new IncompleteAgreementException("No draft agreements found in this group to submit");
        }

        for (AgreementVersion version : drafts) {
            validateAgreementOwnership(version.getAgreement(), currentUserId);
            validateCompleteAgreement(version);
        }
        return drafts;
    }

    @Override
    @Transactional
    public AgreementVersionResponse submitForApproval(Long agreementVersionId, String comments, Long currentUserId) {
        AgreementVersion version = loadAndValidateOwnership(agreementVersionId, currentUserId);
        if (version.getVersionNumber() > 1 && (comments == null || comments.isBlank())) {
            throw new BusinessException("Reason for edit/revision is required when submitting a revised version");
        }
        version = applySubmitForApproval(version, comments, currentUserId);
        return toVersionResponse(version);
    }

    @Override
    @Transactional
    public AgreementVersionResponse approve(Long agreementVersionId, String remarks, Long approverId) {
        AgreementVersion version = agreementVersionRepository.findById(agreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreementVersionId));

        if (version.getApprovalStatus() != ApprovalStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only PENDING_APPROVAL agreements can be approved");
        }
        if (version.getAgreement().getOwner().getId().equals(approverId)) {
            throw new AccessDeniedException("Separation of Duties violation: Cannot approve your own agreement.");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new ResourceNotFoundException("User", approverId));

        ApprovalStatus before = version.getApprovalStatus();
        version.setApprovalStatus(ApprovalStatus.APPROVED);
        version.setApprovedBy(approver);
        version.setApprovalDate(LocalDateTime.now());
        version.setUpdatedByUserId(approverId);
        version = agreementVersionRepository.save(version);

        Agreement parent = version.getAgreement();
        parent.setCurrentVersionId(version.getId());
        parent.setUpdatedByUserId(approverId);
        agreementRepository.save(parent);

        recordApproval(version, ApprovalAction.APPROVED, remarks, before, ApprovalStatus.APPROVED, approverId);
        recordAudit(parent.getId(), agreementVersionId, "APPROVED", before.name(), ApprovalStatus.APPROVED.name(), approverId);

        return toVersionResponse(version);
    }

    @Override
    @Transactional
    public AgreementVersionResponse reject(Long agreementVersionId, String remarks, Long approverId) {
        AgreementVersion version = agreementVersionRepository.findById(agreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreementVersionId));

        if (version.getApprovalStatus() != ApprovalStatus.PENDING_APPROVAL) {
            throw new BusinessException("Only PENDING_APPROVAL agreements can be rejected");
        }
        if (version.getAgreement().getOwner().getId().equals(approverId)) {
            throw new AccessDeniedException("Separation of Duties violation: Cannot approve your own agreement.");
        }

        ApprovalStatus before = version.getApprovalStatus();
        version.setApprovalStatus(ApprovalStatus.REJECTED);
        version.setUpdatedByUserId(approverId);
        version = agreementVersionRepository.save(version);

        recordApproval(version, ApprovalAction.REJECTED, remarks, before, ApprovalStatus.REJECTED, approverId);
        recordAudit(version.getAgreement().getId(), agreementVersionId, "REJECTED",
                before.name(), ApprovalStatus.REJECTED.name(), approverId);

        return toVersionResponse(version);
    }

    @Override
    @Transactional
    public AgreementVersionResponse terminate(Long agreementVersionId, TerminateAgreementRequest request,
                                              Long currentUserId) {
        AgreementVersion version = agreementVersionRepository.findById(agreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreementVersionId));

        if (version.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException("Only APPROVED agreements can be terminated");
        }

        version.setTerminationDate(request.terminationDate());
        version.setTerminationReason(request.terminationReason());
        version.setUpdatedByUserId(currentUserId);
        version = agreementVersionRepository.save(version);

        recordAudit(version.getAgreement().getId(), agreementVersionId, "TERMINATED",
                null, request.terminationReason(), currentUserId);

        return toVersionResponse(version);
    }

    @Override
    @Transactional
    public AgreementVersionResponse toggleAgreementInProgress(Long agreementId, Long currentUserId) {
        Agreement parent = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));

        if (parent.getCurrentVersionId() == null) {
            throw new BusinessException("No active version exists for this agreement");
        }

        AgreementVersion version = agreementVersionRepository.findById(parent.getCurrentVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", parent.getCurrentVersionId()));

        loadAndValidateOwnership(version.getId(), currentUserId);
        assertNotTerminated(version, TERMINATED_EDIT_MSG);

        if (version.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException("In-progress flag can only be toggled on approved agreements");
        }

        boolean nextFlag = !version.isInProgressFlag();
        version.setInProgressFlag(nextFlag);
        version.setInProgressSince(nextFlag ? LocalDateTime.now() : null);
        version.setUpdatedByUserId(currentUserId);
        version = agreementVersionRepository.save(version);

        return toVersionResponse(version);
    }

    @Override
    @Transactional
    public RenewAgreementResponse renewAgreement(Long agreementId, Long currentUserId) {
        Agreement parent = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));

        if (parent.getCurrentVersionId() == null) {
            throw new BusinessException("No active version exists to renew from");
        }

        AgreementVersion source = agreementVersionRepository.findById(parent.getCurrentVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", parent.getCurrentVersionId()));

        assertNotTerminated(source, TERMINATED_RENEW_MSG);

        loadAndValidateOwnership(source.getId(), currentUserId);

        if (source.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException("Only approved agreements can be renewed");
        }

        Integer maxVersion = agreementVersionRepository.findMaxVersionByAgreementId(agreementId);
        User owner = parent.getOwner();

        AgreementVersion newVersion = AgreementVersion.builder()
                .agreement(parent)
                .versionNumber(maxVersion + 1)
                .owner(owner)
                .incomeType(source.getIncomeType())
                .agreementType(source.getAgreementType())
                .commercialStructure(source.getCommercialStructure())
                .commercialValue(source.getCommercialValue())
                .calculationFormula(source.getCalculationFormula())
                .startDate(source.getStartDate())
                .expiryDate(source.getExpiryDate())
                .financialYearStartMonth(source.getFinancialYearStartMonth())
                .approvalStatus(ApprovalStatus.DRAFT)
                .notes(source.getNotes())
                .build();
        newVersion.setCreatedByUserId(currentUserId);
        newVersion = agreementVersionRepository.save(newVersion);

        copyVendors(source.getId(), newVersion, currentUserId);
        copyRulesAndComputed(source.getId(), newVersion, currentUserId);
        copySlabs(source.getId(), newVersion, currentUserId);

        recordAudit(parent.getId(), newVersion.getId(), "AGREEMENT_RENEWED",
                String.valueOf(source.getVersionNumber()), String.valueOf(newVersion.getVersionNumber()), currentUserId);

        return new RenewAgreementResponse(
                newVersion.getId(),
                parent.getId(),
                parent.getCompanyAgreementGroup().getId()
        );
    }

    @Override
    @Transactional
    public void deleteDraftAgreement(Long agreementId, Long currentUserId) {
        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("Agreement", agreementId));

        List<AgreementVersion> versions = agreementVersionRepository.findByAgreementId(agreementId);
        if (versions.isEmpty()) {
            throw new BusinessException("Agreement has no versions to delete");
        }

        boolean everApproved = versions.stream()
                .anyMatch(version -> version.getApprovalStatus() == ApprovalStatus.APPROVED);
        if (everApproved) {
            throw new BusinessException("Cannot delete an agreement that has been approved");
        }

        AgreementVersion activeVersion = resolveActiveVersion(agreement, versions);
        if (activeVersion.getApprovalStatus() != ApprovalStatus.DRAFT) {
            throw new BusinessException("Only draft agreements can be deleted");
        }

        loadAndValidateOwnership(activeVersion.getId(), currentUserId);

        for (AgreementVersion version : versions) {
            hardDeleteAgreementVersion(version.getId());
        }
        auditRepository.deleteByAgreementId(agreementId);
        agreement.getStates().clear();
        agreementRepository.saveAndFlush(agreement);
        agreementRepository.delete(agreement);
    }

    private AgreementVersion resolveActiveVersion(Agreement agreement, List<AgreementVersion> versions) {
        if (agreement.getCurrentVersionId() != null) {
            return agreementVersionRepository.findById(agreement.getCurrentVersionId())
                    .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreement.getCurrentVersionId()));
        }
        return versions.stream()
                .max((left, right) -> Integer.compare(left.getVersionNumber(), right.getVersionNumber()))
                .orElseThrow(() -> new BusinessException("Agreement has no versions to delete"));
    }

    private void hardDeleteAgreementVersion(Long versionId) {
        jbpCommercialPeriodRepository.deleteByAgreementVersionId(versionId);
        jbpConfigurationRepository.deleteByAgreementVersionId(versionId);
        jbpVersionFrequencyRepository.deleteByAgreementVersionId(versionId);
        slabRepository.deleteByAgreementVersionId(versionId);
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
        storeMappingRepository.deleteByAgreementVersionId(versionId);
        assetPayoutPeriodRepository.deleteByAgreementVersionId(versionId);
        agreementVersionRepository.deleteById(versionId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AgreementVersionResponse> getPendingApprovals(String search, Pageable pageable) {
        String term = (search != null && !search.isBlank()) ? search.trim() : null;
        return agreementVersionRepository.findAllPendingApproval(term, pageable)
                .map(this::toVersionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalTimelineResponse> getApprovalTimeline(Long agreementVersionId) {
        List<ApprovalTimelineResponse> approvalEntries = approvalRepository
                .findByAgreementVersionIdOrderByCreatedAtAsc(agreementVersionId)
                .stream()
                .map(a -> new ApprovalTimelineResponse(
                        a.getId(), a.getAction(), null, a.getRemarks(),
                        a.getApprovalStatusBefore(), a.getApprovalStatusAfter(),
                        a.getCreatedByUserId(), resolveUserName(a.getCreatedByUserId()),
                        a.getCreatedAt()))
                .toList();

        List<ApprovalTimelineResponse> operationalEntries = actionRequestRepository
                .findByAgreementVersion_IdOrderByCreatedAtAsc(agreementVersionId)
                .stream()
                .flatMap(r -> mapActionRequestToTimeline(r).stream())
                .toList();

        List<ApprovalTimelineResponse> combined = new ArrayList<>(approvalEntries.size() + operationalEntries.size());
        combined.addAll(approvalEntries);
        combined.addAll(operationalEntries);
        combined.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));
        return combined;
    }

    @Override
    @Transactional
    public void bulkTransferOwnership(Long fromUserId, Long toUserId, List<Long> agreementIds,
                                      Long performedByUserId) {
        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", toUserId));

        List<Agreement> agreementsToTransfer;
        if (agreementIds != null && !agreementIds.isEmpty()) {
            agreementsToTransfer = agreementRepository.findAllById(agreementIds).stream()
                    .filter(a -> a.getOwner().getId().equals(fromUserId))
                    .filter(this::isAgreementOperationallyActive)
                    .toList();
        } else {
            agreementsToTransfer = agreementRepository.findByOwnerId(fromUserId, Pageable.unpaged())
                    .stream()
                    .filter(this::isAgreementOperationallyActive)
                    .toList();
        }

        String auditNewValue = buildAdminTransferAuditNote(toUserId, "Bulk admin reassignment");
        for (Agreement agreement : agreementsToTransfer) {
            applyOwnershipTransfer(agreement, toUser, performedByUserId, auditNewValue, fromUserId);
        }
    }

    private void assertNotTerminated(AgreementVersion version, String message) {
        if (version.getTerminationDate() != null) {
            throw new BusinessException(message);
        }
    }

    private AgreementVersion resolveNewVersionSource(Agreement parent) {
        Long agreementId = parent.getId();
        Integer maxVersion = agreementVersionRepository.findMaxVersionByAgreementId(agreementId);
        AgreementVersion latest = agreementVersionRepository
                .findByAgreementIdAndVersionNumber(agreementId, maxVersion)
                .orElseThrow(() -> new BusinessException("No agreement version exists for this agreement"));

        if (latest.getApprovalStatus() == ApprovalStatus.REJECTED) {
            return latest;
        }

        if (parent.getCurrentVersionId() == null) {
            throw new BusinessException("No active version exists to create a new version from");
        }

        AgreementVersion current = agreementVersionRepository.findById(parent.getCurrentVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", parent.getCurrentVersionId()));

        if (current.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BusinessException("Can only create new version from an APPROVED or REJECTED agreement");
        }

        return current;
    }

    private AgreementVersion loadAndValidateOwnership(Long agreementVersionId, Long userId) {
        AgreementVersion version = agreementVersionRepository.findByIdWithAgreementOwner(agreementVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", agreementVersionId));
        validateAgreementOwnership(version.getAgreement(), userId);
        return version;
    }

    private void validateAgreementOwnership(Agreement agreement, Long userId) {
        if (!agreement.getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("You are not the owner of this agreement");
        }
    }

    private void enforceDraftVisibility(AgreementVersion version, Long currentUserId) {
        if (version.getApprovalStatus() == ApprovalStatus.DRAFT
                && !version.getAgreement().getOwner().getId().equals(currentUserId)) {
            throw new ResourceNotFoundException("AgreementVersion", version.getId());
        }
    }

    private void enforceAgreementDraftVisibility(Agreement parent, Long currentUserId) {
        List<AgreementVersion> latestBatch =
                agreementVersionRepository.findLatestVersionsForAgreementIds(List.of(parent.getId()));
        AgreementVersion latest = latestBatch.isEmpty() ? null : latestBatch.get(0);
        if (latest != null
                && latest.getApprovalStatus() == ApprovalStatus.DRAFT
                && !parent.getOwner().getId().equals(currentUserId)
                && parent.getCurrentVersionId() == null) {
            throw new ResourceNotFoundException("Agreement", parent.getId());
        }
    }

    private AgreementVersion resolveListDisplayVersion(Agreement parent, AgreementVersion latest,
                                                       Map<Long, AgreementVersion> currentVersionById,
                                                       Long currentUserId) {
        if (parent.getCurrentVersionId() != null) {
            AgreementVersion current = currentVersionById.get(parent.getCurrentVersionId());
            if (current != null) {
                return current;
            }
        }
        return resolveVisibleLatest(parent, latest, currentUserId);
    }

    private AgreementVersion resolveVisibleLatest(Agreement parent, AgreementVersion latest, Long currentUserId) {
        if (latest == null) {
            return null;
        }
        if (latest.getApprovalStatus() != ApprovalStatus.DRAFT
                || parent.getOwner().getId().equals(currentUserId)) {
            return latest;
        }
        if (parent.getCurrentVersionId() != null) {
            return agreementVersionRepository.findById(parent.getCurrentVersionId()).orElse(latest);
        }
        return agreementVersionRepository.findByAgreementId(parent.getId()).stream()
                .filter(v -> v.getApprovalStatus() != ApprovalStatus.DRAFT
                        || parent.getOwner().getId().equals(currentUserId))
                .max((a, b) -> Integer.compare(a.getVersionNumber(), b.getVersionNumber()))
                .orElse(null);
    }

    private AgreementVersion buildDraftVersion(DraftAgreementItemRequest item, User owner, Agreement parent,
                                               int versionNumber, Long userId) {
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
        AgreementVersion version = AgreementVersion.builder()
                .agreement(parent)
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
        version.setCreatedByUserId(userId);
        return version;
    }

    private void applyDraftFields(AgreementVersion version, DraftDetailsPayload details,
                                  DraftCommercialsPayload commercials) {
        if (details != null) {
            if (details.startDate() != null && details.expiryDate() != null
                    && details.expiryDate().isBefore(details.startDate())) {
                throw new BusinessException("Expiry date must be on or after start date");
            }
            if (details.incomeTypeId() != null) {
                version.setIncomeType(incomeTypeRepository.findById(details.incomeTypeId())
                        .orElseThrow(() -> new ResourceNotFoundException("IncomeType", details.incomeTypeId())));
            }
            if (details.agreementTypeId() != null) {
                version.setAgreementType(agreementTypeRepository.findById(details.agreementTypeId())
                        .orElseThrow(() -> new ResourceNotFoundException("AgreementType", details.agreementTypeId())));
            }
            if (details.startDate() != null) {
                version.setStartDate(details.startDate());
            }
            if (details.expiryDate() != null) {
                version.setExpiryDate(details.expiryDate());
            }
            if (details.notes() != null) {
                version.setNotes(details.notes());
            }
            if (details.adhocSubType() != null) {
                version.setAdhocSubType(AdHocSubType.valueOf(details.adhocSubType()));
            }
            if (details.quantityCap() != null) {
                version.setQuantityCap(details.quantityCap());
            }
            if (details.invoiceVendorId() != null) {
                version.setInvoiceVendor(vendorMasterRepository.findById(details.invoiceVendorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Vendor", details.invoiceVendorId())));
            }
            if (details.paymentRealizationType() != null) {
                PaymentRealizationType paymentType = PaymentRealizationType.valueOf(details.paymentRealizationType());
                version.setPaymentRealizationType(paymentType);
                if (paymentType == PaymentRealizationType.INVOICE_DISCOUNT) {
                    version.setPayoutBufferDays(null);
                    version.setLeadTimeBasis(null);
                    version.setInvoiceGenerationLeadTime(null);
                } else if (paymentType == PaymentRealizationType.CREDIT_NOTE) {
                    version.setLeadTimeBasis(null);
                    version.setInvoiceGenerationLeadTime(null);
                }
            }
            if (details.payoutBufferDays() != null) {
                version.setPayoutBufferDays(details.payoutBufferDays());
            } else if (details.paymentRealizationType() != null
                    && PaymentRealizationType.INVOICE_DISCOUNT.name().equals(details.paymentRealizationType())) {
                version.setPayoutBufferDays(null);
            }
            if (details.leadTimeBasis() != null) {
                version.setLeadTimeBasis(LeadTimeBasis.valueOf(details.leadTimeBasis()));
            } else if (details.paymentRealizationType() != null
                    && !PaymentRealizationType.DIRECT_PAYMENT_INVOICE.name().equals(details.paymentRealizationType())) {
                version.setLeadTimeBasis(null);
            }
            if (details.invoiceGenerationLeadTime() != null) {
                version.setInvoiceGenerationLeadTime(details.invoiceGenerationLeadTime());
            } else if (details.paymentRealizationType() != null
                    && !PaymentRealizationType.DIRECT_PAYMENT_INVOICE.name().equals(details.paymentRealizationType())) {
                version.setInvoiceGenerationLeadTime(null);
            } else if (details.leadTimeBasis() != null
                    && !LeadTimeBasis.INVOICE_DATE.name().equals(details.leadTimeBasis())) {
                version.setInvoiceGenerationLeadTime(null);
            }
            if (details.calculationBasis() != null) {
                version.setCalculationBasis(CalculationBasis.valueOf(details.calculationBasis()));
            }
        }
        Long incomeTypeId = details != null && details.incomeTypeId() != null
                ? details.incomeTypeId()
                : version.getIncomeType() != null ? version.getIncomeType().getId() : null;
        boolean assetRental = isAssetRentalIncomeType(incomeTypeId);
        if (commercials != null && !assetRental) {
            CommercialStructure incomingStructure = commercials.commercialStructure();
            if (incomingStructure != null) {
                version.setCommercialStructure(incomingStructure);
                if (incomingStructure == CommercialStructure.FLAT) {
                    if (commercials.commercialValue() != null) {
                        version.setCommercialValue(commercials.commercialValue());
                    }
                } else if (incomingStructure == CommercialStructure.SLAB) {
                    version.setCommercialValue(null);
                }
            } else if (commercials.commercialValue() != null) {
                version.setCommercialValue(commercials.commercialValue());
            }
            if (commercials.flatValueType() != null) {
                version.setFlatValueType(commercials.flatValueType());
            }
            if (commercials.flatBaselineFrequency() != null) {
                version.setFlatBaselineFrequency(commercials.flatBaselineFrequency());
            }
            if (commercials.calculationFormula() != null) {
                version.setCalculationFormula(commercials.calculationFormula());
            }
            if (commercials.financialYearStartMonth() != null) {
                version.setFinancialYearStartMonth(com.medplus.agreement_tracker_backend.util.DynamicFinancialYearPeriodGenerator
                        .resolveStartMonth(commercials.financialYearStartMonth()));
            }
        } else if (assetRental) {
            version.setCommercialStructure(null);
            version.setCommercialValue(null);
            version.setFlatValueType(null);
            version.setFlatBaselineFrequency(null);
            version.setCalculationFormula(null);
        }
    }

    private void validateStep1Fields(UpdateDraftRequest request) {
        DraftDetailsPayload details = request.details();
        if (details == null || details.incomeTypeId() == null) {
            throw new BusinessException("Income type is required");
        }
        if (details.agreementTypeId() == null) {
            throw new BusinessException("Agreement type is required");
        }
        if (details.startDate() == null) {
            throw new BusinessException("Start date is required");
        }
        if (details.expiryDate() == null) {
            throw new BusinessException("Expiry date is required");
        }
        if (details.expiryDate().isBefore(details.startDate())) {
            throw new BusinessException("Expiry date must be on or after start date");
        }
    }

    private static final ProductRulesPayload EMPTY_PRODUCT_RULES =
            new ProductRulesPayload(List.of(), List.of(), List.of());

    private void validateStep2Fields(UpdateDraftRequest request) {
        validateStep1Fields(request);
        DraftDetailsPayload details = request.details();
        if (details == null || details.incomeTypeId() == null) {
            throw new BusinessException("Income type is required");
        }
        Long incomeTypeId = details.incomeTypeId();
        if (isAssetRentalIncomeType(incomeTypeId)) {
            validateAssetRentalStep2(request);
        } else if (isDataFeeIncomeType(incomeTypeId)) {
            validateDataFeeStep2(request);
        } else if (isCommercialContractsIncomeType(incomeTypeId)) {
            validateCommercialContractsStep2(request, details);
        } else if (isAdHocIncomeType(incomeTypeId)) {
            validateAdHocPayload(request, details);
        } else {
            validateProductsAndVendorsStep2(request);
        }
        validateSettlementRouting(details, request.vendorIds(), isAssetRentalIncomeType(incomeTypeId));
    }

    private void validateAssetRentalStep2(UpdateDraftRequest request) {
        if (request.vendorIds() != null && !request.vendorIds().isEmpty()) {
            throw new BusinessException("Supply vendors are not applicable for Asset Rentals");
        }
        ProductRulesPayload rulesPayload = request.productRules();
        if (rulesPayload != null) {
            boolean hasProducts = rulesPayload.productRules() != null && !rulesPayload.productRules().isEmpty();
            boolean hasManufacturers = rulesPayload.manufacturers() != null && !rulesPayload.manufacturers().isEmpty();
            boolean hasDivisions = rulesPayload.divisionRules() != null && !rulesPayload.divisionRules().isEmpty();
            if (hasProducts || hasManufacturers || hasDivisions) {
                throw new BusinessException("Product scope is not applicable for Asset Rentals");
            }
        }
        validateAssetRentalConfiguration(request.asset(), request.details());
    }

    private void validateDataFeeStep2(UpdateDraftRequest request) {
        validateProductsAndVendorsStep2(request);
        DraftDetailsPayload details = request.details();
        if (details == null || details.stateIds() == null || details.stateIds().isEmpty()) {
            throw new BusinessException("At least one state is required for Data Fee");
        }
    }

    private void validateCommercialContractsStep2(UpdateDraftRequest request, DraftDetailsPayload details) {
        validateProductsAndVendorsStep2(request);
        if (details.stateIds() == null || details.stateIds().isEmpty()) {
            throw new BusinessException("At least one state is required for Commercial Contracts");
        }
    }

    private void validateProductsAndVendorsStep2(UpdateDraftRequest request) {
        if (request.vendorIds() == null || request.vendorIds().isEmpty()) {
            throw new BusinessException("At least one vendor is required");
        }
        ProductRulesPayload rulesPayload = request.productRules();
        List<RuleDTO> productRules = rulesPayload != null && rulesPayload.productRules() != null
                ? rulesPayload.productRules() : List.of();
        if (productRules.isEmpty()) {
            throw new BusinessException("At least one product rule is required");
        }
    }

    private void validateSettlementRouting(DraftDetailsPayload details, List<Long> vendorIds, boolean assetRental) {
        if (details == null) {
            throw new BusinessException("Settlement details are required");
        }
        if (details.paymentRealizationType() == null || details.paymentRealizationType().isBlank()) {
            throw new BusinessException("Payment realization type is required");
        }
        String paymentType = details.paymentRealizationType();
        if (PaymentRealizationType.INVOICE_DISCOUNT.name().equals(paymentType)) {
            return;
        }
        if (PaymentRealizationType.CREDIT_NOTE.name().equals(paymentType)) {
            if (details.payoutBufferDays() == null) {
                throw new BusinessException("Payout lead time is required for Credit Note");
            }
            return;
        }
        if (PaymentRealizationType.DIRECT_PAYMENT_INVOICE.name().equals(paymentType)) {
            if (details.leadTimeBasis() == null || details.leadTimeBasis().isBlank()) {
                throw new BusinessException("Lead time basis is required for Invoice");
            }
            LeadTimeBasis basis = LeadTimeBasis.valueOf(details.leadTimeBasis());
            if (basis == LeadTimeBasis.ACTIVITY_COMPLETION_DATE && details.payoutBufferDays() == null) {
                throw new BusinessException("Payout lead time is required for Activity Completion Date basis");
            }
            if (basis == LeadTimeBasis.INVOICE_DATE) {
                if (details.invoiceGenerationLeadTime() == null) {
                    throw new BusinessException("Invoice generation lead time is required for Invoice date basis");
                }
                if (details.payoutBufferDays() == null) {
                    throw new BusinessException("Payout lead time is required for Invoice date basis");
                }
            }
        }
        if (assetRental) {
            return;
        }
        if (details.calculationBasis() == null || details.calculationBasis().isBlank()) {
            throw new BusinessException("Calculation basis is required");
        }
    }

    private void validateAssetRentalConfiguration(DraftAssetPayload asset, DraftDetailsPayload details) {
        if (asset == null || asset.assetCategory() == null) {
            throw new BusinessException("Asset category is required for Asset Rentals");
        }
        if (asset.assetCategory() == AssetCategory.ACTIVITY) {
            return;
        }
        if (asset.assetType() == null || asset.assetType().isBlank()) {
            throw new BusinessException("Asset type is required for Asset Rentals");
        }
        if (details.stateIds() == null || details.stateIds().isEmpty()) {
            throw new BusinessException("At least one state is required for Asset Rentals");
        }
        if (asset.storeCount() == null || asset.storeCount() <= 0) {
            throw new BusinessException("Number of participating stores is required for Asset Rentals");
        }
    }

    private void validateAssetRentalPayout(DraftAssetPayload asset) {
        if (asset == null) {
            throw new BusinessException("Asset payout amount is required for Asset Rentals");
        }
        boolean hasFlatPayout = asset.flatPayout() != null && asset.flatPayout().signum() > 0;
        boolean hasSchedule = asset.assetPayoutPeriods() != null && !asset.assetPayoutPeriods().isEmpty();
        if (!hasFlatPayout && !hasSchedule) {
            throw new BusinessException("Asset payout amount is required for Asset Rentals");
        }
        if (hasFlatPayout && hasSchedule) {
            throw new BusinessException("Choose either flat payout or per-store payout schedule, not both");
        }
        if (hasSchedule) {
            for (AssetPayoutPeriodDto period : asset.assetPayoutPeriods()) {
                if (period.periodMonths() == null || period.periodMonths() <= 0) {
                    throw new BusinessException("Each payout period must have a valid month count");
                }
                if (period.payoutPerStore() == null || period.payoutPerStore().signum() <= 0) {
                    throw new BusinessException("Each payout period must have a payout per store amount");
                }
            }
        }
    }

    private void validateCommercialStructureFields(Long agreementVersionId, UpdateDraftRequest request) {
        DraftDetailsPayload details = request.details();
        if (details == null || details.incomeTypeId() == null) {
            throw new BusinessException("Income type is required");
        }
        Long incomeTypeId = details.incomeTypeId();
        if (isAssetRentalIncomeType(incomeTypeId)) {
            validateAssetRentalPayout(request.asset());
            return;
        }
        if (isDataFeeIncomeType(incomeTypeId)) {
            validateDataFeeCommercials(agreementVersionId, incomeTypeId, request.commercials());
            return;
        }
        if (isAdHocIncomeType(incomeTypeId)) {
            validateHybridCommercials(agreementVersionId, incomeTypeId, request.commercials());
            validateQpsOneTimeFrequency(details, request.commercials());
            return;
        }
        if (isCommercialContractsIncomeType(incomeTypeId)) {
            validateCommercialContractsCommercials(agreementVersionId, request.commercials());
            return;
        }
        validateHybridCommercials(agreementVersionId, incomeTypeId, request.commercials());
    }

    private void validateCommercialContractsCommercials(
            Long agreementVersionId,
            DraftCommercialsPayload commercials) {
        if (commercials == null) {
            throw new BusinessException("Commercial configuration is required");
        }
        if (resolveEnableFlatBaseline(commercials)) {
            if (commercials.commercialValue() == null) {
                throw new BusinessException("Flat baseline value is required when flat payout is enabled");
            }
            if (commercials.flatBaselineFrequency() == null) {
                throw new BusinessException("Flat baseline frequency is required when flat payout is enabled");
            }
            return;
        }
        if (resolveEnableSlabIncentives(commercials)
                || commercials.commercialStructure() == CommercialStructure.SLAB) {
            validateJbpMatrixPresent(agreementVersionId);
            return;
        }
        throw new BusinessException(
                "Select Flat Baseline Payout or Slab-Based Complex Incentive (JBP)");
    }

    private void validateDataFeeCommercials(
            Long agreementVersionId,
            Long incomeTypeId,
            DraftCommercialsPayload commercials) {
        validateHybridCommercials(agreementVersionId, incomeTypeId, commercials);
    }

    private void validateHybridCommercials(
            Long agreementVersionId,
            Long incomeTypeId,
            DraftCommercialsPayload commercials) {
        if (commercials == null) {
            throw new BusinessException("Commercial configuration is required");
        }
        boolean enableFlat = resolveEnableFlatBaseline(commercials);
        boolean enableSlab = resolveEnableSlabIncentives(commercials);
        if (!enableFlat && !enableSlab) {
            throw new BusinessException("Enable at least one commercial component (flat baseline or slab incentives)");
        }
        if (enableFlat && commercials.commercialValue() == null) {
            throw new BusinessException("Flat baseline value is required when flat payout is enabled");
        }
        if (enableFlat && commercials.flatBaselineFrequency() == null) {
            throw new BusinessException("Flat baseline frequency is required when flat payout is enabled");
        }
        if (enableSlab) {
            validateLegacySlabStructureForStep(agreementVersionId, incomeTypeId);
        }
    }

    private void validateJbpMatrixPresent(Long agreementVersionId) {
        boolean hasJbpConfig = jbpConfigurationRepository.existsByAgreementVersionId(agreementVersionId);
        boolean hasJbpPeriods = jbpCommercialPeriodRepository.existsByAgreementVersionId(agreementVersionId);
        if (!hasJbpConfig || !hasJbpPeriods) {
            throw new BusinessException(
                    "JBP Matrix Configuration is missing. Please populate and upload the custom workbook.");
        }
    }

    private void validateLegacySlabStructureForStep(Long agreementVersionId, Long incomeTypeId) {
        if (isCommercialContractsIncomeType(incomeTypeId)) {
            validateJbpMatrixPresent(agreementVersionId);
            return;
        }
        if (slabRepository.findByAgreementVersionIdOrderByMinCapAsc(agreementVersionId).isEmpty()) {
            throw new BusinessException("Please add at least one slab row, or disable Slab-Based Incentives");
        }
    }

    private void validateSlabStructureForSubmit(AgreementVersion version, String agreementName) {
        Long incomeTypeId = version.getIncomeType() != null ? version.getIncomeType().getId() : null;
        if (isCommercialContractsIncomeType(incomeTypeId)) {
            try {
                validateJbpMatrixPresent(version.getId());
            } catch (BusinessException ex) {
                throw validationFailure(agreementName, ex.getMessage());
            }
            return;
        }
        if (slabRepository.findByAgreementVersionIdOrderByMinCapAsc(version.getId()).isEmpty()) {
            throw validationFailure(agreementName, "Missing slabs for slab-based incentives.");
        }
    }

    private void validateQpsOneTimeFrequency(DraftDetailsPayload details, DraftCommercialsPayload commercials) {
        if (details == null || commercials == null) {
            return;
        }
        if (!"QPS".equals(details.adhocSubType())) {
            return;
        }
        if (resolveEnableFlatBaseline(commercials)
                && commercials.flatBaselineFrequency() != PayoutFrequency.ONE_TIME) {
            throw new BusinessException("QPS agreements require One-Time payout frequency");
        }
    }

    private UpdateDraftRequest scrubRequestForIncomeType(UpdateDraftRequest request, Long incomeTypeId) {
        if (incomeTypeId == null) {
            return request;
        }
        if (isAssetRentalIncomeType(incomeTypeId)) {
            return new UpdateDraftRequest(
                    request.agreementName(),
                    request.companyId(),
                    List.of(),
                    EMPTY_PRODUCT_RULES,
                    scrubDetailsForAssetRental(request.details()),
                    null,
                    request.asset(),
                    request.requiresReapproval());
        }
        if (isDataFeeIncomeType(incomeTypeId)) {
            return new UpdateDraftRequest(
                    request.agreementName(),
                    request.companyId(),
                    request.vendorIds(),
                    request.productRules(),
                    scrubDetailsForDataFee(request.details()),
                    request.commercials(),
                    null,
                    request.requiresReapproval());
        }
        if (isCommercialContractsIncomeType(incomeTypeId)) {
            return new UpdateDraftRequest(
                    request.agreementName(),
                    request.companyId(),
                    request.vendorIds(),
                    request.productRules(),
                    scrubDetailsForStandardContract(request.details()),
                    request.commercials(),
                    null,
                    request.requiresReapproval());
        }
        if (isAdHocIncomeType(incomeTypeId)) {
            return new UpdateDraftRequest(
                    request.agreementName(),
                    request.companyId(),
                    request.vendorIds(),
                    request.productRules(),
                    scrubDetailsForAdHoc(request.details()),
                    scrubCommercialsForAdHoc(request.commercials(), request.details()),
                    null,
                    request.requiresReapproval());
        }
        return new UpdateDraftRequest(
                request.agreementName(),
                request.companyId(),
                request.vendorIds(),
                request.productRules(),
                scrubDetailsForStandardContract(request.details()),
                request.commercials(),
                null,
                request.requiresReapproval());
    }

    private DraftDetailsPayload scrubDetailsForAssetRental(DraftDetailsPayload details) {
        if (details == null) {
            return null;
        }
        return new DraftDetailsPayload(
                details.incomeTypeId(),
                details.agreementTypeId(),
                details.startDate(),
                details.expiryDate(),
                details.notes(),
                details.stateIds(),
                null,
                null,
                details.invoiceVendorId(),
                details.payoutBufferDays(),
                details.leadTimeBasis(),
                details.invoiceGenerationLeadTime(),
                null,
                details.paymentRealizationType());
    }

    private DraftDetailsPayload scrubDetailsForDataFee(DraftDetailsPayload details) {
        if (details == null) {
            return null;
        }
        return new DraftDetailsPayload(
                details.incomeTypeId(),
                details.agreementTypeId(),
                details.startDate(),
                details.expiryDate(),
                details.notes(),
                details.stateIds(),
                null,
                null,
                details.invoiceVendorId(),
                details.payoutBufferDays(),
                details.leadTimeBasis(),
                details.invoiceGenerationLeadTime(),
                details.calculationBasis(),
                details.paymentRealizationType());
    }

    private DraftDetailsPayload scrubDetailsForStandardContract(DraftDetailsPayload details) {
        if (details == null) {
            return null;
        }
        return new DraftDetailsPayload(
                details.incomeTypeId(),
                details.agreementTypeId(),
                details.startDate(),
                details.expiryDate(),
                details.notes(),
                details.stateIds(),
                null,
                null,
                details.invoiceVendorId(),
                details.payoutBufferDays(),
                details.leadTimeBasis(),
                details.invoiceGenerationLeadTime(),
                details.calculationBasis(),
                details.paymentRealizationType());
    }

    private DraftDetailsPayload scrubDetailsForAdHoc(DraftDetailsPayload details) {
        if (details == null) {
            return null;
        }
        String resolvedSubType = details.adhocSubType();
        if (resolvedSubType == null || resolvedSubType.isBlank()
                || "CONSUMER_PRICE_OFF".equals(resolvedSubType)) {
            resolvedSubType = "QPS";
        }
        return new DraftDetailsPayload(
                details.incomeTypeId(),
                details.agreementTypeId(),
                details.startDate(),
                details.expiryDate(),
                details.notes(),
                details.stateIds(),
                resolvedSubType,
                null,
                details.invoiceVendorId(),
                details.payoutBufferDays(),
                details.leadTimeBasis(),
                details.invoiceGenerationLeadTime(),
                details.calculationBasis(),
                details.paymentRealizationType());
    }

    private DraftCommercialsPayload scrubCommercialsForFlatOnly(DraftCommercialsPayload commercials) {
        if (commercials == null) {
            return null;
        }
        return new DraftCommercialsPayload(
                CommercialStructure.FLAT,
                commercials.commercialValue(),
                commercials.flatValueType(),
                commercials.flatBaselineFrequency(),
                true,
                false,
                null,
                commercials.financialYearStartMonth());
    }

    private DraftCommercialsPayload scrubCommercialsForAdHoc(DraftCommercialsPayload commercials,
                                                               DraftDetailsPayload details) {
        if (commercials == null) {
            return null;
        }
        if (details != null && "QPS".equals(details.adhocSubType())) {
            return new DraftCommercialsPayload(
                    commercials.commercialStructure(),
                    commercials.commercialValue(),
                    commercials.flatValueType(),
                    PayoutFrequency.ONE_TIME,
                    commercials.enableFlatBaseline(),
                    commercials.enableSlabIncentives(),
                    commercials.calculationFormula(),
                    commercials.financialYearStartMonth());
        }
        return commercials;
    }

    private boolean resolveEnableFlatBaseline(DraftCommercialsPayload commercials) {
        if (commercials.enableFlatBaseline() != null) {
            return Boolean.TRUE.equals(commercials.enableFlatBaseline());
        }
        CommercialStructure structure = commercials.commercialStructure();
        return structure == CommercialStructure.FLAT;
    }

    private boolean resolveEnableSlabIncentives(DraftCommercialsPayload commercials) {
        if (commercials.enableSlabIncentives() != null) {
            return Boolean.TRUE.equals(commercials.enableSlabIncentives());
        }
        return commercials.commercialStructure() == CommercialStructure.SLAB;
    }

    private void validateAssetRentalPayload(DraftAssetPayload asset, DraftDetailsPayload details) {
        validateAssetRentalConfiguration(asset, details);
        validateAssetRentalPayout(asset);
    }

    private boolean isAdHocIncomeType(Long incomeTypeId) {
        if (incomeTypeId == null) {
            return false;
        }
        return incomeTypeRepository.findById(incomeTypeId)
                .map(incomeType -> IncomeTypeNames.AD_HOC_ACTIVITIES.equalsIgnoreCase(incomeType.getName()))
                .orElse(false);
    }

    private boolean isDataFeeIncomeType(Long incomeTypeId) {
        if (incomeTypeId == null) {
            return false;
        }
        return incomeTypeRepository.findById(incomeTypeId)
                .map(incomeType -> IncomeTypeNames.DATA_FEE.equalsIgnoreCase(incomeType.getName()))
                .orElse(false);
    }

    private boolean isCommercialContractsIncomeType(Long incomeTypeId) {
        if (incomeTypeId == null) {
            return false;
        }
        return incomeTypeRepository.findById(incomeTypeId)
                .map(incomeType -> IncomeTypeNames.COMMERCIAL_CONTRACTS.equalsIgnoreCase(incomeType.getName()))
                .orElse(false);
    }

    private void validateAdHocPayload(UpdateDraftRequest request, DraftDetailsPayload details) {
        String subType = details.adhocSubType();
        if (subType == null || subType.isBlank() || "CONSUMER_PRICE_OFF".equals(subType)) {
            subType = "QPS";
        }
        if (!"QPS".equals(subType)) {
            throw new BusinessException("Ad-Hoc activity sub-type must be QPS");
        }
        ProductRulesPayload rulesPayload = request.productRules();
        List<RuleDTO> productRules = rulesPayload != null && rulesPayload.productRules() != null
                ? rulesPayload.productRules() : List.of();
        if (productRules.isEmpty()) {
            throw new BusinessException("At least one product rule is required for QPS");
        }
    }

    private boolean isAssetRentalIncomeType(Long incomeTypeId) {
        if (incomeTypeId == null) {
            return false;
        }
        return incomeTypeRepository.findById(incomeTypeId)
                .map(incomeType -> IncomeTypeNames.ASSET_RENTALS.equalsIgnoreCase(incomeType.getName()))
                .orElse(false);
    }

    private boolean hasPersistableAssetPayload(DraftAssetPayload payload) {
        if (payload == null || payload.assetCategory() == null) {
            return false;
        }
        if (payload.assetCategory() == AssetCategory.ACTIVITY) {
            return true;
        }
        return payload.assetType() != null && !payload.assetType().isBlank();
    }

    private boolean shouldPersistAsset(DraftAssetPayload payload, boolean validateStep2) {
        if (hasPersistableAssetPayload(payload)) {
            return true;
        }
        // Partial Step 1 save (validateStep2=false): defer asset row until Step 2 payload is complete.
        return false;
    }

    private void replaceAsset(AgreementVersion version, DraftAssetPayload payload, Long userId) {
        if (!hasPersistableAssetPayload(payload)) {
            return;
        }
        AgreementAsset asset = assetRepository.findByAgreementVersionId(version.getId())
                .orElseGet(() -> AgreementAsset.builder().agreementVersion(version).build());

        boolean hasFlatPayout = payload.flatPayout() != null && payload.flatPayout().signum() > 0;
        boolean hasSchedule = payload.assetPayoutPeriods() != null && !payload.assetPayoutPeriods().isEmpty();

        if (hasFlatPayout) {
            assetPayoutPeriodRepository.deleteByAgreementVersionId(version.getId());
            asset.setFlatPayout(payload.flatPayout());
            asset.setPayoutPerStore(null);
        } else if (hasSchedule) {
            asset.setFlatPayout(null);
            asset.setPayoutPerStore(null);
            replaceAssetPayoutPeriods(version, payload.assetPayoutPeriods());
        }

        if (payload.assetCategory() != null) {
            asset.setAssetCategory(payload.assetCategory());
        }
        if (payload.assetCategory() == AssetCategory.ACTIVITY) {
            asset.setAssetType(null);
        } else if (payload.assetType() != null) {
            asset.setAssetType(payload.assetType().trim());
        }
        if (payload.storeCount() != null) {
            asset.setStoreCount(payload.storeCount());
        }
        if (payload.remarks() != null) {
            asset.setRemarks(payload.remarks().trim());
        }

        if (asset.getId() == null) {
            asset.setCreatedByUserId(userId);
        }
        asset.setUpdatedByUserId(userId);
        assetRepository.save(asset);
        version.setAsset(asset);
    }

    private void replaceAssetPayoutPeriods(AgreementVersion version, List<AssetPayoutPeriodDto> periods) {
        assetPayoutPeriodRepository.deleteByAgreementVersionId(version.getId());
        if (periods == null || periods.isEmpty()) {
            return;
        }
        for (AssetPayoutPeriodDto period : periods) {
            assetPayoutPeriodRepository.save(AgreementAssetPayoutPeriod.builder()
                    .agreementVersion(version)
                    .periodMonths(period.periodMonths())
                    .payoutPerStore(period.payoutPerStore())
                    .build());
        }
    }

    private void copyAssetPayoutPeriods(Long sourceVersionId, Long targetVersionId) {
        AgreementVersion target = agreementVersionRepository.findById(targetVersionId)
                .orElseThrow(() -> new ResourceNotFoundException("AgreementVersion", targetVersionId));
        assetPayoutPeriodRepository.deleteByAgreementVersionId(targetVersionId);

        List<AgreementAssetPayoutPeriod> sourcePeriods =
                assetPayoutPeriodRepository.findByAgreementVersionIdOrderByPeriodMonthsAscIdAsc(sourceVersionId);
        for (AgreementAssetPayoutPeriod sourcePeriod : sourcePeriods) {
            assetPayoutPeriodRepository.save(AgreementAssetPayoutPeriod.builder()
                    .agreementVersion(target)
                    .periodMonths(sourcePeriod.getPeriodMonths())
                    .payoutPerStore(sourcePeriod.getPayoutPerStore())
                    .build());
        }
    }

    private AgreementVersionResponse.AssetSummary toAssetSummary(AgreementAsset asset) {
        if (asset == null) {
            return null;
        }
        return new AgreementVersionResponse.AssetSummary(
                asset.getAssetCategory() != null ? asset.getAssetCategory().name() : null,
                asset.getAssetType(),
                asset.getStoreCount(),
                asset.getPayoutPerStore(),
                asset.getFlatPayout(),
                asset.getRemarks()
        );
    }

    private void validateCompleteAgreement(AgreementVersion version) {
        Agreement parent = version.getAgreement();
        String agreementName = resolveAgreementDisplayName(parent);

        if (parent.getAgreementName() == null || parent.getAgreementName().isBlank()) {
            throw validationFailure(agreementName, "Missing Agreement Name.");
        }
        if (parent.getCompanyAgreementGroup() == null
                || parent.getCompanyAgreementGroup().getCompany() == null) {
            throw validationFailure(agreementName, "Missing Company.");
        }
        if (version.getStartDate() == null) {
            throw validationFailure(agreementName, "Missing Start Date.");
        }
        if (version.getExpiryDate() == null) {
            throw validationFailure(agreementName, "Missing Expiry Date.");
        }
        if (version.getIncomeType() == null) {
            throw validationFailure(agreementName, "Missing Income Type.");
        }
        if (version.getAgreementType() == null) {
            throw validationFailure(agreementName, "Missing Agreement Type.");
        }
        if (version.getIncomeType() != null
                && isAssetRentalIncomeType(version.getIncomeType().getId())) {
            validateCompleteAssetRental(version, agreementName);
            if (version.getInvoiceVendor() == null) {
                throw validationFailure(agreementName, "Missing Invoice Vendor.");
            }
            return;
        }
        if (version.getCommercialStructure() == null) {
            throw validationFailure(agreementName, "Missing Commercial Structure.");
        }
        if (version.getExpiryDate().isBefore(version.getStartDate())) {
            throw validationFailure(agreementName, "Expiry Date must be on or after Start Date.");
        }
        if (version.getCommercialStructure() == CommercialStructure.FLAT) {
            if (version.getCommercialValue() == null) {
                throw validationFailure(agreementName, "Missing Commercial Value for flat baseline.");
            }
            if (version.getFlatBaselineFrequency() == null) {
                throw validationFailure(agreementName, "Missing flat baseline frequency.");
            }
        }
        if (version.getCommercialStructure() == CommercialStructure.SLAB) {
            validateSlabStructureForSubmit(version, agreementName);
        }
        if (vendorRepository.findByAgreementVersionId(version.getId()).isEmpty()) {
            throw validationFailure(agreementName, "Missing Vendor.");
        }
        if (productRuleRepository.findByAgreementVersionId(version.getId()).isEmpty()) {
            throw validationFailure(agreementName, "Missing Product selection.");
        }
    }

    private void validateCompleteAssetRental(AgreementVersion version, String agreementName) {
        AgreementAsset asset = assetRepository.findByAgreementVersionId(version.getId()).orElse(null);
        if (asset == null || asset.getAssetCategory() == null) {
            throw validationFailure(agreementName, "Missing Asset Category.");
        }
        if (asset.getAssetCategory() != AssetCategory.ACTIVITY
                && (asset.getAssetType() == null || asset.getAssetType().isBlank())) {
            throw validationFailure(agreementName, "Missing Asset Type.");
        }
        if (asset.getStoreCount() == null || asset.getStoreCount() <= 0) {
            throw validationFailure(agreementName, "Missing participating store count.");
        }
        boolean hasFlatPayout = asset.getFlatPayout() != null && asset.getFlatPayout().signum() > 0;
        List<AgreementAssetPayoutPeriod> payoutPeriods = assetPayoutPeriodRepository
                .findByAgreementVersionIdOrderByPeriodMonthsAscIdAsc(version.getId());
        boolean hasSchedule = !payoutPeriods.isEmpty();
        if (!hasFlatPayout && !hasSchedule) {
            throw validationFailure(agreementName, "Missing Asset Payout amount.");
        }
        long mappedStoreCount = storeMappingRepository
                .findByAgreementVersionIdOrderByStoreStoreCodeAsc(version.getId())
                .size();
        if (mappedStoreCount == 0) {
            throw validationFailure(agreementName, "Upload at least one participating store.");
        }
        if (hasSchedule) {
            for (AgreementAssetPayoutPeriod period : payoutPeriods) {
                if (period.getPeriodMonths() == null || period.getPeriodMonths() <= 0) {
                    throw validationFailure(agreementName, "Each payout period must have a valid month count.");
                }
                if (period.getPayoutPerStore() == null || period.getPayoutPerStore().signum() <= 0) {
                    throw validationFailure(agreementName, "Each payout period must have a payout per store amount.");
                }
            }
        }
    }

    private String resolveAgreementDisplayName(Agreement parent) {
        if (parent.getAgreementName() != null && !parent.getAgreementName().isBlank()) {
            return parent.getAgreementName();
        }
        return "Agreement #" + parent.getId();
    }

    private IncompleteAgreementException validationFailure(String agreementName, String reason) {
        return new IncompleteAgreementException(
                "Validation failed for '" + agreementName + "': " + reason);
    }

    private AgreementVersion applySubmitForApproval(AgreementVersion version, String comments, Long userId) {
        if (version.getApprovalStatus() != ApprovalStatus.DRAFT) {
            throw new BusinessException("Only DRAFT agreements can be submitted for approval");
        }
        validateCompleteAgreement(version);
        return commitDraftSubmit(version, comments, userId);
    }

    /** Status transition only — caller must validate completeness first. */
    private AgreementVersion commitDraftSubmit(AgreementVersion version, String comments, Long userId) {
        if (version.getApprovalStatus() != ApprovalStatus.DRAFT) {
            throw new BusinessException("Only DRAFT agreements can be submitted for approval");
        }

        ApprovalStatus before = version.getApprovalStatus();
        version.setApprovalStatus(ApprovalStatus.PENDING_APPROVAL);
        version.setUpdatedByUserId(userId);
        version = agreementVersionRepository.save(version);

        String remarks = comments != null && !comments.isBlank() ? comments.trim() : null;
        recordApproval(version, ApprovalAction.SUBMITTED, remarks, before, ApprovalStatus.PENDING_APPROVAL, userId);
        recordAudit(version.getAgreement().getId(), version.getId(), "SUBMITTED_FOR_APPROVAL",
                before.name(), ApprovalStatus.PENDING_APPROVAL.name(), userId);

        return version;
    }

    private void replaceVendors(AgreementVersion version, List<Long> vendorIds, Long userId) {
        vendorRepository.deleteByAgreementVersionId(version.getId());
        if (vendorIds != null && !vendorIds.isEmpty()) {
            saveVendors(version, vendorIds, userId);
        }
    }

    private void replaceRulesAndComputeProducts(AgreementVersion version, List<Long> manufacturerIds,
                                                List<RuleDTO> divisionRules, List<RuleDTO> productRules,
                                                Long userId) {
        manufacturerRuleRepository.deleteByAgreementVersionId(version.getId());
        divisionRuleRepository.deleteByAgreementVersionId(version.getId());
        productRuleRepository.deleteByAgreementVersionId(version.getId());
        computedProductRepository.deleteByAgreementVersionId(version.getId());
        saveRulesAndComputeProducts(version, manufacturerIds, divisionRules, productRules, userId);
    }

    private void saveVendors(AgreementVersion version, List<Long> vendorIds, Long userId) {
        List<VendorMaster> vendors = vendorMasterRepository.findByIdIn(vendorIds);
        for (VendorMaster vendor : vendors) {
            AgreementVendor av = AgreementVendor.builder()
                    .agreementVersion(version)
                    .vendorId(vendor.getId())
                    .vendorNameSnapshot(vendor.getVendorName())
                    .build();
            av.setCreatedByUserId(userId);
            vendorRepository.save(av);
        }
    }

    private void saveRulesAndComputeProducts(AgreementVersion version, List<Long> manufacturerIds,
                                             List<RuleDTO> divisionRules, List<RuleDTO> productRules, Long userId) {
        List<Long> safeManufacturerIds = manufacturerIds != null ? manufacturerIds : List.of();
        for (Long mfrId : safeManufacturerIds) {
            AgreementManufacturer am = AgreementManufacturer.builder()
                    .agreementVersion(version).manufacturerId(mfrId).build();
            am.setCreatedByUserId(userId);
            manufacturerRuleRepository.save(am);
        }

        List<RuleDTO> safeDivisionRules = divisionRules != null ? divisionRules : List.of();
        for (RuleDTO dr : safeDivisionRules) {
            AgreementDivisionRule adr = AgreementDivisionRule.builder()
                    .agreementVersion(version).divisionId(dr.id())
                    .ruleType(RuleType.valueOf(dr.ruleType())).build();
            adr.setCreatedByUserId(userId);
            divisionRuleRepository.save(adr);
        }

        List<RuleDTO> safeProductRules = productRules != null ? productRules : List.of();
        for (RuleDTO pr : safeProductRules) {
            AgreementProductRule apr = AgreementProductRule.builder()
                    .agreementVersion(version).productId(pr.id())
                    .ruleType(RuleType.valueOf(pr.ruleType())).build();
            apr.setCreatedByUserId(userId);
            productRuleRepository.save(apr);
        }

        List<ProductMaster> baseProducts = safeManufacturerIds.isEmpty()
                ? productMasterRepository.findAllActiveWithRelations()
                : productMasterRepository.findByManufacturerIdsAndIsActiveTrue(safeManufacturerIds);

        List<ProductMaster> afterDivisionFilter = applyDivisionRules(baseProducts, safeDivisionRules);
        List<ProductMaster> finalProducts = applyProductRules(afterDivisionFilter, safeProductRules);

        for (ProductMaster p : finalProducts) {
            AgreementComputedProduct acp = AgreementComputedProduct.builder()
                    .agreementVersion(version)
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
        if (rules.isEmpty()) {
            return products;
        }
        RuleType ruleType = RuleType.valueOf(rules.get(0).ruleType());
        Set<Long> divisionIds = new HashSet<>();
        rules.forEach(r -> divisionIds.add(r.id()));
        if (ruleType == RuleType.INCLUDE) {
            return products.stream().filter(p -> divisionIds.contains(p.getDivision().getId())).toList();
        }
        return products.stream().filter(p -> !divisionIds.contains(p.getDivision().getId())).toList();
    }

    private List<ProductMaster> applyProductRules(List<ProductMaster> products, List<RuleDTO> rules) {
        if (rules.isEmpty()) {
            return products;
        }
        RuleType ruleType = RuleType.valueOf(rules.get(0).ruleType());
        Set<Long> productIds = new HashSet<>();
        rules.forEach(r -> productIds.add(r.id()));
        if (ruleType == RuleType.INCLUDE) {
            return products.stream().filter(p -> productIds.contains(p.getId())).toList();
        }
        return products.stream().filter(p -> !productIds.contains(p.getId())).toList();
    }

    private Map<Long, Long> copySlabs(Long sourceVersionId, AgreementVersion target, Long userId) {
        Map<Long, Long> idMap = new HashMap<>();
        slabRepository.findByAgreementVersionIdOrderByMinCapAsc(sourceVersionId).forEach(s -> {
            AgreementSlab copy = AgreementSlab.builder()
                    .agreementVersion(target)
                    .slabType(s.getSlabType())
                    .minCap(s.getMinCap())
                    .maxCap(s.getMaxCap())
                    .capUnit(s.getCapUnit())
                    .valueType(s.getValueType())
                    .commercialValue(s.getCommercialValue())
                    .payoutFrequency(s.getPayoutFrequency())
                    .build();
            copy.setCreatedByUserId(userId);
            copy = slabRepository.save(copy);
            idMap.put(s.getId(), copy.getId());
        });
        return idMap;
    }

    private Map<Long, Long> copyJbpConfigurations(Long sourceVersionId, AgreementVersion target) {
        Map<Long, Long> idMap = new HashMap<>();
        jbpConfigurationRepository.findHydratedByAgreementVersionId(sourceVersionId).forEach(source -> {
            AgreementJbpConfiguration copy = AgreementJbpConfiguration.builder()
                    .agreementVersion(target)
                    .frequency(source.getFrequency())
                    .slabCount(source.getSlabCount())
                    .build();
            copy = jbpConfigurationRepository.save(copy);
            copy.setSelectedPeriods(new LinkedHashSet<>(source.getSelectedPeriods()));
            jbpConfigurationRepository.save(copy);
            idMap.put(source.getId(), copy.getId());
        });
        return idMap;
    }

    private void copyJbpVersionFrequencies(Long sourceVersionId, AgreementVersion target) {
        jbpVersionFrequencyRepository.findByAgreementVersionIdOrderByFrequencyAsc(sourceVersionId).forEach(source ->
                jbpVersionFrequencyRepository.save(AgreementJbpVersionFrequency.builder()
                        .agreementVersion(target)
                        .frequency(source.getFrequency())
                        .build()));
    }

    private void copyJbpCommercialPeriods(
            Long sourceVersionId,
            AgreementVersion target,
            Map<Long, Long> configurationIdMap) {
        if (configurationIdMap.isEmpty()) {
            return;
        }
        jbpCommercialPeriodRepository.findByAgreementVersionId(sourceVersionId).forEach(sourcePeriod -> {
            Long newConfigurationId = configurationIdMap.get(sourcePeriod.getJbpConfiguration().getId());
            if (newConfigurationId == null) {
                return;
            }
            AgreementJbpConfiguration targetConfiguration = jbpConfigurationRepository.findById(newConfigurationId)
                    .orElseThrow(() -> new ResourceNotFoundException("AgreementJbpConfiguration", newConfigurationId));
            AgreementJbpCommercialPeriod copy = AgreementJbpCommercialPeriod.builder()
                    .agreementVersion(target)
                    .jbpConfiguration(targetConfiguration)
                    .targetType(sourcePeriod.getTargetType())
                    .target(sourcePeriod.getTarget())
                    .qualifierPercent(sourcePeriod.getQualifierPercent())
                    .payoutType(sourcePeriod.getPayoutType())
                    .payout(sourcePeriod.getPayout())
                    .maxPurchase(sourcePeriod.getMaxPurchase())
                    .maxPayout(sourcePeriod.getMaxPayout())
                    .slabTierNumber(sourcePeriod.getSlabTierNumber())
                    .timePeriod(sourcePeriod.getTimePeriod())
                    .parentTimePeriod(sourcePeriod.getParentTimePeriod())
                    .build();
            jbpCommercialPeriodRepository.save(copy);
        });
    }

    private void copyVendors(Long sourceVersionId, AgreementVersion target, Long userId) {
        vendorRepository.findByAgreementVersionId(sourceVersionId).forEach(v -> {
            AgreementVendor copy = AgreementVendor.builder()
                    .agreementVersion(target)
                    .vendorId(v.getVendorId())
                    .vendorNameSnapshot(v.getVendorNameSnapshot())
                    .build();
            copy.setCreatedByUserId(userId);
            vendorRepository.save(copy);
        });
    }

    private void copyRulesAndComputed(Long sourceVersionId, AgreementVersion target, Long userId) {
        manufacturerRuleRepository.findByAgreementVersionId(sourceVersionId).forEach(m -> {
            AgreementManufacturer copy = AgreementManufacturer.builder()
                    .agreementVersion(target).manufacturerId(m.getManufacturerId()).build();
            copy.setCreatedByUserId(userId);
            manufacturerRuleRepository.save(copy);
        });

        divisionRuleRepository.findByAgreementVersionId(sourceVersionId).forEach(dr -> {
            AgreementDivisionRule copy = AgreementDivisionRule.builder()
                    .agreementVersion(target).divisionId(dr.getDivisionId()).ruleType(dr.getRuleType()).build();
            copy.setCreatedByUserId(userId);
            divisionRuleRepository.save(copy);
        });

        productRuleRepository.findByAgreementVersionId(sourceVersionId).forEach(pr -> {
            AgreementProductRule copy = AgreementProductRule.builder()
                    .agreementVersion(target).productId(pr.getProductId()).ruleType(pr.getRuleType()).build();
            copy.setCreatedByUserId(userId);
            productRuleRepository.save(copy);
        });

        computedProductRepository.findByAgreementVersionId(sourceVersionId).forEach(cp -> {
            AgreementComputedProduct copy = AgreementComputedProduct.builder()
                    .agreementVersion(target)
                    .productId(cp.getProductId())
                    .productNameSnapshot(cp.getProductNameSnapshot())
                    .divisionNameSnapshot(cp.getDivisionNameSnapshot())
                    .manufacturerNameSnapshot(cp.getManufacturerNameSnapshot())
                    .build();
            copy.setCreatedByUserId(userId);
            computedProductRepository.save(copy);
        });
    }

    private void recordApproval(AgreementVersion version, ApprovalAction action, String remarks,
                                ApprovalStatus before, ApprovalStatus after, Long userId) {
        AgreementApproval approval = AgreementApproval.builder()
                .agreementVersion(version)
                .action(action)
                .remarks(remarks)
                .approvalStatusBefore(before)
                .approvalStatusAfter(after)
                .build();
        approval.setCreatedByUserId(userId);
        approvalRepository.save(approval);
    }

    private void recordAudit(Long agreementId, Long agreementVersionId, String action,
                             String oldVal, String newVal, Long userId) {
        AgreementAudit audit = AgreementAudit.builder()
                .agreementId(agreementId)
                .agreementVersionId(agreementVersionId)
                .entityType("Agreement")
                .action(action)
                .oldValueJson(oldVal)
                .newValueJson(newVal)
                .createdByUserId(userId)
                .build();
        auditRepository.save(audit);
    }

    private AgreementVersionResponse toVersionResponse(AgreementVersion version) {
        Agreement parent = version.getAgreement();
        CompanyAgreementGroup cag = parent.getCompanyAgreementGroup();
        CompanyMaster company = cag.getCompany();

        List<AgreementVersionResponse.VendorSummary> vendors =
                vendorRepository.findByAgreementVersionId(version.getId())
                        .stream()
                        .map(v -> new AgreementVersionResponse.VendorSummary(v.getVendorId(), v.getVendorNameSnapshot()))
                        .toList();

        List<Long> manufacturerIds = manufacturerRuleRepository.findByAgreementVersionId(version.getId())
                .stream().map(AgreementManufacturer::getManufacturerId).toList();

        List<AgreementVersionResponse.RuleSummary> divisionRules =
                divisionRuleRepository.findByAgreementVersionId(version.getId())
                        .stream()
                        .map(dr -> new AgreementVersionResponse.RuleSummary(dr.getDivisionId(), dr.getRuleType().name()))
                        .toList();

        List<AgreementVersionResponse.RuleSummary> productRules =
                productRuleRepository.findByAgreementVersionId(version.getId())
                        .stream()
                        .map(pr -> new AgreementVersionResponse.RuleSummary(pr.getProductId(), pr.getRuleType().name()))
                        .toList();

        List<AgreementVersionResponse.ProductSummary> products =
                computedProductRepository.findByAgreementVersionId(version.getId())
                        .stream()
                        .map(p -> new AgreementVersionResponse.ProductSummary(
                                p.getProductId(), p.getProductNameSnapshot(),
                                p.getManufacturerNameSnapshot(), p.getDivisionNameSnapshot()))
                        .toList();

        AgreementType agreementType = version.getAgreementType();

        List<AgreementVersionResponse.StateSummary> states = parent.getStates().stream()
                .sorted(Comparator.comparing(StateMaster::getStateName))
                .map(s -> new AgreementVersionResponse.StateSummary(s.getId(), s.getStateName(), s.getStateCode()))
                .toList();
        List<Long> stateIds = states.stream().map(AgreementVersionResponse.StateSummary::id).toList();

        AgreementVersionResponse.AssetSummary assetSummary = assetRepository
                .findByAgreementVersionId(version.getId())
                .map(this::toAssetSummary)
                .orElse(null);

        List<AgreementVersionResponse.StoreMappingSummary> storeMappings =
                storeMappingRepository.findByAgreementVersionIdOrderByStoreStoreCodeAsc(version.getId())
                        .stream()
                        .map(mapping -> new AgreementVersionResponse.StoreMappingSummary(
                                mapping.getId(),
                                mapping.getStore().getId(),
                                mapping.getStore().getStoreCode(),
                                mapping.getStore().getStoreName(),
                                mapping.getStore().getState().getId(),
                                mapping.getStore().getState().getStateName()))
                        .toList();

        List<AgreementVersionResponse.AssetPayoutPeriodSummary> assetPayoutPeriods =
                assetPayoutPeriodRepository.findByAgreementVersionIdOrderByPeriodMonthsAscIdAsc(version.getId())
                        .stream()
                        .map(period -> new AgreementVersionResponse.AssetPayoutPeriodSummary(
                                period.getId(),
                                period.getPeriodMonths(),
                                period.getPayoutPerStore()))
                        .toList();

        return new AgreementVersionResponse(
                version.getId(),
                parent.getId(),
                parent.getAgreementName(),
                version.getVersionNumber(),
                company.getId(),
                company.getCompanyName(),
                cag.getId(),
                cag.getName(),
                version.getOwner().getId(),
                version.getOwner().getFullName(),
                version.getIncomeType() != null ? version.getIncomeType().getId() : null,
                version.getIncomeType() != null ? version.getIncomeType().getName() : null,
                agreementType != null ? agreementType.getId() : null,
                agreementType != null ? agreementType.getName() : null,
                version.getCommercialStructure(),
                version.getCommercialValue(),
                version.getFlatValueType(),
                version.getFlatBaselineFrequency(),
                version.getCalculationFormula(),
                version.getQuantityCap(),
                version.getAdhocSubType() != null ? version.getAdhocSubType().name() : null,
                version.getInvoiceVendor() != null ? version.getInvoiceVendor().getId() : null,
                version.getInvoiceVendor() != null ? version.getInvoiceVendor().getVendorName() : null,
                version.getPayoutBufferDays(),
                version.getLeadTimeBasis(),
                version.getInvoiceGenerationLeadTime(),
                version.getCalculationBasis(),
                version.getPaymentRealizationType(),
                version.getStartDate(),
                version.getExpiryDate(),
                version.getFinancialYearStartMonth(),
                version.getApprovalStatus(),
                statusResolver.resolve(version),
                version.isInProgressFlag(),
                version.getTerminationDate(),
                version.getTerminationReason(),
                version.getNotes(),
                stateIds,
                states,
                vendors,
                manufacturerIds,
                divisionRules,
                productRules,
                products,
                assetSummary,
                storeMappings,
                assetPayoutPeriods,
                jbpCommercialPeriodRepository.existsByAgreementVersionId(version.getId()),
                resolvePendingActionRequest(parent.getId()),
                version.getCreatedAt(),
                version.getUpdatedAt()
        );
    }

    private AgreementResponse toParentResponse(Agreement parent, AgreementVersion visible,
                                               List<AgreementVendor> vendors) {
        CompanyAgreementGroup cag = parent.getCompanyAgreementGroup();
        CompanyMaster company = cag.getCompany();

        List<AgreementVersionResponse.VendorSummary> vendorSummaries = vendors.stream()
                .map(v -> new AgreementVersionResponse.VendorSummary(v.getVendorId(), v.getVendorNameSnapshot()))
                .toList();

        List<AgreementVersion> latestBatch =
                agreementVersionRepository.findLatestVersionsForAgreementIds(List.of(parent.getId()));
        AgreementVersion latest = latestBatch.isEmpty() ? null : latestBatch.get(0);

        AgreementType agreementType = visible.getAgreementType();

        return new AgreementResponse(
                parent.getId(),
                parent.getAgreementName(),
                company.getId(),
                company.getCompanyName(),
                cag.getId(),
                cag.getName(),
                agreementType != null ? agreementType.getId() : null,
                agreementType != null ? agreementType.getName() : null,
                parent.getCurrentVersionId(),
                latest != null ? latest.getId() : null,
                visible.getVersionNumber(),
                statusResolver.resolve(visible),
                visible.getApprovalStatus(),
                parent.isActive(),
                parent.getCreatedAt(),
                visible.getUpdatedAt(),
                visible.getIncomeType() != null ? visible.getIncomeType().getName() : null,
                visible.getStartDate(),
                visible.getExpiryDate(),
                parent.getOwner().getFullName(),
                parent.getOwner().getId(),
                vendorSummaries
        );
    }

    private AgreementResponse toParentResponseEmpty(Agreement parent) {
        CompanyAgreementGroup cag = parent.getCompanyAgreementGroup();
        CompanyMaster company = cag != null ? cag.getCompany() : null;
        User owner = parent.getOwner();
        return new AgreementResponse(
                parent.getId(),
                parent.getAgreementName(),
                company != null ? company.getId() : null,
                company != null ? company.getCompanyName() : null,
                cag != null ? cag.getId() : null,
                cag != null ? cag.getName() : null,
                null,
                null,
                parent.getCurrentVersionId(),
                null,
                null,
                null,
                null,
                parent.isActive(),
                parent.getCreatedAt(),
                null,
                null,
                null,
                null,
                owner != null ? owner.getFullName() : null,
                owner != null ? owner.getId() : null,
                List.of()
        );
    }

    private PendingActionRequestInfo resolvePendingActionRequest(Long agreementId) {
        return actionRequestRepository
                .findFirstByAgreementVersion_Agreement_IdAndStatus(agreementId, ActionRequestStatus.PENDING)
                .map(r -> new PendingActionRequestInfo(
                        r.getId(),
                        r.getActionType(),
                        r.getReasonComments(),
                        r.getRequestedTerminationDate(),
                        r.getTargetUser() != null ? r.getTargetUser().getId() : null,
                        r.getTargetUser() != null ? r.getTargetUser().getFullName() : null,
                        r.getRequestedBy().getFullName(),
                        r.getCreatedAt()))
                .orElse(null);
    }

    private List<ApprovalTimelineResponse> mapActionRequestToTimeline(AgreementActionRequest request) {
        List<ApprovalTimelineResponse> entries = new ArrayList<>(2);
        String actionPrefix = request.getActionType().name();
        entries.add(new ApprovalTimelineResponse(
                request.getId() * 10,
                null,
                actionPrefix + "_REQUESTED",
                request.getReasonComments(),
                null,
                null,
                request.getRequestedBy().getId(),
                request.getRequestedBy().getFullName(),
                request.getCreatedAt()));

        if (request.getResolvedAt() != null) {
            String eventSuffix = request.getStatus() == ActionRequestStatus.APPROVED ? "APPROVED" : "REJECTED";
            String remarks = request.getStatus() == ActionRequestStatus.REJECTED
                    && request.getApproverComments() != null
                    ? request.getApproverComments()
                    : request.getReasonComments();
            Long actorId = request.getResolvedBy() != null ? request.getResolvedBy().getId() : null;
            String actorName = request.getResolvedBy() != null ? request.getResolvedBy().getFullName() : null;
            entries.add(new ApprovalTimelineResponse(
                    request.getId() * 10 + 1,
                    null,
                    actionPrefix + "_" + eventSuffix,
                    remarks,
                    null,
                    null,
                    actorId,
                    actorName,
                    request.getResolvedAt()));
        }
        return entries;
    }

    private String buildAdminTransferAuditNote(Long newOwnerUserId, String comments) {
        StringBuilder note = new StringBuilder("Admin override | newOwner=").append(newOwnerUserId);
        if (comments != null && !comments.isBlank()) {
            note.append(" | reason=").append(comments.trim());
        }
        return note.toString();
    }

    private void applyOwnershipTransfer(Agreement agreement, User newOwner, Long performedByUserId,
                                        String auditNewValue, Long fromOwnerId) {
        agreement.setOwner(newOwner);
        agreement.setUpdatedByUserId(performedByUserId);
        agreementRepository.save(agreement);

        AgreementVersion operationalVersion = resolveOperationalVersion(agreement);
        if (operationalVersion != null) {
            operationalVersion.setOwner(newOwner);
            operationalVersion.setUpdatedByUserId(performedByUserId);
            agreementVersionRepository.save(operationalVersion);
            recordAudit(agreement.getId(), operationalVersion.getId(), "OWNERSHIP_TRANSFERRED",
                    String.valueOf(fromOwnerId), auditNewValue, performedByUserId);
        }
    }

    private AgreementVersion resolveOperationalVersion(Agreement agreement) {
        if (agreement.getCurrentVersionId() != null) {
            return agreementVersionRepository.findById(agreement.getCurrentVersionId()).orElse(null);
        }
        return agreementVersionRepository.findByAgreementId(agreement.getId()).stream()
                .filter(v -> v.getApprovalStatus() == ApprovalStatus.DRAFT
                        || v.getApprovalStatus() == ApprovalStatus.PENDING_APPROVAL)
                .max((a, b) -> Integer.compare(a.getVersionNumber(), b.getVersionNumber()))
                .orElse(null);
    }

    private boolean isAgreementOperationallyActive(Agreement agreement) {
        AgreementVersion operationalVersion = resolveOperationalVersion(agreement);
        return operationalVersion == null || operationalVersion.getTerminationDate() == null;
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).map(User::getFullName).orElse(null);
    }

    private Pageable mapAgreementPageable(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }
        List<Sort.Order> orders = pageable.getSort().stream()
                .map(order -> {
                    String property = switch (order.getProperty()) {
                        case "companyName", "companyAgreementGroupName" -> "createdAt";
                        case "agreementName" -> "agreementName";
                        case "createdAt" -> "createdAt";
                        default -> order.getProperty();
                    };
                    return new Sort.Order(order.getDirection(), property);
                })
                .toList();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }

    private void syncAgreementName(Agreement parent, AgreementVersion version, Long userId) {
        regenerateAgreementName(parent, version);
        parent.setUpdatedByUserId(userId);
        agreementRepository.save(parent);
    }

    private void regenerateAgreementName(Agreement parent, AgreementVersion version) {
        if (version.getIncomeType() == null || version.getStartDate() == null) {
            if (parent.getAgreementName() == null || parent.getAgreementName().isBlank()) {
                parent.setAgreementName(DRAFT_AGREEMENT_NAME_PLACEHOLDER);
            }
            return;
        }
        CompanyAgreementGroup cag = parent.getCompanyAgreementGroup();
        String stateCodes = parent.getStates().stream()
                .map(StateMaster::getStateCode)
                .sorted()
                .collect(Collectors.joining(","));
        StringBuilder generatedName = new StringBuilder(cag.getName())
                .append(" - ")
                .append(version.getIncomeType().getName());
        if (!stateCodes.isBlank()) {
            generatedName.append(" - ").append(stateCodes);
        }
        generatedName.append(" - ").append(version.getStartDate().format(AGREEMENT_NAME_DATE_FORMAT));
        parent.setAgreementName(generatedName.toString());
    }

    private void replaceAgreementStates(Agreement parent, List<Long> stateIds, Long userId) {
        parent.getStates().clear();
        if (stateIds != null && !stateIds.isEmpty()) {
            List<StateMaster> states = stateMasterRepository.findByIdInAndIsActiveTrue(stateIds);
            Set<Long> foundIds = states.stream().map(StateMaster::getId).collect(Collectors.toSet());
            for (Long stateId : stateIds) {
                if (!foundIds.contains(stateId)) {
                    throw new ResourceNotFoundException("StateMaster", stateId);
                }
            }
            parent.getStates().addAll(states);
        }
        parent.setUpdatedByUserId(userId);
        agreementRepository.save(parent);
    }

    private void copyAgreementStates(Agreement source, Agreement target, Long userId) {
        if (source.getStates() == null || source.getStates().isEmpty()) {
            return;
        }
        target.getStates().addAll(source.getStates());
        target.setUpdatedByUserId(userId);
        agreementRepository.save(target);
    }

    private CompanyAgreementGroup resolveCompanyAgreementGroup(Long companyId, Long groupId, String newName,
                                                               Long userId) {
        CompanyAgreementGroupResponse response =
                companyAgreementGroupService.resolveOrCreate(companyId, groupId, newName, userId);
        return companyAgreementGroupRepository.findById(response.id())
                .orElseThrow(() -> new ResourceNotFoundException("CompanyAgreementGroup", response.id()));
    }
}
