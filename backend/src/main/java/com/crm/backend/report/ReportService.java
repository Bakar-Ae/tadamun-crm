package com.crm.backend.report;

import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.lead.LeadStatus;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.task.TaskRepository;
import com.crm.backend.task.TaskPriority;
import com.crm.backend.task.TaskStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final TaskRepository taskRepository;
    private final ReportAnalyticsRepository reportAnalyticsRepository;
    private final DataScopeService dataScopeService;
    private final ZoneId appTimeZone;

    public ReportService(
            CustomerRepository customerRepository,
            LeadRepository leadRepository,
            TaskRepository taskRepository,
            ReportAnalyticsRepository reportAnalyticsRepository,
            DataScopeService dataScopeService,
            @Value("${app.time-zone:Africa/Mogadishu}") String appTimeZone
    ) {
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.taskRepository = taskRepository;
        this.reportAnalyticsRepository = reportAnalyticsRepository;
        this.dataScopeService = dataScopeService;
        this.appTimeZone = ZoneId.of(appTimeZone);
    }

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummaryReport() {
        DataScopeContext context = dataScopeService.currentContext();
        long activeCustomers = countCustomers(CustomerStatus.ACTIVE, context);
        long archivedCustomers = countCustomers(CustomerStatus.ARCHIVED, context);
        long newLeads = countLeads(LeadStatus.NEW, context);
        long contactedLeads = countLeads(LeadStatus.CONTACTED, context);
        long qualifiedLeads = countLeads(LeadStatus.QUALIFIED, context);
        long convertedLeads = countLeads(LeadStatus.CONVERTED, context);
        long lostLeads = countLeads(LeadStatus.LOST, context);
        long archivedLeads = countLeads(LeadStatus.ARCHIVED, context);
        long openTasks = countTasks(TaskStatus.OPEN, context);
        long inProgressTasks = countTasks(TaskStatus.IN_PROGRESS, context);
        long completedTasks = countTasks(TaskStatus.COMPLETED, context);
        long cancelledTasks = countTasks(TaskStatus.CANCELLED, context);

        return new ReportSummaryResponse(
                activeCustomers + archivedCustomers,
                activeCustomers,
                archivedCustomers,
                newLeads + contactedLeads + qualifiedLeads
                        + convertedLeads + lostLeads + archivedLeads,
                newLeads,
                qualifiedLeads,
                convertedLeads,
                lostLeads,
                openTasks + inProgressTasks + completedTasks + cancelledTasks,
                openTasks,
                inProgressTasks,
                completedTasks,
                cancelledTasks
        );
    }

    @Transactional(readOnly = true)
    public AdvancedReportResponse getAdvancedReport(
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        validateRange(from, to);
        DataScopeContext context = dataScopeService.currentContext();

        LocalDateTime localFrom = from.atZoneSameInstant(appTimeZone).toLocalDateTime();
        LocalDateTime localTo = to.atZoneSameInstant(appTimeZone).toLocalDateTime();

        List<ReportBreakdownItem> leadStatuses = includeZeroCounts(
                LeadStatus.values(),
                reportAnalyticsRepository.countLeadsByStatus(
                        localFrom, localTo, context
                )
        );

        List<ReportBreakdownItem> taskStatuses = includeZeroCounts(
                TaskStatus.values(),
                reportAnalyticsRepository.countTasksByStatus(
                        localFrom, localTo, context
                )
        );

        List<ReportBreakdownItem> taskPriorities = includeZeroCounts(
                TaskPriority.values(),
                reportAnalyticsRepository.countTasksByPriority(
                        localFrom, localTo, context
                )
        );

        List<ReportDailyActivity> dailyActivity = includeEmptyDays(
                localFrom.toLocalDate(),
                localTo.minusNanos(1).toLocalDate(),
                reportAnalyticsRepository.countDailyActivity(
                        localFrom, localTo, context
                )
        );

        long leadsCreated = sumCounts(leadStatuses);
        long tasksCreated = sumCounts(taskStatuses);
        long activitiesRecorded = dailyActivity.stream()
                .mapToLong(ReportDailyActivity::count)
                .sum();

        return new AdvancedReportResponse(
                from,
                to,
                reportAnalyticsRepository.countCustomersCreated(
                        localFrom, localTo, context
                ),
                leadsCreated,
                reportAnalyticsRepository.countAuditEventsByAction(
                        "LEAD_CONVERTED", localFrom, localTo, context
                ),
                tasksCreated,
                reportAnalyticsRepository.countAuditEventsByAction(
                        "TASK_COMPLETED", localFrom, localTo, context
                ),
                activitiesRecorded,
                reportAnalyticsRepository.countAuditEventsByEntityType(
                        "CUSTOMER", localFrom, localTo, context
                ),
                leadStatuses,
                taskStatuses,
                taskPriorities,
                dailyActivity
        );
    }

    private long countCustomers(
            CustomerStatus status,
            DataScopeContext context
    ) {
        return customerRepository.countAccessibleByStatus(
                status,
                isAllAccess(context),
                isTeamAccess(context),
                context.userId(),
                context.teamId()
        );
    }

    private long countLeads(
            LeadStatus status,
            DataScopeContext context
    ) {
        return leadRepository.countAccessibleByStatus(
                status,
                isAllAccess(context),
                isTeamAccess(context),
                context.userId(),
                context.teamId()
        );
    }

    private long countTasks(
            TaskStatus status,
            DataScopeContext context
    ) {
        return taskRepository.countAccessibleByStatus(
                status,
                isAllAccess(context),
                isTeamAccess(context),
                context.userId(),
                context.teamId()
        );
    }

    private static boolean isAllAccess(DataScopeContext context) {
        return context.scope() == DataScope.ALL;
    }

    private static boolean isTeamAccess(DataScopeContext context) {
        return context.scope() == DataScope.TEAM;
    }

    private static void validateRange(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Report date range is required");
        }

        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        }

        if (Duration.between(from, to).compareTo(Duration.ofDays(366)) > 0) {
            throw new IllegalArgumentException("Report range cannot exceed 366 days");
        }
    }

    private static long sumCounts(List<ReportBreakdownItem> counts) {
        return counts.stream()
                .mapToLong(ReportBreakdownItem::count)
                .sum();
    }

    private static <E extends Enum<E>> List<ReportBreakdownItem> includeZeroCounts(
            E[] values,
            List<ReportBreakdownItem> actualCounts
    ) {
        Map<String, ReportBreakdownItem> countByKey = actualCounts.stream()
                .collect(Collectors.toMap(
                        ReportBreakdownItem::key,
                        Function.identity(),
                        (first, ignored) -> first
                ));

        return Arrays.stream(values)
                .map(value -> countByKey.getOrDefault(
                        value.name(),
                        new ReportBreakdownItem(value.name(), 0)
                ))
                .toList();
    }

    private static List<ReportDailyActivity> includeEmptyDays(
            LocalDate firstDay,
            LocalDate lastDay,
            List<ReportDailyActivity> actualCounts
    ) {
        Map<LocalDate, ReportDailyActivity> countByDate = actualCounts.stream()
                .collect(Collectors.toMap(
                        ReportDailyActivity::date,
                        Function.identity(),
                        (first, ignored) -> first
                ));

        List<ReportDailyActivity> completeSeries = new ArrayList<>();

        for (LocalDate day = firstDay; !day.isAfter(lastDay); day = day.plusDays(1)) {
            completeSeries.add(countByDate.getOrDefault(
                    day,
                    new ReportDailyActivity(day, 0)
            ));
        }

        return List.copyOf(completeSeries);
    }
}
