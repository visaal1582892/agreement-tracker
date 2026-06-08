package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.request.InitiateTerminateRequest;
import com.medplus.agreement_tracker_backend.dto.request.InitiateTransferRequest;
import com.medplus.agreement_tracker_backend.dto.request.ResolveActionRequest;
import com.medplus.agreement_tracker_backend.dto.response.AgreementActionRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AgreementActionRequestService {

    AgreementActionRequestResponse initiateTransfer(Long agreementId, InitiateTransferRequest request,
                                                    Long currentUserId, boolean isAdmin);

    AgreementActionRequestResponse initiateTerminate(Long agreementId, InitiateTerminateRequest request,
                                                     Long currentUserId);

    AgreementActionRequestResponse resolveRequest(Long requestId, ResolveActionRequest request, Long approverId);

    Page<AgreementActionRequestResponse> getPendingRequests(String search, Pageable pageable);
}
