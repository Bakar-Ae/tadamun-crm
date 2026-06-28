package com.crm.backend.notification;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPreferenceRequest(
        @NotNull Boolean emailEnabled,
        @NotNull Boolean inAppEnabled,
        @NotNull Boolean taskNotificationsEnabled,
        @NotNull Boolean customerNotificationsEnabled,
        @NotNull Boolean leadNotificationsEnabled,
        @NotNull Boolean reportNotificationsEnabled
) {
}