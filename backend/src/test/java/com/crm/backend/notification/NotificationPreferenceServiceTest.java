package com.crm.backend.notification;

import com.crm.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificationPreferenceServiceTest {

    private NotificationPreferenceRepository preferenceRepository;
    private UserRepository userRepository;
    private NotificationPreferenceService preferenceService;

    @BeforeEach
    void setUp() {
        preferenceRepository = mock(NotificationPreferenceRepository.class);
        userRepository = mock(UserRepository.class);

        preferenceService = new NotificationPreferenceService(
                preferenceRepository,
                userRepository
        );
    }

    @Test
    void disabledInAppNotificationsShouldRejectOptionalTaskNotification() {
        NotificationPreference preference = new NotificationPreference();
        preference.setInAppEnabled(false);

        when(preferenceRepository.findById(1L))
                .thenReturn(Optional.of(preference));

        boolean allowed = preferenceService.allowsInAppNotification(
                1L,
                NotificationType.TASK_ASSIGNED
        );

        assertFalse(allowed);
    }

    @Test
    void disabledCategoryShouldRejectOptionalReportNotification() {
        NotificationPreference preference = new NotificationPreference();
        preference.setInAppEnabled(true);
        preference.setReportNotificationsEnabled(false);

        when(preferenceRepository.findById(1L))
                .thenReturn(Optional.of(preference));

        boolean allowed = preferenceService.allowsInAppNotification(
                1L,
                NotificationType.REPORT_READY
        );

        assertFalse(allowed);
    }

    @Test
    void passwordChangedNotificationShouldAlwaysBeAllowed() {
        boolean allowed = preferenceService.allowsInAppNotification(
                1L,
                NotificationType.PASSWORD_CHANGED
        );

        assertTrue(allowed);
        verifyNoInteractions(preferenceRepository, userRepository);
    }
}