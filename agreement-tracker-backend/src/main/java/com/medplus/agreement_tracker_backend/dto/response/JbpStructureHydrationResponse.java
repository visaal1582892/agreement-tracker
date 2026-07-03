package com.medplus.agreement_tracker_backend.dto.response;

import com.medplus.agreement_tracker_backend.dto.request.JbpConfigurationBlockDto;

import java.util.List;

public record JbpStructureHydrationResponse(
        List<String> frequencies,
        List<JbpConfigurationBlockDto> configurations,
        JbpStagedWorkbookDto stagedWorkbook
) {
}
