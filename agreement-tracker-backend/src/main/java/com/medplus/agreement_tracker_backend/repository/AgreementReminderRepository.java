package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementReminder;
import com.medplus.agreement_tracker_backend.enums.ReminderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AgreementReminderRepository extends JpaRepository<AgreementReminder, Long> {

    boolean existsByAgreementVersionIdAndReminderTypeAndSentAtAfter(
            Long agreementVersionId, ReminderType reminderType, LocalDateTime after);

    @Query("SELECT COUNT(r) FROM AgreementReminder r WHERE r.agreementVersion.id = :agreementVersionId AND r.reminderType = :type AND r.sentAt >= :since")
    long countRecentReminders(
            @Param("agreementVersionId") Long agreementVersionId,
            @Param("type") ReminderType type,
            @Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM AgreementReminder r WHERE r.agreementVersion.id = :agreementVersionId")
    void deleteByAgreementVersionId(@Param("agreementVersionId") Long agreementVersionId);
}
