package com.crm.backend.dashboard;

import java.util.List;

public record TeamDashboardResponse(
        String scope,
        Long teamId,
        String teamName,
        DashboardSummaryResponse summary,
        long overdueTasks,
        int taskCompletionRate,
        List<TeamMemberWorkloadResponse> members,
        List<TeamActivityResponse> recentActivity
) {
}
