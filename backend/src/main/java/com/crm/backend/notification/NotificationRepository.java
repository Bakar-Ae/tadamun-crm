package com.crm.backend.notification;

import com.crm.backend.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientUserOrderByCreatedAtDesc(User recipientUser, Pageable pageable);

    long countByRecipientUserAndReadStatusFalse(User recipientUser);
}