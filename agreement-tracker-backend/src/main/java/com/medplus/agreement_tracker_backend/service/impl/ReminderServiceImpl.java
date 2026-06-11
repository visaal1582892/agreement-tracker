package com.medplus.agreement_tracker_backend.service.impl;

import com.medplus.agreement_tracker_backend.dto.response.ReminderResponse;
import com.medplus.agreement_tracker_backend.dto.response.UnreadRemindersResponse;
import com.medplus.agreement_tracker_backend.entity.AgreementReminder;
import com.medplus.agreement_tracker_backend.exception.ResourceNotFoundException;
import com.medplus.agreement_tracker_backend.repository.AgreementReminderRepository;
import com.medplus.agreement_tracker_backend.service.ReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {

    private final AgreementReminderRepository reminderRepository;

    @Override
    @Transactional(readOnly = true)
    public UnreadRemindersResponse getUnreadReminders(Long currentUserId) {
        List<ReminderResponse> reminders = reminderRepository
                .findUnreadWithDetails(currentUserId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new UnreadRemindersResponse(reminders.size(), reminders);
    }

    @Override
    @Transactional
    public void markAsRead(Long reminderId, Long currentUserId) {
        AgreementReminder reminder = reminderRepository.findByIdAndSentToUserId(reminderId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Reminder", reminderId));
        reminder.setRead(true);
        reminderRepository.save(reminder);
    }

    private ReminderResponse toResponse(AgreementReminder reminder) {
        var version = reminder.getAgreementVersion();
        var agreement = version.getAgreement();
        return new ReminderResponse(
                reminder.getId(),
                reminder.getReminderType(),
                agreement.getId(),
                agreement.getAgreementName(),
                version.getId(),
                version.getExpiryDate(),
                reminder.getSentAt()
        );
    }
}
