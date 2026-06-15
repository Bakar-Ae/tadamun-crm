package com.crm.backend.report;

public record ReportSummaryResponse(
        long totalCustomers,
        long activeCustomers,
        long archivedCustomers,
        long totalLeads,
        long newLeads,
        long qualifiedLeads,
        long convertedLeads,
        long lostLeads,
        long totalTasks,
        long openTasks,
        long inProgressTasks,
        long completedTasks,
        long cancelledTasks
) {
}
