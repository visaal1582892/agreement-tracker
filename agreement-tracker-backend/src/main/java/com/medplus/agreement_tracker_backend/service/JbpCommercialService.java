package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.CommitJbpRequest;
import com.medplus.agreement_tracker_backend.dto.response.JbpStructureHydrationResponse;
import com.medplus.agreement_tracker_backend.dto.response.TimePeriodSummaryResponse;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;

import java.util.List;

public interface JbpCommercialService {

    void commitJbpStructure(Long agreementVersionId, CommitJbpRequest request, Long currentUserId);

    JbpStructureHydrationResponse getJbpStructure(Long agreementVersionId, Long currentUserId);

    List<TimePeriodSummaryResponse> listAvailablePeriods(
            Long agreementVersionId,
            PayoutFrequency frequency,
            Long currentUserId,
            Integer financialYearStartMonth);
}
