package com.medplus.agreement_tracker_backend.controller;

import com.medplus.agreement_tracker_backend.dto.request.CommitJbpRequest;
import com.medplus.agreement_tracker_backend.dto.request.JbpWorkbookRequest;
import com.medplus.agreement_tracker_backend.dto.response.JbpStructureHydrationResponse;
import com.medplus.agreement_tracker_backend.dto.response.JbpStagedWorkbookDto;
import com.medplus.agreement_tracker_backend.dto.response.TimePeriodSummaryResponse;
import com.medplus.agreement_tracker_backend.enums.PayoutFrequency;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import com.medplus.agreement_tracker_backend.service.JbpCommercialService;
import com.medplus.agreement_tracker_backend.service.JbpExcelGeneratorService;
import com.medplus.agreement_tracker_backend.service.JbpExcelParserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.medplus.agreement_tracker_backend.security.RightExpressions.AGREEMENT_EDIT;
import static com.medplus.agreement_tracker_backend.security.RightExpressions.AGREEMENT_VIEW;

@RestController
@RequestMapping("/agreement-versions/{agreementVersionId}")
@RequiredArgsConstructor
public class JbpExcelController {

    private final JbpExcelGeneratorService jbpExcelGeneratorService;
    private final JbpExcelParserService jbpExcelParserService;
    private final JbpCommercialService jbpCommercialService;

    @PostMapping("/jbp-template")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<byte[]> downloadJbpTemplate(
            @PathVariable Long agreementVersionId,
            @Valid @RequestBody JbpWorkbookRequest request,
            @RequestParam(required = false) Integer startMonth,
            @AuthenticationPrincipal UserPrincipal principal) {
        byte[] file = jbpExcelGeneratorService.generateWorkbook(
                agreementVersionId, request, principal.getId(), startMonth);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=jbp-workbook-" + agreementVersionId + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @PostMapping(value = "/jbp-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<JbpStagedWorkbookDto> uploadJbpWorkbook(
            @PathVariable Long agreementVersionId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                jbpExcelParserService.parseUpload(agreementVersionId, file, principal.getId()));
    }

    @PutMapping("/commit-jbp")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<Void> commitJbpStructure(
            @PathVariable Long agreementVersionId,
            @Valid @RequestBody CommitJbpRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        jbpCommercialService.commitJbpStructure(agreementVersionId, request, principal.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/jbp-structure")
    @PreAuthorize("hasAnyAuthority('AGREEMENT_EDIT', 'AGREEMENT_VIEW', 'AGREEMENT_VIEW_ALL')")
    public ResponseEntity<JbpStructureHydrationResponse> getJbpStructure(
            @PathVariable Long agreementVersionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                jbpCommercialService.getJbpStructure(agreementVersionId, principal.getId()));
    }

    @GetMapping("/jbp-time-periods")
    @PreAuthorize(AGREEMENT_EDIT)
    public ResponseEntity<List<TimePeriodSummaryResponse>> listJbpTimePeriods(
            @PathVariable Long agreementVersionId,
            @RequestParam PayoutFrequency frequency,
            @RequestParam(required = false) Integer financialYearStartMonth,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(jbpCommercialService.listAvailablePeriods(
                agreementVersionId, frequency, principal.getId(), financialYearStartMonth));
    }
}
