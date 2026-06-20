package com.medplus.agreement_tracker_backend.controller;

import com.medplus.agreement_tracker_backend.dto.request.ApprovalActionRequest;
import com.medplus.agreement_tracker_backend.dto.request.EditAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.SubmitForApprovalRequest;
import com.medplus.agreement_tracker_backend.dto.request.TerminateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.request.TransferOwnershipRequest;
import com.medplus.agreement_tracker_backend.dto.request.UpdateDraftRequest;
import com.medplus.agreement_tracker_backend.dto.response.AgreementVersionResponse;
import com.medplus.agreement_tracker_backend.dto.response.ApprovalTimelineResponse;
import com.medplus.agreement_tracker_backend.dto.response.PagedResponse;
import com.medplus.agreement_tracker_backend.enums.RightCode;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.AgreementService;
import com.medplus.agreement_tracker_backend.validation.DraftValidation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.medplus.agreement_tracker_backend.security.RightExpressions.*;

@RestController
@RequestMapping("/agreement-versions")
@RequiredArgsConstructor
public class AgreementVersionController {

    private final AgreementService agreementService;

    @GetMapping("/{id}")
    @PreAuthorize(AGREEMENT_VIEW)
    public ResponseEntity<AgreementVersionResponse> getAgreementVersion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agreementService.getAgreementVersionById(id, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<AgreementVersionResponse> updateDraft(
            @PathVariable Long id,
            @Validated(DraftValidation.class) @RequestBody UpdateDraftRequest request,
            @RequestParam(defaultValue = "false") boolean validateStep1,
            @RequestParam(defaultValue = "false") boolean validateStep2,
            @RequestParam(defaultValue = "false") boolean validateCommercialStructure,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agreementService.updateDraft(
                id, request, principal.getId(), validateStep1, validateStep2, validateCommercialStructure));
    }

    @PostMapping("/{id}/versions")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<AgreementVersionResponse> createVersionedEdit(
            @PathVariable Long id,
            @Valid @RequestBody EditAgreementRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agreementService.createVersionedEdit(id, request, principal.getId()));
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize(AGREEMENT_CREATE)
    public ResponseEntity<AgreementVersionResponse> cloneAgreement(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agreementService.cloneAgreement(id, principal.getId()));
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<AgreementVersionResponse> submitForApproval(
            @PathVariable Long id,
            @RequestBody(required = false) SubmitForApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String comments = request != null ? request.comments() : null;
        return ResponseEntity.ok(agreementService.submitForApproval(id, comments, principal.getId()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(AGREEMENT_APPROVE)
    public ResponseEntity<AgreementVersionResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String remarks = request != null ? request.remarks() : null;
        return ResponseEntity.ok(agreementService.approve(id, remarks, principal.getId()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize(AGREEMENT_APPROVE)
    public ResponseEntity<AgreementVersionResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalActionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agreementService.reject(id, request.remarks(), principal.getId()));
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<AgreementVersionResponse> terminate(
            @PathVariable Long id,
            @Valid @RequestBody TerminateAgreementRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agreementService.terminate(id, request, principal.getId()));
    }

    @GetMapping("/pending-approvals")
    @PreAuthorize(AGREEMENT_APPROVE)
    public ResponseEntity<PagedResponse<AgreementVersionResponse>> getPendingApprovals(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(agreementService.getPendingApprovals(search, pageable)));
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize(AGREEMENT_VIEW)
    public ResponseEntity<List<ApprovalTimelineResponse>> getTimeline(@PathVariable Long id) {
        return ResponseEntity.ok(agreementService.getApprovalTimeline(id));
    }

    @PutMapping("/{id}/transfer")
    @PreAuthorize("hasAnyAuthority('ADMIN_USERS', 'AGREEMENT_EDIT')")
    public ResponseEntity<AgreementVersionResponse> transferOwnership(
            @PathVariable Long id,
            @Valid @RequestBody TransferOwnershipRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean isAdmin = principal.hasRight(RightCode.ADMIN_USERS.name());
        return ResponseEntity.ok(agreementService.transferOwnership(
                id, request.newOwnerUserId(), principal.getId(), isAdmin, request.comments()));
    }
}
