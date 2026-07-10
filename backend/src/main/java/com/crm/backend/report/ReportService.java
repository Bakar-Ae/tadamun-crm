package com.crm.backend.report;

import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.lead.LeadStatus;
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
    private final ZoneId appTimeZone;

    public ReportService(
            CustomerRepository customerRepository,
            LeadRepository leadRepository,
            TaskRepository taskRepository,
            ReportAnalyticsRepository reportAnalyticsRepository,
            @Value("${app.time-zone:Africa/Mogadishu}") String appTimeZone
    ) {
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.taskRepository = taskRepository;
        this.reportAnalyticsRepository = reportAnalyticsRepository;
        this.appTimeZone = ZoneId.of(appTimeZone);
    }

    @Transactional(readOnly = true)
    public ReportSummaryResponse getSummaryReport() {
        return new ReportSummaryResponse(
                customerRepository.count(),
                customerRepository.countByStatus(CustomerStatus.ACTIVE),
                customerRepository.countByStatus(CustomerStatus.ARCHIVED),
                leadRepository.count(),
                leadRepository.countByStatus(LeadStatus.NEW),
                leadRepository.countByStatus(LeadStatus.QUALIFIED),
                leadRepository.countByStatus(LeadStatus.CONVERTED),
                leadRepository.countByStatus(LeadStatus.LOST),
                taskRepository.count(),
                taskRepository.countByStatus(TaskStatus.OPEN),
                taskRepository.countByStatus(TaskStatus.IN_PROGRESS),
                taskRepository.countByStatus(TaskStatus.COMPLETED),
                taskRepository.countByStatus(TaskStatus.CANCELLED)
        );
    }

    @Transactional(readOnly = true)
    public AdvancedReportResponse getAdvancedReport(
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        validateRange(from, to);

        LocalDateTime localFrom = from.atZoneSameInstant(appTimeZone).toLocalDateTime();
        LocalDateTime localTo = to.atZoneSameInstant(appTimeZone).toLocalDateTime();

        List<ReportBreakdownItem> leadStatuses = includeZeroCounts(
                LeadStatus.values(),
                reportAnalyticsRepository.countLeadsByStatus(localFrom, localTo)
        );

        List<ReportBreakdownItem> taskStatuses = includeZeroCounts(
                TaskStatus.values(),
                reportAnalyticsRepository.countTasksByStatus(localFrom, localTo)
        );

        List<ReportBreakdownItem> taskPriorities = includeZeroCounts(
                TaskPriority.values(),
                reportAnalyticsRepository.countTasksByPriority(localFrom, localTo)
        );

        List<ReportDailyActivity> dailyActivity = includeEmptyDays(
                localFrom.toLocalDate(),
                localTo.minusNanos(1).toLocalDate(),
                reportAnalyticsRepository.countDailyActivity(localFrom, localTo)
        );

        long leadsCreated = sumCounts(leadStatuses);
        long tasksCreated = sumCounts(taskStatuses);
        long activitiesRecorded = dailyActivity.stream()
                .mapToLong(ReportDailyActivity::count)
                .sum();

        return new AdvancedReportResponse(
                from,
                to,
                reportAnalyticsRepository.countCustomersCreated(localFrom, localTo),
                leadsCreated,
                reportAnalyticsRepository.countAuditEventsByAction(
                        "LEAD_CONVERTED", localFrom, localTo
                ),
                tasksCreated,
                reportAnalyticsRepository.countAuditEventsByAction(
                        "TASK_COMPLETED", localFrom, localTo
                ),
                activitiesRecorded,
                reportAnalyticsRepository.countAuditEventsByEntityType(
                        "CUSTOMER", localFrom, localTo
                ),
                leadStatuses,
                taskStatuses,
                taskPriorities,
                dailyActivity
        );
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
