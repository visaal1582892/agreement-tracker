package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.Agreement;
import com.medplus.agreement_tracker_backend.entity.AgreementVendor;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.entity.User;
import com.medplus.agreement_tracker_backend.enums.ApprovalStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class AgreementSpec {

    private AgreementSpec() {}

    public static Specification<Agreement> withFilters(
            Long companyId, Long companyAgreementGroupId, String companyAgreementGroupName,
            String agreementName, String status, String ownerName,
            Long vendorId, Long incomeTypeId,
            LocalDate startDateFrom, LocalDate startDateTo,
            LocalDate endDateFrom, LocalDate endDateTo) {
        Specification<Agreement> spec = unrestricted();
        Specification<Agreement> agreementNameSpec = hasAgreementName(agreementName);
        if (agreementNameSpec != null) {
            spec = spec.and(agreementNameSpec);
        }
        Specification<Agreement> companyIdSpec = hasCompanyId(companyId);
        if (companyIdSpec != null) {
            spec = spec.and(companyIdSpec);
        }
        Specification<Agreement> cagIdSpec = hasCompanyAgreementGroupId(companyAgreementGroupId);
        if (cagIdSpec != null) {
            spec = spec.and(cagIdSpec);
        }
        Specification<Agreement> cagNameSpec = hasCompanyAgreementGroupName(companyAgreementGroupName);
        if (cagNameSpec != null) {
            spec = spec.and(cagNameSpec);
        }
        Specification<Agreement> statusSpec = hasApprovalStatus(status);
        if (statusSpec != null) {
            spec = spec.and(statusSpec);
        }
        Specification<Agreement> ownerNameSpec = hasOwnerName(ownerName);
        if (ownerNameSpec != null) {
            spec = spec.and(ownerNameSpec);
        }
        Specification<Agreement> vendorSpec = hasVendorId(vendorId);
        if (vendorSpec != null) {
            spec = spec.and(vendorSpec);
        }
        Specification<Agreement> incomeTypeSpec = hasIncomeTypeId(incomeTypeId);
        if (incomeTypeSpec != null) {
            spec = spec.and(incomeTypeSpec);
        }
        Specification<Agreement> dateSpec = hasCurrentVersionDateRange(
                startDateFrom, startDateTo, endDateFrom, endDateTo);
        if (dateSpec != null) {
            spec = spec.and(dateSpec);
        }
        return spec;
    }

    public static Specification<Agreement> ownedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), userId);
    }

    public static Specification<Agreement> draftVisibleTo(Long userId) {
        return (root, query, cb) -> {
            Subquery<Long> hidden = query.subquery(Long.class);
            Root<AgreementVersion> av = hidden.from(AgreementVersion.class);

            Subquery<Integer> maxVer = query.subquery(Integer.class);
            Root<AgreementVersion> avMax = maxVer.from(AgreementVersion.class);
            maxVer.select(cb.max(avMax.get("versionNumber")))
                    .where(cb.equal(avMax.get("agreement").get("id"), root.get("id")));

            hidden.select(av.get("agreement").get("id"))
                    .where(
                            cb.equal(av.get("agreement").get("id"), root.get("id")),
                            cb.equal(av.get("versionNumber"), maxVer),
                            cb.equal(av.get("approvalStatus"), ApprovalStatus.DRAFT),
                            cb.notEqual(root.get("owner").get("id"), userId),
                            cb.isNull(root.get("currentVersionId"))
                    );

            return cb.not(root.get("id").in(hidden));
        };
    }

    public static Specification<Agreement> unrestricted() {
        return (root, query, cb) -> cb.conjunction();
    }

    private static Specification<Agreement> hasAgreementName(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("agreementName")), "%" + value.toLowerCase() + "%");
    }

    private static Specification<Agreement> hasCompanyId(Long companyId) {
        if (companyId == null) {
            return null;
        }
        return (root, query, cb) -> {
            Join<Object, Object> cag = root.join("companyAgreementGroup", JoinType.INNER);
            return cb.equal(cag.get("company").get("id"), companyId);
        };
    }

    private static Specification<Agreement> hasCompanyAgreementGroupId(Long companyAgreementGroupId) {
        if (companyAgreementGroupId == null) {
            return null;
        }
        return (root, query, cb) ->
                cb.equal(root.get("companyAgreementGroup").get("id"), companyAgreementGroupId);
    }

    private static Specification<Agreement> hasCompanyAgreementGroupName(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return (root, query, cb) -> {
            Join<Object, Object> cag = root.join("companyAgreementGroup", JoinType.INNER);
            return cb.like(cb.lower(cag.get("name")), "%" + value.toLowerCase() + "%");
        };
    }

    private static Specification<Agreement> hasApprovalStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        ApprovalStatus parsed;
        try {
            parsed = ApprovalStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
        final ApprovalStatus finalStatus = parsed;
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<AgreementVersion> av = sub.from(AgreementVersion.class);
            sub.select(av.get("agreement").get("id"))
               .where(cb.equal(av.get("approvalStatus"), finalStatus));
            return root.get("id").in(sub);
        };
    }

    private static Specification<Agreement> hasOwnerName(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return (root, query, cb) -> {
            Join<Agreement, User> owner = root.join("owner", JoinType.INNER);
            return cb.like(cb.lower(owner.get("fullName")), "%" + value.toLowerCase() + "%");
        };
    }

    private static Specification<Agreement> hasVendorId(Long vendorId) {
        if (vendorId == null) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<AgreementVendor> av = sub.from(AgreementVendor.class);
            Join<AgreementVendor, AgreementVersion> version = av.join("agreementVersion", JoinType.INNER);
            sub.select(version.get("agreement").get("id"))
               .where(cb.equal(av.get("vendorId"), vendorId));
            return root.get("id").in(sub);
        };
    }

    private static Specification<Agreement> hasIncomeTypeId(Long incomeTypeId) {
        if (incomeTypeId == null) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<AgreementVersion> av = sub.from(AgreementVersion.class);
            sub.select(av.get("agreement").get("id"))
               .where(cb.equal(av.get("incomeType").get("id"), incomeTypeId));
            return root.get("id").in(sub);
        };
    }

    private static Specification<Agreement> hasCurrentVersionDateRange(
            LocalDate startDateFrom, LocalDate startDateTo,
            LocalDate endDateFrom, LocalDate endDateTo) {
        if (startDateFrom == null && startDateTo == null && endDateFrom == null && endDateTo == null) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<AgreementVersion> av = sub.from(AgreementVersion.class);

            Subquery<Integer> maxVer = query.subquery(Integer.class);
            Root<AgreementVersion> avMax = maxVer.from(AgreementVersion.class);
            maxVer.select(cb.max(avMax.get("versionNumber")))
                    .where(cb.equal(avMax.get("agreement").get("id"), root.get("id")));

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(av.get("agreement").get("id"), root.get("id")));
            predicates.add(cb.or(
                    cb.equal(av.get("id"), root.get("currentVersionId")),
                    cb.and(
                            cb.isNull(root.get("currentVersionId")),
                            cb.equal(av.get("versionNumber"), maxVer)
                    )
            ));

            if (startDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(av.get("startDate"), startDateFrom));
            }
            if (startDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(av.get("startDate"), startDateTo));
            }
            if (endDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(av.get("expiryDate"), endDateFrom));
            }
            if (endDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(av.get("expiryDate"), endDateTo));
            }

            sub.select(av.get("agreement").get("id")).where(predicates.toArray(Predicate[]::new));
            return root.get("id").in(sub);
        };
    }
}
