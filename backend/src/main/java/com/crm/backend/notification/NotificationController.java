package com.crm.backend.notification;

import com.crm.backend.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable
    ) {
        Page<NotificationResponse> notifications = notificationService
                .getNotifications(userDetails.getUser(), pageable)
                .map(NotificationResponse::from);

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long unreadCount = notificationService.countUnread(userDetails.getUser());

        return ResponseEntity.ok(new UnreadNotificationCountResponse(unreadCount));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id
    ) {
        notificationService.markAsRead(id, userDetails.getUser());
        return ResponseEntity.noContent().build();
    }
}