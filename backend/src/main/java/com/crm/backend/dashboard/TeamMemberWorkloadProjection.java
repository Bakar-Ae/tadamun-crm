package com.crm.backend.dashboard;

public interface TeamMemberWorkloadProjection {

    Long getUserId();

    String getFullName();

    Long getActiveCustomers();

    Long getActiveLeads();

    Long getOpenTasks();

    Long getCompletedTasks();

    Long getRecentActivities();
}
