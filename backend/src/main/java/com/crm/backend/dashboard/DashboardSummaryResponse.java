package com.crm.backend.dashboard;

public record DashboardSummaryResponse(
        long totalUsers,
        long activeCustomers,
        long archivedCustomers,
        long activeLeads,
        long openTasks,
        long completedTasks
) {
}