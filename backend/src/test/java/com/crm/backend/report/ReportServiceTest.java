package com.crm.backend.report;

import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.lead.LeadStatus;
import com.crm.backend.task.TaskPriority;
import com.crm.backend.task.TaskRepository;
import com.crm.backend.task.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private ReportAnalyticsRepository analyticsRepository;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        analyticsRepository = mock(ReportAnalyticsRepository.class);

        reportService = new ReportService(
                mock(CustomerRepository.class),
                mock(LeadRepository.class),
                mock(TaskRepository.class),
                analyticsRepository,
                "Africa/Mogadishu"
        );
    }

    @Test
    void advancedReportShouldRejectInvalidRange() {
        OffsetDateTime date = OffsetDateTime.parse("2026-07-01T00:00:00Z");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reportService.getAdvancedReport(date, date)
        );

        assertEquals("'from' must be before 'to'", exception.getMessage());
        verifyNoInteractions(analyticsRepository);
    }

    @Test
    void advancedReportShouldUseBusinessTimezoneAndFillMissingGroups() {
        OffsetDateTime from = OffsetDateTime.parse("2026-07-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-07-03T00:00:00Z");
        LocalDateTime localFrom = LocalDateTime.of(2026, 7, 1, 3, 0);
        LocalDateTime localTo = LocalDateTime.of(2026, 7, 3, 3, 0);

        when(analyticsRepository.countCustomersCreated(localFrom, localTo))
                .thenReturn(3L);
        when(analyticsRepository.countLeadsByStatus(localFrom, localTo))
                .thenReturn(List.of(
                        new ReportBreakdownItem("NEW", 2),
                        new ReportBreakdownItem("QUALIFIED", 1)
                ));
        when(analyticsRepository.countTasksByStatus(localFrom, localTo))
                .thenReturn(List.of(new ReportBreakdownItem("OPEN", 4)));
        when(analyticsRepository.countTasksByPriority(localFrom, localTo))
                .thenReturn(List.of(new ReportBreakdownItem("HIGH", 4)));
        when(analyticsRepository.countAuditEventsByAction(
                "LEAD_CONVERTED", localFrom, localTo
        )).thenReturn(1L);
        when(analyticsRepository.countAuditEventsByAction(
                "TASK_COMPLETED", localFrom, localTo
        )).thenReturn(2L);
        when(analyticsRepository.countAuditEventsByEntityType(
                "CUSTOMER", localFrom, localTo
        )).thenReturn(5L);
        when(analyticsRepository.countDailyActivity(localFrom, localTo))
                .thenReturn(List.of(
                        new ReportDailyActivity(LocalDate.of(2026, 7, 1), 6)
                ));

        AdvancedReportResponse response = reportService.getAdvancedReport(from, to);

        assertEquals(3, response.customersCreated());
        assertEquals(3, response.leadsCreated());
        assertEquals(4, response.tasksCreated());
        assertEquals(6, response.activitiesRecorded());
        assertEquals(LeadStatus.values().length, response.leadStatusBreakdown().size());
        assertEquals(TaskStatus.values().length, response.taskStatusBreakdown().size());
        assertEquals(TaskPriority.values().length, response.taskPriorityBreakdown().size());
        assertEquals(3, response.dailyActivity().size());
        assertEquals(0, response.dailyActivity().get(1).count());
    }
}
