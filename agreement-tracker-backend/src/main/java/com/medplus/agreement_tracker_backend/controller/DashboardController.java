package com.medplus.agreement_tracker_backend.controller;

import com.medplus.agreement_tracker_backend.dto.response.DashboardStatsResponse;
import com.medplus.agreement_tracker_backend.dto.response.ExpiringAgreementResponse;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.repository.AgreementVersionRepository;
import com.medplus.agreement_tracker_backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.medplus.agreement_tracker_backend.security.RightExpressions.DASHBOARD_VIEW;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AgreementVersionRepository agreementVersionRepository;

    @GetMapping("/stats")
    @PreAuthorize(DASHBOARD_VIEW)
    public ResponseEntity<DashboardStatsResponse> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        LocalDate today = LocalDate.now();

        long active = agreementVersionRepository.countActive(today);
        long expiring30 = agreementVersionRepository.findApprovedExpiringSoon(today, today.plusDays(30)).size();
        long expiring60 = agreementVersionRepository.findApprovedExpiringSoon(today.plusDays(31), today.plusDays(60)).size();
        long expiring90 = agreementVersionRepository.findApprovedExpiringSoon(today.plusDays(61), today.plusDays(90)).size();
        long expired = agreementVersionRepository.findExpiredNotInProgress(today).size();
        long pendingApproval = agreementVersionRepository.countPendingByOwner(principal.getId());

        return ResponseEntity.ok(new DashboardStatsResponse(
                active, expiring30, expiring60, expiring90, expired, pendingApproval, 0L, 0L
        ));
    }

    @GetMapping("/expiring")
    @PreAuthorize(DASHBOARD_VIEW)
    public ResponseEntity<List<ExpiringAgreementResponse>> getExpiring() {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(90);
        List<ExpiringAgreementResponse> items = agreementVersionRepository.findApprovedExpiringWithinDays(today, limit)
                .stream()
                .map(v -> toExpiringResponse(v, today))
                .toList();
        return ResponseEntity.ok(items);
    }

    private ExpiringAgreementResponse toExpiringResponse(AgreementVersion v, LocalDate today) {
        long days = ChronoUnit.DAYS.between(today, v.getExpiryDate());
        String urgency = days < 30 ? "RED" : days < 60 ? "YELLOW" : "BLUE";
        var parent = v.getAgreement();
        var cag = parent.getCompanyAgreementGroup();
        return new ExpiringAgreementResponse(
                v.getId(),
                parent.getId(),
                parent.getAgreementName(),
                cag.getCompany().getCompanyName(),
                parent.getOwner().getFullName(),
                v.getExpiryDate(),
                days,
                urgency
        );
    }
}
