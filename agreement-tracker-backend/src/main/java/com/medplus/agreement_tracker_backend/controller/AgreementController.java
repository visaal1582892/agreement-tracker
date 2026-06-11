package com.medplus.agreement_tracker_backend.controller;

import com.medplus.agreement_tracker_backend.dto.request.BulkTransferRequest;
import com.medplus.agreement_tracker_backend.dto.request.CreateAgreementRequest;
import com.medplus.agreement_tracker_backend.dto.response.AgreementResponse;
import com.medplus.agreement_tracker_backend.dto.response.AgreementVersionResponse;
import com.medplus.agreement_tracker_backend.dto.response.BulkAgreementCreateResponse;
import com.medplus.agreement_tracker_backend.dto.response.RenewAgreementResponse;
import com.medplus.agreement_tracker_backend.enums.RightCode;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.AgreementService;
import com.medplus.agreement_tracker_backend.validation.DraftValidation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.medplus.agreement_tracker_backend.security.RightExpressions.*;

@RestController
@RequestMapping("/agreements")
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;

    @PostMapping
    @PreAuthorize(AGREEMENT_CREATE)
    public ResponseEntity<BulkAgreementCreateResponse> createDraft(
            @Validated(DraftValidation.class) @RequestBody CreateAgreementRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agreementService.createDraft(request, principal.getId()));
    }

    @GetMapping
    @PreAuthorize(AGREEMENT_VIEW)
    public ResponseEntity<Page<AgreementResponse>> getAllAgreements(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(defaultValue = "MY") String scope,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long companyAgreementGroupId,
            @RequestParam(required = false) String agreementGroupName,
            @RequestParam(required = false) String companyAgreementGroupName,
            @RequestParam(required = false) String agreementName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) Long incomeTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDateTo,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean canViewAll = principal.hasRight(RightCode.AGREEMENT_VIEW_ALL.name());
        boolean canViewMy = principal.hasRight(RightCode.AGREEMENT_VIEW.name());

        String effectiveScope = scope.toUpperCase();
        if ("ALL".equals(effectiveScope) && !canViewAll) {
            throw new AccessDeniedException("Missing AGREEMENT_VIEW_ALL right");
        }
        if ("MY".equals(effectiveScope) && !canViewMy) {
            throw new AccessDeniedException("Missing AGREEMENT_VIEW right");
        }
        if (!canViewMy && canViewAll) {
            effectiveScope = "ALL";
        } else if (canViewMy && !canViewAll) {
            effectiveScope = "MY";
        }

        String effectiveGroupName = agreementGroupName != null ? agreementGroupName : companyAgreementGroupName;

        return ResponseEntity.ok(agreementService.getAllAgreements(
                pageable, principal.getId(), effectiveScope, canViewAll,
                companyId, companyAgreementGroupId, effectiveGroupName,
                agreementName, status, ownerName, vendorId, incomeTypeId,
                startDateFrom, startDateTo, endDateFrom, endDateTo));
    }

    @GetMapping("/{id}")
    @PreAuthorize(AGREEMENT_VIEW)
    public ResponseEntity<AgreementResponse> getAgreement(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agreementService.getAgreementById(id, principal.getId()));
    }

    @GetMapping("/{id}/versions")
    @PreAuthorize(AGREEMENT_VIEW)
    public ResponseEntity<List<AgreementVersionResponse>> getVersions(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agreementService.getVersionsByAgreementId(id, principal.getId()));
    }

    @PostMapping("/{id}/new-version")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<AgreementVersionResponse> createNewVersion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agreementService.createNewVersion(id, principal.getId()));
    }

    @PatchMapping("/{id}/in-progress")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<AgreementVersionResponse> toggleInProgress(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agreementService.toggleAgreementInProgress(id, principal.getId()));
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<RenewAgreementResponse> renewAgreement(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agreementService.renewAgreement(id, principal.getId()));
    }

    @PutMapping("/bulk-transfer")
    @PreAuthorize(ADMIN_USERS)
    public ResponseEntity<Void> bulkTransfer(
            @Valid @RequestBody BulkTransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        agreementService.bulkTransferOwnership(
                request.fromUserId(), request.toUserId(),
                request.specificAgreementIds(), principal.getId());
        return ResponseEntity.ok().build();
    }
}
