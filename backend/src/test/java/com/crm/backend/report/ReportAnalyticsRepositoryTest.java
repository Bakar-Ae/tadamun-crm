package com.crm.backend.report;

import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.support.MySqlTestContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(MySqlTestContainerConfiguration.class)
@Transactional
class ReportAnalyticsRepositoryTest {

    @Autowired
    private ReportAnalyticsRepository reportAnalyticsRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void aggregateQueriesShouldExecuteAgainstMySql() {
        LocalDateTime from = LocalDateTime.now().minusYears(10);
        LocalDateTime to = LocalDateTime.now().plusYears(10);
        DataScopeContext context = new DataScopeContext(
                1L, null, DataScope.ALL
        );
        Long organizationId = organizationRepository.findBySlug("tadamun")
                .orElseThrow()
                .getId();

        assertTrue(reportAnalyticsRepository.countCustomersCreated(
                from, to, context, organizationId
        ) >= 0);
        assertNotNull(reportAnalyticsRepository.countLeadsByStatus(
                from, to, context, organizationId
        ));
        assertNotNull(reportAnalyticsRepository.countTasksByStatus(
                from, to, context, organizationId
        ));
        assertNotNull(reportAnalyticsRepository.countTasksByPriority(
                from, to, context, organizationId
        ));
        assertTrue(reportAnalyticsRepository.countAuditEventsByAction(
                "LEAD_CONVERTED", from, to, context, organizationId
        ) >= 0);
        assertTrue(reportAnalyticsRepository.countAuditEventsByEntityType(
                "CUSTOMER", from, to, context, organizationId
        ) >= 0);
        assertNotNull(reportAnalyticsRepository.countDailyActivity(
                from, to, context, organizationId
        ));
    }
}
