package com.crm.backend.notification;

import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationPreferenceService preferenceService;
    private final CurrentOrganizationProvider currentOrganizationProvider;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            NotificationPreferenceService preferenceService,
            CurrentOrganizationProvider currentOrganizationProvider
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.preferenceService = preferenceService;
        this.currentOrganizationProvider = currentOrganizationProvider;
    }

    @Transactional
    public Optional<Notification> createNotification(
            Long recipientUserId,
            String title,
            String message,
            NotificationType type
    ) {
        boolean allowed = preferenceService.allowsInAppNotification(
                recipientUserId,
                type
        );

        if (!allowed) {
            return Optional.empty();
        }

        User recipientUser = userRepository.findById(recipientUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Recipient user not found")
                );

        Notification notification = new Notification();
        currentOrganizationProvider
                .getOptionalOrganizationReference()
                .ifPresent(notification::setOrganization);
        notification.setRecipientUser(recipientUser);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);

        return Optional.of(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(User recipientUser, Pageable pageable) {
        return notificationRepository.findVisibleForRecipient(
                recipientUser,
                currentOrganizationProvider.getOrganizationId(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public long countUnread(User recipientUser) {
        return notificationRepository.countUnreadVisibleForRecipient(
                recipientUser,
                currentOrganizationProvider.getOrganizationId()
        );
    }

    @Transactional
    public void markAsRead(Long notificationId, User recipientUser) {
        Notification notification = notificationRepository
                .findVisibleByIdAndRecipient(
                        notificationId,
                        recipientUser,
                        currentOrganizationProvider.getOrganizationId()
                )
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.isReadStatus()) {
            notification.setReadStatus(true);
            notification.setReadAt(LocalDateTime.now());
        }
    }
}
