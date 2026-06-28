package com.crm.backend.notification;

public record NotificationPreferenceResponse(
        boolean emailEnabled,
        boolean inAppEnabled,
        boolean taskNotificationsEnabled,
        boolean customerNotificationsEnabled,
        boolean leadNotificationsEnabled,
        boolean reportNotificationsEnabled
) {
    public static NotificationPreferenceResponse from(
            NotificationPreference preference
    ) {
        return new NotificationPreferenceResponse(
                preference.isEmailEnabled(),
                preference.isInAppEnabled(),
                preference.isTaskNotificationsEnabled(),
                preference.isCustomerNotificationsEnabled(),
                preference.isLeadNotificationsEnabled(),
                preference.isReportNotificationsEnabled()
        );
    }
}