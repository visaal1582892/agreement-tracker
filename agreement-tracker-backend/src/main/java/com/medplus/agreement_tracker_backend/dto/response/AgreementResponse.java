package com.medplus.agreement_tracker_backend.dto.response;

import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import com.medplus.agreement_tracker_backend.enums.AgreementStatus;
import com.medplus.agreement_tracker_backend.enums.CommercialStructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AgreementResponse(
        Long id,
        Long agreementGroupId,
        String agreementNumber,
        Integer versionNumber,
        Long companyId,
        String companyName,
        Long ownerId,
        String ownerName,
        Long incomeTypeId,
        String incomeTypeName,
        Long agreementTypeId,
        String agreementTypeName,
        CommercialStructure commercialStructure,
        BigDecimal commercialValue,
        String calculationFormula,
        LocalDate startDate,
        LocalDate expiryDate,
        ApprovalStatus approvalStatus,
        AgreementStatus computedStatus,
        boolean inProgressFlag,
        LocalDate terminationDate,
        String terminationReason,
        String notes,
        List<VendorSummary> vendors,
        List<Long> manufacturerIds,
        List<RuleSummary> divisionRules,
        List<RuleSummary> productRules,
        List<ProductSummary> products,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record VendorSummary(Long vendorId, String vendorName) {}
    public record ProductSummary(Long productId, String productName, String manufacturerName, String divisionName) {}
    public record RuleSummary(Long id, String ruleType) {}
}
