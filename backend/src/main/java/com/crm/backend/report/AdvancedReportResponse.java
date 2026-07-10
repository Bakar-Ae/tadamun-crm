package com.crm.backend.report;

import java.time.OffsetDateTime;
import java.util.List;

public record AdvancedReportResponse(
        OffsetDateTime from,
        OffsetDateTime to,
        long customersCreated,
        long leadsCreated,
        long leadConversions,
        long tasksCreated,
        long taskCompletions,
        long activitiesRecorded,
        long customerActivities,
        List<ReportBreakdownItem> leadStatusBreakdown,
        List<ReportBreakdownItem> taskStatusBreakdown,
        List<ReportBreakdownItem> taskPriorityBreakdown,
        List<ReportDailyActivity> dailyActivity
) {
}
