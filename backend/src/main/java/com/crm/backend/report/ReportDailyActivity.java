package com.crm.backend.report;

import java.time.LocalDate;

public record ReportDailyActivity(
        LocalDate date,
        long count
) {
}
