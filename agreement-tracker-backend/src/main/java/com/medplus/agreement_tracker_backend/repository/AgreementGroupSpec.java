package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.Agreement;
import com.medplus.agreement_tracker_backend.entity.AgreementGroup;
import com.medplus.agreement_tracker_backend.entity.AgreementVendor;
import com.medplus.agreement_tracker_backend.entity.User;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class AgreementGroupSpec {

    private AgreementGroupSpec() {}

    public static Specification<AgreementGroup> withFilters(
            String agreementNumber, String companyName, String status, String ownerName,
            Long vendorId, Long incomeTypeId) {
        Specification<AgreementGroup> spec = unrestricted();
        Specification<AgreementGroup> agreementNumberSpec = hasAgreementNumber(agreementNumber);
        if (agreementNumberSpec != null) {
            spec = spec.and(agreementNumberSpec);
        }
        Specification<AgreementGroup> companyNameSpec = hasCompanyName(companyName);
        if (companyNameSpec != null) {
            spec = spec.and(companyNameSpec);
        }
        Specification<AgreementGroup> statusSpec = hasApprovalStatus(status);
        if (statusSpec != null) {
            spec = spec.and(statusSpec);
        }
        Specification<AgreementGroup> ownerNameSpec = hasOwnerName(ownerName);
        if (ownerNameSpec != null) {
            spec = spec.and(ownerNameSpec);
        }
        Specification<AgreementGroup> vendorSpec = hasVendorId(vendorId);
        if (vendorSpec != null) {
            spec = spec.and(vendorSpec);
        }
        Specification<AgreementGroup> incomeTypeSpec = hasIncomeTypeId(incomeTypeId);
        if (incomeTypeSpec != null) {
            spec = spec.and(incomeTypeSpec);
        }
        return spec;
    }

    public static Specification<AgreementGroup> ownedBy(Long userId) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Agreement> a = sub.from(Agreement.class);
            sub.select(a.get("agreementGroup").get("id"))
               .where(cb.equal(a.get("owner").get("id"), userId));
            return root.get("id").in(sub);
        };
    }

    /** Matches all groups — used when no column filters are active. */
    public static Specification<AgreementGroup> unrestricted() {
        return (root, query, cb) -> cb.conjunction();
    }

    private static Specification<AgreementGroup> hasAgreementNumber(String value) {
        if (!StringUtils.hasText(value)) return null;
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("agreementNumber")), "%" + value.toLowerCase() + "%");
    }

    private static Specification<AgreementGroup> hasCompanyName(String value) {
        if (!StringUtils.hasText(value)) return null;
        return (root, query, cb) -> {
            Join<Object, Object> company = root.join("company", JoinType.INNER);
            return cb.like(cb.lower(company.get("companyName")), "%" + value.toLowerCase() + "%");
        };
    }

    private static Specification<AgreementGroup> hasApprovalStatus(String status) {
        if (!StringUtils.hasText(status)) return null;
        ApprovalStatus parsed;
        try {
            parsed = ApprovalStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
        final ApprovalStatus finalStatus = parsed;
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Agreement> a = sub.from(Agreement.class);
            sub.select(a.get("agreementGroup").get("id"))
               .where(cb.equal(a.get("approvalStatus"), finalStatus));
            return root.get("id").in(sub);
        };
    }

    private static Specification<AgreementGroup> hasOwnerName(String value) {
        if (!StringUtils.hasText(value)) return null;
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Agreement> a = sub.from(Agreement.class);
            Join<Agreement, User> owner = a.join("owner", JoinType.INNER);
            sub.select(a.get("agreementGroup").get("id"))
               .where(cb.like(cb.lower(owner.get("fullName")), "%" + value.toLowerCase() + "%"));
            return root.get("id").in(sub);
        };
    }

    private static Specification<AgreementGroup> hasVendorId(Long vendorId) {
        if (vendorId == null) return null;
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<AgreementVendor> av = sub.from(AgreementVendor.class);
            Join<AgreementVendor, Agreement> agreement = av.join("agreement", JoinType.INNER);
            sub.select(agreement.get("agreementGroup").get("id"))
               .where(cb.equal(av.get("vendorId"), vendorId));
            return root.get("id").in(sub);
        };
    }

    private static Specification<AgreementGroup> hasIncomeTypeId(Long incomeTypeId) {
        if (incomeTypeId == null) return null;
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Agreement> a = sub.from(Agreement.class);
            sub.select(a.get("agreementGroup").get("id"))
               .where(cb.equal(a.get("incomeType").get("id"), incomeTypeId));
            return root.get("id").in(sub);
        };
    }
}
