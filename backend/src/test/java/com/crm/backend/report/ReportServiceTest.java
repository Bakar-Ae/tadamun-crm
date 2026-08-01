package com.crm.backend.report;

import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.lead.LeadStatus;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
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
    private CustomerRepository customerRepository;
    private LeadRepository leadRepository;
    private TaskRepository taskRepository;
    private DataScopeService dataScopeService;
    private CurrentOrganizationProvider currentOrganizationProvider;
    private DataScopeContext context;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        analyticsRepository = mock(ReportAnalyticsRepository.class);
        customerRepository = mock(CustomerRepository.class);
        leadRepository = mock(LeadRepository.class);
        taskRepository = mock(TaskRepository.class);
        dataScopeService = mock(DataScopeService.class);
        currentOrganizationProvider = mock(CurrentOrganizationProvider.class);
        context = new DataScopeContext(1L, null, DataScope.ALL);

        when(dataScopeService.currentContext()).thenReturn(context);
        when(currentOrganizationProvider.getOrganizationId()).thenReturn(1L);

        reportService = new ReportService(
                customerRepository,
                leadRepository,
                taskRepository,
                analyticsRepository,
                dataScopeService,
                currentOrganizationProvider,
                "Africa/Mogadishu"
        );
    }

    @Test
    void summaryReportShouldUseAccessibleCounts() {
        when(customerRepository.countAccessibleByStatusInOrganization(
                1L, CustomerStatus.ACTIVE, true, false, 1L, null
        )).thenReturn(3L);
        when(customerRepository.countAccessibleByStatusInOrganization(
                1L, CustomerStatus.ARCHIVED, true, false, 1L, null
        )).thenReturn(1L);
        when(leadRepository.countAccessibleByStatusInOrganization(
                1L, LeadStatus.NEW, true, false, 1L, null
        )).thenReturn(2L);
        when(leadRepository.countAccessibleByStatusInOrganization(
                1L, LeadStatus.CONTACTED, true, false, 1L, null
        )).thenReturn(1L);
        when(leadRepository.countAccessibleByStatusInOrganization(
                1L, LeadStatus.QUALIFIED, true, false, 1L, null
        )).thenReturn(1L);
        when(leadRepository.countAccessibleByStatusInOrganization(
                1L, LeadStatus.CONVERTED, true, false, 1L, null
        )).thenReturn(1L);
        when(leadRepository.countAccessibleByStatusInOrganization(
                1L, LeadStatus.ARCHIVED, true, false, 1L, null
        )).thenReturn(1L);
        when(taskRepository.countAccessibleByStatusInOrganization(
                1L, TaskStatus.OPEN, true, false, 1L, null
        )).thenReturn(4L);
        when(taskRepository.countAccessibleByStatusInOrganization(
                1L, TaskStatus.COMPLETED, true, false, 1L, null
        )).thenReturn(2L);

        ReportSummaryResponse response = reportService.getSummaryReport();

        assertEquals(4, response.totalCustomers());
        assertEquals(6, response.totalLeads());
        assertEquals(6, response.totalTasks());
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

        when(analyticsRepository.countCustomersCreated(
                localFrom, localTo, context, 1L
        ))
                .thenReturn(3L);
        when(analyticsRepository.countLeadsByStatus(
                localFrom, localTo, context, 1L
        ))
                .thenReturn(List.of(
                        new ReportBreakdownItem("NEW", 2),
                        new ReportBreakdownItem("QUALIFIED", 1)
                ));
        when(analyticsRepository.countTasksByStatus(
                localFrom, localTo, context, 1L
        ))
                .thenReturn(List.of(new ReportBreakdownItem("OPEN", 4)));
        when(analyticsRepository.countTasksByPriority(
                localFrom, localTo, context, 1L
        ))
                .thenReturn(List.of(new ReportBreakdownItem("HIGH", 4)));
        when(analyticsRepository.countAuditEventsByAction(
                "LEAD_CONVERTED", localFrom, localTo, context, 1L
        )).thenReturn(1L);
        when(analyticsRepository.countAuditEventsByAction(
                "TASK_COMPLETED", localFrom, localTo, context, 1L
        )).thenReturn(2L);
        when(analyticsRepository.countAuditEventsByEntityType(
                "CUSTOMER", localFrom, localTo, context, 1L
        )).thenReturn(5L);
        when(analyticsRepository.countDailyActivity(
                localFrom, localTo, context, 1L
        ))
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
