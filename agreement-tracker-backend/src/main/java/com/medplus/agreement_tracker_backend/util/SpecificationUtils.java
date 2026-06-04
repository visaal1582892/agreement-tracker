package com.medplus.agreement_tracker_backend.util;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class SpecificationUtils {

    private SpecificationUtils() {}

    /** Base specification that matches all rows (avoids ambiguous Specification.where(null)). */
    public static <T> Specification<T> empty() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static <T> Specification<T> stringLike(String field, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase().trim() + "%");
        };
    }

    public static <T> Specification<T> booleanEquals(String field, Boolean value) {
        return (root, query, cb) -> {
            if (value == null) return cb.conjunction();
            return cb.equal(root.get(field), value);
        };
    }

    public static <T> Specification<T> longEquals(String field, Long value) {
        return (root, query, cb) -> {
            if (value == null) return cb.conjunction();
            return cb.equal(root.get(field), value);
        };
    }

    /** Filter on a joined entity's string field (e.g. product.manufacturer.manufacturerName). */
    public static <T> Specification<T> joinStringLike(String joinField, String joinedEntityField, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) return cb.conjunction();
            Join<Object, Object> join = root.join(joinField, JoinType.LEFT);
            return cb.like(cb.lower(join.get(joinedEntityField)), "%" + value.toLowerCase().trim() + "%");
        };
    }

    /** Filter on a joined entity's ID (exact match). */
    public static <T> Specification<T> joinIdEquals(String joinField, Long value) {
        return (root, query, cb) -> {
            if (value == null) return cb.conjunction();
            Join<Object, Object> join = root.join(joinField, JoinType.LEFT);
            return cb.equal(join.get("id"), value);
        };
    }

    public static Boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) return null;
        return Boolean.parseBoolean(value);
    }

    public static Long parseLong(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.parseLong(value); } catch (NumberFormatException e) { return null; }
    }
}
