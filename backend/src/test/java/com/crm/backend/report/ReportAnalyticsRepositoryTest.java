package com.crm.backend.report;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class ReportAnalyticsRepositoryTest {

    @Autowired
    private ReportAnalyticsRepository reportAnalyticsRepository;

    @Test
    void aggregateQueriesShouldExecuteAgainstMySql() {
        LocalDateTime from = LocalDateTime.now().minusYears(10);
        LocalDateTime to = LocalDateTime.now().plusYears(10);

        assertTrue(reportAnalyticsRepository.countCustomersCreated(from, to) >= 0);
        assertNotNull(reportAnalyticsRepository.countLeadsByStatus(from, to));
        assertNotNull(reportAnalyticsRepository.countTasksByStatus(from, to));
        assertNotNull(reportAnalyticsRepository.countTasksByPriority(from, to));
        assertTrue(reportAnalyticsRepository.countAuditEventsByAction(
                "LEAD_CONVERTED", from, to
        ) >= 0);
        assertTrue(reportAnalyticsRepository.countAuditEventsByEntityType(
                "CUSTOMER", from, to
        ) >= 0);
        assertNotNull(reportAnalyticsRepository.countDailyActivity(from, to));
    }
}
