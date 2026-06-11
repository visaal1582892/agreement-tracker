package com.medplus.agreement_tracker_backend.scheduler;

import com.medplus.agreement_tracker_backend.entity.AgreementReminder;
import com.medplus.agreement_tracker_backend.entity.AgreementVersion;
import com.medplus.agreement_tracker_backend.entity.User;
import com.medplus.agreement_tracker_backend.repository.AgreementReminderRepository;
import com.medplus.agreement_tracker_backend.repository.AgreementVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgreementLifecycleScheduler {

    private final AgreementVersionRepository agreementVersionRepository;
    private final AgreementReminderRepository reminderRepository;

    @Value("${agreement.in-progress.max-days:30}")
    private int inProgressMaxDays;

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void runDailyLifecycleJobs() {
        autoRevertInProgress();
        generateReminders();
    }

    private void autoRevertInProgress() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(inProgressMaxDays);
        var staleVersions = agreementVersionRepository.findStaleInProgressCurrentVersions(cutoff);
        for (AgreementVersion version : staleVersions) {
            version.setInProgressFlag(false);
            version.setInProgressSince(null);
            agreementVersionRepository.save(version);
            log.info("Auto-reverted in-progress flag for agreement version {}", version.getId());
        }
    }

    private void generateReminders() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();

        for (AgreementVersion version : agreementVersionRepository.findCurrentApprovedVersions()) {
            if (version.isInProgressFlag() || version.getExpiryDate() == null) {
                continue;
            }

            long daysToExpiry = ChronoUnit.DAYS.between(today, version.getExpiryDate());
            String reminderType = resolveReminderType(daysToExpiry);
            if (reminderType == null) {
                continue;
            }

            if (reminderRepository.existsByAgreementVersionIdAndReminderTypeAndSentAtGreaterThanEqual(
                    version.getId(), reminderType, startOfDay)) {
                continue;
            }

            User owner = version.getAgreement().getOwner();
            AgreementReminder reminder = AgreementReminder.builder()
                    .agreementVersion(version)
                    .reminderType(reminderType)
                    .sentToUser(owner)
                    .sentAt(LocalDateTime.now())
                    .isRead(false)
                    .build();
            reminderRepository.save(reminder);
            log.info("Created reminder {} for agreement version {}", reminderType, version.getId());
        }
    }

    private String resolveReminderType(long daysToExpiry) {
        if (daysToExpiry <= 0) {
            return "EXPIRED_DAILY";
        }
        if (daysToExpiry == 90 || daysToExpiry == 60 || daysToExpiry == 45 || daysToExpiry == 30) {
            return "D_" + daysToExpiry;
        }
        if (daysToExpiry < 30 && daysToExpiry % 7 == 0) {
            return "D_" + daysToExpiry;
        }
        return null;
    }
}
