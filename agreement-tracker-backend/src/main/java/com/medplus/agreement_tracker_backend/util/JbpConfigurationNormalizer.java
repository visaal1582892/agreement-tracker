package com.medplus.agreement_tracker_backend.util;

import com.medplus.agreement_tracker_backend.dto.request.JbpConfigurationBlockDto;

import java.util.List;
import java.util.Map;

public final class JbpConfigurationNormalizer {

    private JbpConfigurationNormalizer() {
    }

    public static JbpConfigurationBlockDto normalize(Map<String, Object> raw) {
        String configId = stringValue(raw.get("configId"));
        @SuppressWarnings("unchecked")
        List<Long> parentPeriodIds = raw.get("parentPeriodIds") instanceof List<?> list
                ? list.stream().map(value -> ((Number) value).longValue()).toList()
                : List.of();
        Integer slabCount = raw.get("slabCount") instanceof Number number
                ? number.intValue()
                : 1;

        return new JbpConfigurationBlockDto(configId, parentPeriodIds, slabCount);
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
