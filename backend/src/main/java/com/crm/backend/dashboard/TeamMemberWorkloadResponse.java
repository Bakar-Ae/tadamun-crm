package com.crm.backend.dashboard;

public record TeamMemberWorkloadResponse(
        Long userId,
        String fullName,
        long activeCustomers,
        long activeLeads,
        long openTasks,
        long completedTasks,
        long recentActivities
) {
    public static TeamMemberWorkloadResponse from(
            TeamMemberWorkloadProjection projection
    ) {
        return new TeamMemberWorkloadResponse(
                projection.getUserId(),
                projection.getFullName(),
                valueOrZero(projection.getActiveCustomers()),
                valueOrZero(projection.getActiveLeads()),
                valueOrZero(projection.getOpenTasks()),
                valueOrZero(projection.getCompletedTasks()),
                valueOrZero(projection.getRecentActivities())
        );
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }
}
