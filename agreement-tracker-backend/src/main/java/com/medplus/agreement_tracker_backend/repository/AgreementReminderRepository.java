package com.medplus.agreement_tracker_backend.repository;

import com.medplus.agreement_tracker_backend.entity.AgreementReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgreementReminderRepository extends JpaRepository<AgreementReminder, Long> {

    boolean existsByAgreementVersionIdAndReminderTypeAndSentAtGreaterThanEqual(
            Long agreementVersionId, String reminderType, LocalDateTime sentAt);

    long countBySentToUserIdAndIsReadFalse(Long sentToUserId);

    @Query("""
            SELECT r FROM AgreementReminder r
            JOIN FETCH r.agreementVersion av
            JOIN FETCH av.agreement ag
            WHERE r.sentToUser.id = :userId AND r.isRead = false
            ORDER BY r.sentAt DESC
            """)
    List<AgreementReminder> findUnreadWithDetails(@Param("userId") Long userId);

    Optional<AgreementReminder> findByIdAndSentToUserId(Long id, Long sentToUserId);

    @Modifying
    @Query("DELETE FROM AgreementReminder r WHERE r.agreementVersion.id = :agreementVersionId")
    void deleteByAgreementVersionId(@Param("agreementVersionId") Long agreementVersionId);
}
