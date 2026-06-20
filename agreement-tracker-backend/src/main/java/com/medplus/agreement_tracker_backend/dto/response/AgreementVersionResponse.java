package com.medplus.agreement_tracker_backend.dto.response;

import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.enums.AgreementStatus;
import com.medplus.agreement_tracker_backend.enums.CalculationBasis;
import com.medplus.agreement_tracker_backend.enums.CommercialStructure;
import com.medplus.agreement_tracker_backend.enums.PaymentRealizationType;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import com.medplus.agreement_tracker_backend.enums.SlabValueType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AgreementVersionResponse(
        Long id,
        Long agreementId,
        String agreementName,
        Integer versionNumber,
        Long companyId,
        String companyName,
        Long companyAgreementGroupId,
        String companyAgreementGroupName,
        Long ownerId,
        String ownerName,
        Long incomeTypeId,
        String incomeTypeName,
        Long agreementTypeId,
        String agreementTypeName,
        CommercialStructure commercialStructure,
        BigDecimal commercialValue,
        SlabValueType flatValueType,
        PayoutFrequency flatBaselineFrequency,
        String calculationFormula,
        BigDecimal quantityCap,
        String adhocSubType,
        Long invoiceVendorId,
        String invoiceVendorName,
        Integer payoutBufferDays,
        CalculationBasis calculationBasis,
        PaymentRealizationType paymentRealizationType,
        LocalDate startDate,
        LocalDate expiryDate,
        ApprovalStatus approvalStatus,
        AgreementStatus computedStatus,
        boolean inProgressFlag,
        LocalDate terminationDate,
        String terminationReason,
        String notes,
        List<Long> stateIds,
        List<StateSummary> states,
        List<VendorSummary> vendors,
        List<Long> manufacturerIds,
        List<RuleSummary> divisionRules,
        List<RuleSummary> productRules,
        List<ProductSummary> products,
        AssetSummary asset,
        PendingActionRequestInfo pendingActionRequest,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record StateSummary(Long id, String stateName, String stateCode) {}
    public record VendorSummary(Long vendorId, String vendorName) {}
    public record ProductSummary(Long productId, String productName, String manufacturerName, String divisionName) {}
    public record RuleSummary(Long id, String ruleType) {}
    public record AssetSummary(
            String assetCategory,
            String assetType,
            Integer storeCount,
            BigDecimal payoutPerStore,
            BigDecimal flatPayout,
            String remarks
    ) {}
}
