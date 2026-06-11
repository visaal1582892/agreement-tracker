package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.Agreement;
import com.medplus.agreement_tracker_backend.entity.CompanyAgreementGroup;
import com.medplus.agreement_tracker_backend.entity.User;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CompanyAgreementGroupSpec {

    private CompanyAgreementGroupSpec() {}

    public static Specification<CompanyAgreementGroup> withFilters(
            Long companyId, Boolean isActive, String groupName, String lastModifiedBy, String createdBy) {
        Specification<CompanyAgreementGroup> spec = unrestricted();
        Specification<CompanyAgreementGroup> companySpec = hasCompanyId(companyId);
        if (companySpec != null) {
            spec = spec.and(companySpec);
        }
        Specification<CompanyAgreementGroup> activeSpec = hasActiveStatus(isActive);
        if (activeSpec != null) {
            spec = spec.and(activeSpec);
        }
        Specification<CompanyAgreementGroup> nameSpec = hasGroupName(groupName);
        if (nameSpec != null) {
            spec = spec.and(nameSpec);
        }
        Specification<CompanyAgreementGroup> modifierSpec = hasLastModifiedBy(lastModifiedBy);
        if (modifierSpec != null) {
            spec = spec.and(modifierSpec);
        }
        Specification<CompanyAgreementGroup> creatorSpec = hasCreatedBy(createdBy);
        if (creatorSpec != null) {
            spec = spec.and(creatorSpec);
        }
        return spec;
    }

    public static Specification<CompanyAgreementGroup> unrestricted() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<CompanyAgreementGroup> visibleTo(Long userId) {
        return (root, query, cb) -> {
            Predicate isCreator = cb.equal(root.get("createdByUserId"), userId);

            Subquery<Long> ownedGroupIds = query.subquery(Long.class);
            Root<Agreement> agreement = ownedGroupIds.from(Agreement.class);
            ownedGroupIds.select(agreement.get("companyAgreementGroup").get("id"))
                    .where(cb.equal(agreement.get("owner").get("id"), userId));

            return cb.or(isCreator, root.get("id").in(ownedGroupIds));
        };
    }

    private static Specification<CompanyAgreementGroup> hasCompanyId(Long companyId) {
        if (companyId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("company").get("id"), companyId);
    }

    private static Specification<CompanyAgreementGroup> hasActiveStatus(Boolean isActive) {
        if (isActive == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
    }

    private static Specification<CompanyAgreementGroup> hasGroupName(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + value.toLowerCase() + "%");
    }

    private static Specification<CompanyAgreementGroup> hasLastModifiedBy(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String pattern = "%" + value.toLowerCase() + "%";
        return (root, query, cb) -> {
            Subquery<Long> matchingUsers = query.subquery(Long.class);
            Root<User> user = matchingUsers.from(User.class);
            matchingUsers.select(user.get("id"))
                    .where(cb.or(
                            cb.like(cb.lower(user.get("fullName")), pattern),
                            cb.like(cb.lower(user.get("username")), pattern)));

            Predicate updatedByMatches = cb.and(
                    cb.isNotNull(root.get("updatedByUserId")),
                    root.get("updatedByUserId").in(matchingUsers));
            Predicate createdByMatches = cb.and(
                    cb.isNull(root.get("updatedByUserId")),
                    root.get("createdByUserId").in(matchingUsers));
            return cb.or(updatedByMatches, createdByMatches);
        };
    }

    private static Specification<CompanyAgreementGroup> hasCreatedBy(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String pattern = "%" + value.toLowerCase() + "%";
        return (root, query, cb) -> {
            Subquery<Long> matchingUsers = query.subquery(Long.class);
            Root<User> user = matchingUsers.from(User.class);
            matchingUsers.select(user.get("id"))
                    .where(cb.or(
                            cb.like(cb.lower(user.get("fullName")), pattern),
                            cb.like(cb.lower(user.get("username")), pattern)));
            return root.get("createdByUserId").in(matchingUsers);
        };
    }
}
