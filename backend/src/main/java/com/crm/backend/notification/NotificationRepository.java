package com.crm.backend.notification;

import com.crm.backend.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO notifications (
                organization_id,
                recipient_user_id,
                title,
                message,
                type,
                deduplication_key,
                read_status,
                created_at
            ) VALUES (
                :organizationId,
                :recipientUserId,
                :title,
                :message,
                :type,
                :deduplicationKey,
                FALSE,
                CURRENT_TIMESTAMP
            )
            ON DUPLICATE KEY UPDATE id = id
            """, nativeQuery = true)
    int insertOnce(
            @Param("organizationId") Long organizationId,
            @Param("recipientUserId") Long recipientUserId,
            @Param("title") String title,
            @Param("message") String message,
            @Param("type") String type,
            @Param("deduplicationKey") String deduplicationKey
    );

    @Query("""
            SELECT n FROM Notification n
            WHERE n.recipientUser = :recipientUser
            AND (
                n.organization.id = :organizationId
                OR n.organization IS NULL
            )
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> findVisibleForRecipient(
            @Param("recipientUser") User recipientUser,
            @Param("organizationId") Long organizationId,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.recipientUser = :recipientUser
            AND n.readStatus = false
            AND (
                n.organization.id = :organizationId
                OR n.organization IS NULL
            )
            """)
    long countUnreadVisibleForRecipient(
            @Param("recipientUser") User recipientUser,
            @Param("organizationId") Long organizationId
    );

    @Query("""
            SELECT n FROM Notification n
            WHERE n.id = :id
            AND n.recipientUser = :recipientUser
            AND (
                n.organization.id = :organizationId
                OR n.organization IS NULL
            )
            """)
    Optional<Notification> findVisibleByIdAndRecipient(
            @Param("id") Long id,
            @Param("recipientUser") User recipientUser,
            @Param("organizationId") Long organizationId
    );
}
