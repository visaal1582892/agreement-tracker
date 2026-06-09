package com.medplus.agreement_tracker_backend.controller;

import com.medplus.agreement_tracker_backend.dto.request.SlabDTO;
import com.medplus.agreement_tracker_backend.dto.response.PurchaseSlabResponse;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.AgreementPurchaseSlabService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.medplus.agreement_tracker_backend.security.RightExpressions.AGREEMENT_EDIT;
import static com.medplus.agreement_tracker_backend.security.RightExpressions.AGREEMENT_VIEW;

@RestController
@RequestMapping("/agreements/{agreementId}/slabs")
@RequiredArgsConstructor
public class AgreementPurchaseSlabController {

    private final AgreementPurchaseSlabService purchaseSlabService;

    @GetMapping
    @PreAuthorize(AGREEMENT_VIEW)
    public ResponseEntity<List<PurchaseSlabResponse>> listSlabs(
            @PathVariable Long agreementId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(purchaseSlabService.listSlabs(agreementId, principal.getId()));
    }

    @PostMapping
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<PurchaseSlabResponse> createSlab(
            @PathVariable Long agreementId,
            @Valid @RequestBody SlabDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(purchaseSlabService.createSlab(agreementId, request, principal.getId()));
    }

    @PutMapping("/{slabId}")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<PurchaseSlabResponse> updateSlab(
            @PathVariable Long agreementId,
            @PathVariable Long slabId,
            @Valid @RequestBody SlabDTO request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                purchaseSlabService.updateSlab(agreementId, slabId, request, principal.getId()));
    }

    @DeleteMapping("/{slabId}")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<Void> deleteSlab(
            @PathVariable Long agreementId,
            @PathVariable Long slabId,
            @AuthenticationPrincipal UserPrincipal principal) {
        purchaseSlabService.deleteSlab(agreementId, slabId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
