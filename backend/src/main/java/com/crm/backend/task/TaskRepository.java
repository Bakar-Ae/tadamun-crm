package com.crm.backend.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<CrmTask, Long> {
    default long countByStatus(TaskStatus status) {
        return 0;
    }

    @Query("""
            SELECT t FROM CrmTask t
            WHERE (:keyword IS NULL OR :keyword = ''
                OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:status IS NULL OR t.status = :status)
            AND (:priority IS NULL OR t.priority = :priority)
            AND (:assignedToUserId IS NULL OR t.assignedToUser.id = :assignedToUserId)
            AND (:customerId IS NULL OR t.customer.id = :customerId)
            AND (:leadId IS NULL OR t.lead.id = :leadId)
            """)
    Page<CrmTask> searchTasks(
            @Param("keyword") String keyword,
            @Param("status") TaskStatus status,
            @Param("priority") TaskPriority priority,
            @Param("assignedToUserId") Long assignedToUserId,
            @Param("customerId") Long customerId,
            @Param("leadId") Long leadId,
            Pageable pageable
    );
}