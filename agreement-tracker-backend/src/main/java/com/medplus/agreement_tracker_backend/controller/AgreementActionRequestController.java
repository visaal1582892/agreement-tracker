package com.medplus.agreement_tracker_backend.controller;

import com.medplus.agreement_tracker_backend.dto.request.InitiateTerminateRequest;
import com.medplus.agreement_tracker_backend.dto.request.InitiateTransferRequest;
import com.medplus.agreement_tracker_backend.dto.request.ResolveActionRequest;
import com.medplus.agreement_tracker_backend.dto.response.AgreementActionRequestResponse;
import com.medplus.agreement_tracker_backend.dto.response.AgreementVersionResponse;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.enums.RightCode;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.AgreementActionRequestService;
import com.medplus.agreement_tracker_backend.service.AgreementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.medplus.agreement_tracker_backend.security.RightExpressions.*;

@RestController
@RequiredArgsConstructor
public class AgreementActionRequestController {

    private final AgreementActionRequestService actionRequestService;
    private final AgreementService agreementService;

    @PostMapping("/agreement-versions/{agreementVersionId}/requests/transfer")
    @PreAuthorize("hasAnyAuthority('ADMIN_USERS', 'AGREEMENT_EDIT')")
    public ResponseEntity<?> initiateTransfer(
            @PathVariable Long agreementVersionId,
            @Valid @RequestBody InitiateTransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.hasRight(RightCode.ADMIN_USERS.name());
        if (isAdmin) {
            AgreementVersionResponse result = agreementService.transferOwnership(
                    agreementVersionId, request.newOwnerId(), principal.getId(), true, request.comments());
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(actionRequestService.initiateTransfer(agreementVersionId, request, principal.getId(), false));
    }

    @PostMapping("/agreement-versions/{agreementVersionId}/requests/terminate")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<AgreementActionRequestResponse> initiateTerminate(
            @PathVariable Long agreementVersionId,
            @Valid @RequestBody InitiateTerminateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(actionRequestService.initiateTerminate(agreementVersionId, request, principal.getId()));
    }

    @PutMapping("/agreement-versions/requests/{requestId}/resolve")
    @PreAuthorize(AGREEMENT_APPROVE)
    public ResponseEntity<AgreementActionRequestResponse> resolveRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody ResolveActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                actionRequestService.resolveRequest(requestId, request, principal.getId()));
    }

    @GetMapping("/agreement-versions/requests/pending")
    @PreAuthorize(AGREEMENT_APPROVE)
    public ResponseEntity<PagedResponse<AgreementActionRequestResponse>> getPendingRequests(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(actionRequestService.getPendingRequests(search, pageable)));
    }
}
