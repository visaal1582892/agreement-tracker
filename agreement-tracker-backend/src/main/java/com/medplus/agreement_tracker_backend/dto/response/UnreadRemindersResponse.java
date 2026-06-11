package com.medplus.agreement_tracker_backend.dto.response;

import java.util.List;

public record UnreadRemindersResponse(
        long count,
        List<ReminderResponse> reminders
) {}
