package com.medplus.agreement_tracker_backend.entity;

import com.medplus.agreement_tracker_backend.entity.base.AuditableEntity;
import com.medplus.agreement_tracker_backend.enums.ReminderType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agreement_reminders", indexes = {
        @Index(name = "idx_reminder_agreement_version_id", columnList = "agreement_version_id"),
        @Index(name = "idx_reminder_sent_to", columnList = "sent_to_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementReminder extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_version_id", nullable = false)
    private AgreementVersion agreementVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_type", nullable = false, length = 20)
    private ReminderType reminderType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sent_to_user_id", nullable = false)
    private User sentToUser;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
