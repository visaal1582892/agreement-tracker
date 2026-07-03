package com.medplus.agreement_tracker_backend.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class JbpExcelRowErrorCollector {

    private final Map<String, Map<Integer, String>> rowErrors = new LinkedHashMap<>();

    public void add(String sheetName, int rowIndex, String message) {
        if (sheetName == null || sheetName.isBlank() || message == null || message.isBlank()) {
            return;
        }
        rowErrors.computeIfAbsent(sheetName, ignored -> new LinkedHashMap<>())
                .merge(rowIndex, message, (existing, incoming) -> existing + "; " + incoming);
    }

    public boolean hasErrors() {
        return !rowErrors.isEmpty();
    }

    public Map<String, Map<Integer, String>> rowErrors() {
        return rowErrors;
    }
}
