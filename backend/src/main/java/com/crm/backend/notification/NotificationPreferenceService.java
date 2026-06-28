package com.crm.backend.notification;

import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public NotificationPreferenceService(
            NotificationPreferenceRepository preferenceRepository,
            UserRepository userRepository
    ) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public NotificationPreferenceResponse getPreferences(Long userId) {
        return NotificationPreferenceResponse.from(
                getOrCreatePreferences(userId)
        );
    }

    @Transactional
    public NotificationPreferenceResponse updatePreferences(
            Long userId,
            UpdateNotificationPreferenceRequest request
    ) {
        NotificationPreference preference = getOrCreatePreferences(userId);

        preference.setEmailEnabled(request.emailEnabled());
        preference.setInAppEnabled(request.inAppEnabled());
        preference.setTaskNotificationsEnabled(
                request.taskNotificationsEnabled()
        );
        preference.setCustomerNotificationsEnabled(
                request.customerNotificationsEnabled()
        );
        preference.setLeadNotificationsEnabled(
                request.leadNotificationsEnabled()
        );
        preference.setReportNotificationsEnabled(
                request.reportNotificationsEnabled()
        );

        return NotificationPreferenceResponse.from(preference);
    }

    @Transactional
    public boolean allowsInAppNotification(
            Long userId,
            NotificationType type
    ) {
        if (isMandatory(type)) {
            return true;
        }

        NotificationPreference preference = getOrCreatePreferences(userId);

        return preference.isInAppEnabled()
                && isCategoryEnabled(preference, type);
    }

    @Transactional
    public boolean allowsEmailNotification(
            Long userId,
            NotificationType type
    ) {
        if (isMandatory(type)) {
            return true;
        }

        NotificationPreference preference = getOrCreatePreferences(userId);

        return preference.isEmailEnabled()
                && isCategoryEnabled(preference, type);
    }

    private NotificationPreference getOrCreatePreferences(Long userId) {
        return preferenceRepository.findById(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
    }

    private NotificationPreference createDefaultPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        NotificationPreference preference = new NotificationPreference();
        preference.setUser(user);

        return preferenceRepository.save(preference);
    }

    private boolean isMandatory(NotificationType type) {
        return type == NotificationType.SYSTEM
                || type == NotificationType.PASSWORD_CHANGED;
    }

    private boolean isCategoryEnabled(
            NotificationPreference preference,
            NotificationType type
    ) {
        return switch (type) {
            case TASK_ASSIGNED ->
                    preference.isTaskNotificationsEnabled();
            case CUSTOMER_UPDATED ->
                    preference.isCustomerNotificationsEnabled();
            case LEAD_UPDATED ->
                    preference.isLeadNotificationsEnabled();
            case REPORT_READY ->
                    preference.isReportNotificationsEnabled();
            case SYSTEM, PASSWORD_CHANGED -> true;
        };
    }
}