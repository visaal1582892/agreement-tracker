package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.JbpWorkbookRequest;

public interface JbpExcelGeneratorService {

    byte[] generateWorkbook(
            Long agreementVersionId,
            JbpWorkbookRequest request,
            Long currentUserId,
            Integer startMonthOverride);
}
