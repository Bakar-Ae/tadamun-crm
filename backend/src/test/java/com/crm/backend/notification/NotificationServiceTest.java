package com.crm.backend.notification;

import com.crm.backend.organization.Organization;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private UserRepository userRepository;
    private NotificationPreferenceService preferenceService;
    private CurrentOrganizationProvider currentOrganizationProvider;
    private NotificationService notificationService;
    private Organization organization;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        userRepository = mock(UserRepository.class);
        preferenceService = mock(NotificationPreferenceService.class);
        currentOrganizationProvider =
                mock(CurrentOrganizationProvider.class);

        organization = new Organization();
        organization.setId(7L);

        when(currentOrganizationProvider.getOrganizationId())
                .thenReturn(7L);
        when(currentOrganizationProvider.getOptionalOrganizationReference())
                .thenReturn(Optional.of(organization));

        notificationService = new NotificationService(
                notificationRepository,
                userRepository,
                preferenceService,
                currentOrganizationProvider
        );
    }

    @Test
    void createNotificationShouldAssignCurrentOrganization() {
        User recipient = new User();
        recipient.setId(3L);

        when(preferenceService.allowsInAppNotification(
                3L,
                NotificationType.TASK_ASSIGNED
        )).thenReturn(true);
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(recipient));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification notification = notificationService
                .createNotification(
                        3L,
                        "Task assigned",
                        "A task was assigned to you.",
                        NotificationType.TASK_ASSIGNED
                )
                .orElseThrow();

        assertSame(organization, notification.getOrganization());
    }

    @Test
    void markAsReadShouldHideNotificationOutsideOrganization() {
        User recipient = new User();
        recipient.setId(3L);

        when(notificationRepository.findVisibleByIdAndRecipient(
                99L,
                recipient,
                7L
        )).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> notificationService.markAsRead(99L, recipient)
        );

        verify(notificationRepository).findVisibleByIdAndRecipient(
                99L,
                recipient,
                7L
        );
    }

    @Test
    void listShouldUseCurrentOrganization() {
        User recipient = new User();
        recipient.setId(3L);
        PageRequest pageable = PageRequest.of(0, 20);

        notificationService.getNotifications(recipient, pageable);

        verify(notificationRepository).findVisibleForRecipient(
                recipient,
                7L,
                pageable
        );
    }
}
