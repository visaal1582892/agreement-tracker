package com.medplus.agreement_tracker_backend.controller;

import com.medplus.agreement_tracker_backend.dto.request.CommitCutoffsRequest;
import com.medplus.agreement_tracker_backend.dto.response.SlabPeriodCutoffMatrixResponse;
import com.medplus.agreement_tracker_backend.dto.response.StagedCutoffMatrixResponse;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.CommercialContactsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static com.medplus.agreement_tracker_backend.security.RightExpressions.AGREEMENT_EDIT;
import static com.medplus.agreement_tracker_backend.security.RightExpressions.AGREEMENT_VIEW;

@RestController
@RequestMapping("/agreement-versions/{agreementVersionId}")
@RequiredArgsConstructor
public class CommercialContactsExcelController {

    private final CommercialContactsService commercialContactsService;

    @GetMapping("/contacts-template")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<byte[]> downloadContactsTemplate(
            @PathVariable Long agreementVersionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        byte[] file = commercialContactsService.generateCutoffTemplate(agreementVersionId, principal.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=contacts-cutoff-template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @PostMapping(value = "/contacts-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<StagedCutoffMatrixResponse> uploadContactsCutoffs(
            @PathVariable Long agreementVersionId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                commercialContactsService.parseCutoffUpload(agreementVersionId, file, principal.getId()));
    }

    @PutMapping("/commit-cutoffs")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<Void> commitContactsCutoffs(
            @PathVariable Long agreementVersionId,
            @Valid @RequestBody CommitCutoffsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        commercialContactsService.commitCutoffs(agreementVersionId, request, principal.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/contacts-cutoffs")
    @PreAuthorize(AGREEMENT_VIEW)
    public ResponseEntity<SlabPeriodCutoffMatrixResponse> listContactsCutoffs(
            @PathVariable Long agreementVersionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(commercialContactsService.listCutoffMatrix(agreementVersionId, principal.getId()));
    }

    @DeleteMapping("/commercial-structure-data")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<Void> purgeCommercialStructureData(
            @PathVariable Long agreementVersionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        commercialContactsService.purgeCommercialStructureData(agreementVersionId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
