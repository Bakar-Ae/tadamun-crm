package com.crm.backend.report;

import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
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
        DataScopeContext context = new DataScopeContext(
                1L, null, DataScope.ALL
        );

        assertTrue(reportAnalyticsRepository.countCustomersCreated(
                from, to, context
        ) >= 0);
        assertNotNull(reportAnalyticsRepository.countLeadsByStatus(
                from, to, context
        ));
        assertNotNull(reportAnalyticsRepository.countTasksByStatus(
                from, to, context
        ));
        assertNotNull(reportAnalyticsRepository.countTasksByPriority(
                from, to, context
        ));
        assertTrue(reportAnalyticsRepository.countAuditEventsByAction(
                "LEAD_CONVERTED", from, to, context
        ) >= 0);
        assertTrue(reportAnalyticsRepository.countAuditEventsByEntityType(
                "CUSTOMER", from, to, context
        ) >= 0);
        assertNotNull(reportAnalyticsRepository.countDailyActivity(
                from, to, context
        ));
    }
}
