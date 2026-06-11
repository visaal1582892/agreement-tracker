package com.medplus.agreement_tracker_backend.service;

import com.medplus.agreement_tracker_backend.dto.response.UnreadRemindersResponse;

public interface ReminderService {

    UnreadRemindersResponse getUnreadReminders(Long currentUserId);

    void markAsRead(Long reminderId, Long currentUserId);
}
