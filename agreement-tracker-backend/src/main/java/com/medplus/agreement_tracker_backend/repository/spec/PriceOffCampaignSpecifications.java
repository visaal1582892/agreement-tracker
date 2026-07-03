package com.medplus.agreement_tracker_backend.repository.spec;

import com.medplus.agreement_tracker_backend.entity.ConsumerPriceOffCampaign;
import com.medplus.agreement_tracker_backend.enums.PriceOffApprovalStatus;
import com.medplus.agreement_tracker_backend.enums.PriceOffDiscountType;
import com.medplus.agreement_tracker_backend.enums.PriceOffDisplayStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class PriceOffCampaignSpecifications {

    private PriceOffCampaignSpecifications() {}

    @SafeVarargs
    public static Specification<ConsumerPriceOffCampaign> combine(Specification<ConsumerPriceOffCampaign>... specs) {
        return Stream.of(specs)
                .filter(Objects::nonNull)
                .reduce(Specification::and)
                .orElse((root, query, cb) -> cb.conjunction());
    }

    public static Specification<ConsumerPriceOffCampaign> withProductFilter(String product) {
        if (product == null || product.isBlank()) {
            return null;
        }
        String pattern = "%" + product.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            var productJoin = root.join("product", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("productName")), pattern),
                    cb.like(cb.lower(productJoin.get("productCode")), pattern));
        };
    }

    public static Specification<ConsumerPriceOffCampaign> withCampaignIdFilter(String campaignId) {
        return withLike("campaignId", campaignId);
    }

    public static Specification<ConsumerPriceOffCampaign> withLocationFilter(String location) {
        return withLike("locationLabel", location);
    }

    public static Specification<ConsumerPriceOffCampaign> withChannelFilter(String channel) {
        return withLike("channelLabel", channel);
    }

    public static Specification<ConsumerPriceOffCampaign> withDiscountTypeFilter(PriceOffDiscountType discountType) {
        if (discountType == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("discountType"), discountType);
    }

    private static Specification<ConsumerPriceOffCampaign> withLike(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String pattern = "%" + value.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get(field)), pattern);
    }

    /** @deprecated use column filters instead */
    public static Specification<ConsumerPriceOffCampaign> withSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            var productJoin = root.join("product", JoinType.LEFT);
            return cb.or(
                    cb.like(cb.lower(root.get("productName")), pattern),
                    cb.like(cb.lower(productJoin.get("productCode")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("campaignId"), "")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("remarks"), "")), pattern));
        };
    }

    public static Specification<ConsumerPriceOffCampaign> withDisplayStatus(PriceOffDisplayStatus status) {
        if (status == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            switch (status) {
                case DRAFT -> predicates.add(cb.equal(root.get("approvalStatus"), PriceOffApprovalStatus.DRAFT));
                case PENDING_APPROVAL -> predicates.add(
                        cb.equal(root.get("approvalStatus"), PriceOffApprovalStatus.PENDING_APPROVAL));
                case REJECTED -> predicates.add(
                        cb.equal(root.get("approvalStatus"), PriceOffApprovalStatus.REJECTED));
                case APPROVED -> {
                    predicates.add(cb.equal(root.get("approvalStatus"), PriceOffApprovalStatus.APPROVED));
                    predicates.add(cb.greaterThan(root.get("startDate"), today));
                    predicates.add(notCompleted(root, cb, today));
                }
                case PENDING_ACTIVATION -> {
                    predicates.add(cb.equal(root.get("approvalStatus"), PriceOffApprovalStatus.APPROVED));
                    predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), today));
                    predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), today));
                    predicates.add(cb.or(
                            cb.isNull(root.get("campaignId")),
                            cb.equal(root.get("campaignId"), "")));
                    predicates.add(notCapReached(root, cb));
                }
                case LIVE -> {
                    predicates.add(cb.equal(root.get("approvalStatus"), PriceOffApprovalStatus.APPROVED));
                    predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), today));
                    predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), today));
                    predicates.add(cb.isNotNull(root.get("campaignId")));
                    predicates.add(cb.notEqual(root.get("campaignId"), ""));
                    predicates.add(notCapReached(root, cb));
                }
                case COMPLETED -> {
                    predicates.add(cb.equal(root.get("approvalStatus"), PriceOffApprovalStatus.APPROVED));
                    predicates.add(completed(root, cb, today));
                }
                default -> predicates.add(cb.disjunction());
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate notCapReached(
            jakarta.persistence.criteria.Root<ConsumerPriceOffCampaign> root,
            jakarta.persistence.criteria.CriteriaBuilder cb) {
        return cb.or(
                cb.isNull(root.get("maxUnitCap")),
                cb.lessThan(root.get("unitsConsumed"), root.get("maxUnitCap")));
    }

    private static Predicate completed(
            jakarta.persistence.criteria.Root<ConsumerPriceOffCampaign> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            LocalDate today) {
        return cb.or(
                cb.lessThan(root.get("endDate"), today),
                cb.and(
                        cb.isNotNull(root.get("maxUnitCap")),
                        cb.greaterThanOrEqualTo(root.get("unitsConsumed"), root.get("maxUnitCap"))));
    }

    private static Predicate notCompleted(
            jakarta.persistence.criteria.Root<ConsumerPriceOffCampaign> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            LocalDate today) {
        return cb.not(completed(root, cb, today));
    }
}
