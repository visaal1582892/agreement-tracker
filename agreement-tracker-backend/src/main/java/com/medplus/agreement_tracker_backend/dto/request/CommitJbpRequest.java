package com.medplus.agreement_tracker_backend.dto.request;

import com.medplus.agreement_tracker_backend.dto.response.JbpStagedWorkbookDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CommitJbpRequest(
        @NotNull @Valid JbpStagedWorkbookDto stagedWorkbook
) {
}
