package com.crm.backend.user;

import com.crm.backend.role.RoleName;
import com.crm.backend.dashboard.TeamMemberWorkloadProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query(value = """
        SELECT COUNT(*)
        FROM organization_memberships membership
        JOIN users u ON u.id = membership.user_id
        LEFT JOIN teams team ON team.id = u.team_id
        WHERE membership.organization_id = :organizationId
          AND membership.status = 'ACTIVE'
          AND u.status = 'ACTIVE'
          AND (
              :allAccess = TRUE
              OR u.id = :currentUserId
              OR (
                  :teamAccess = TRUE
                  AND :currentTeamId IS NOT NULL
                  AND team.id = :currentTeamId
              )
          )
        """, nativeQuery = true)
    long countAccessibleUsersInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId
    );

    @Query(value = """
        SELECT
            u.id AS userId,
            u.full_name AS fullName,
            COALESCE((
                SELECT COUNT(*) FROM customers c
                WHERE c.organization_id = :organizationId
                  AND c.owner_user_id = u.id
                  AND c.status = 'ACTIVE'
            ), 0) AS activeCustomers,
            COALESCE((
                SELECT COUNT(*) FROM leads l
                WHERE l.organization_id = :organizationId
                  AND l.assigned_to_user_id = u.id
                  AND l.status IN ('NEW', 'CONTACTED', 'QUALIFIED')
            ), 0) AS activeLeads,
            COALESCE((
                SELECT COUNT(*) FROM tasks t
                WHERE t.organization_id = :organizationId
                  AND t.assigned_to_user_id = u.id
                  AND t.status IN ('OPEN', 'IN_PROGRESS')
            ), 0) AS openTasks,
            COALESCE((
                SELECT COUNT(*) FROM tasks t
                WHERE t.organization_id = :organizationId
                  AND t.assigned_to_user_id = u.id
                  AND t.status = 'COMPLETED'
            ), 0) AS completedTasks,
            COALESCE((
                SELECT COUNT(*) FROM audit_logs a
                WHERE a.organization_id = :organizationId
                  AND a.scope = 'ORGANIZATION'
                  AND a.actor_user_id = u.id
                  AND a.created_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY)
            ), 0) AS recentActivities
        FROM organization_memberships membership
        JOIN users u ON u.id = membership.user_id
        WHERE membership.organization_id = :organizationId
          AND membership.status = 'ACTIVE'
          AND u.status = 'ACTIVE'
          AND (
              :allAccess = TRUE
              OR u.id = :currentUserId
              OR (
                  :teamAccess = TRUE
                  AND :currentTeamId IS NOT NULL
                  AND u.team_id = :currentTeamId
              )
          )
        ORDER BY openTasks DESC, recentActivities DESC, u.full_name ASC
        """, nativeQuery = true)
    List<TeamMemberWorkloadProjection> findDashboardMemberWorkloadsInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId,
            Pageable pageable
    );

    @Query("""
        SELECT u FROM User u
        WHERE (:keyword IS NULL OR :keyword = ''
            OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:status IS NULL OR u.status = :status)
        AND (:role IS NULL OR u.role.name = :role)
        """)
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("status") UserStatus status,
            @Param("role") RoleName role,
            Pageable pageable
    );
}
