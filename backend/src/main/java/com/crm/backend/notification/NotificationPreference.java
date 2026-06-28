package com.crm.backend.notification;

import com.crm.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    @Column(name = "task_notifications_enabled", nullable = false)
    private boolean taskNotificationsEnabled = true;

    @Column(name = "customer_notifications_enabled", nullable = false)
    private boolean customerNotificationsEnabled = true;

    @Column(name = "lead_notifications_enabled", nullable = false)
    private boolean leadNotificationsEnabled = true;

    @Column(name = "report_notifications_enabled", nullable = false)
    private boolean reportNotificationsEnabled = true;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}