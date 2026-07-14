package com.crm.backend.dashboard;

import com.crm.backend.audit.AuditLogRepository;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.lead.LeadStatus;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.task.TaskRepository;
import com.crm.backend.task.TaskStatus;
import com.crm.backend.team.TeamRepository;
import com.crm.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final TaskRepository taskRepository;
    private final AuditLogRepository auditLogRepository;
    private final TeamRepository teamRepository;
    private final DataScopeService dataScopeService;
    private final ZoneId appTimeZone;

    public DashboardService(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            LeadRepository leadRepository,
            TaskRepository taskRepository,
            AuditLogRepository auditLogRepository,
            TeamRepository teamRepository,
            DataScopeService dataScopeService,
            @Value("${app.time-zone:Africa/Mogadishu}") String appTimeZone
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.taskRepository = taskRepository;
        this.auditLogRepository = auditLogRepository;
        this.teamRepository = teamRepository;
        this.dataScopeService = dataScopeService;
        this.appTimeZone = ZoneId.of(appTimeZone);
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        return buildSummary(dataScopeService.currentContext());
    }

    @Transactional(readOnly = true)
    public TeamDashboardResponse getTeamDashboard() {
        DataScopeContext context = dataScopeService.currentContext();
        DashboardSummaryResponse summary = buildSummary(context);

        List<TeamMemberWorkloadResponse> members = userRepository
                .findDashboardMemberWorkloads(
                        isAllAccess(context),
                        isTeamAccess(context),
                        context.userId(),
                        context.teamId(),
                        PageRequest.of(0, 8)
                )
                .stream()
                .map(TeamMemberWorkloadResponse::from)
                .toList();

        List<TeamActivityResponse> recentActivity = auditLogRepository
                .findAccessibleRecentActivity(
                        isAllAccess(context),
                        isTeamAccess(context),
                        context.userId(),
                        context.teamId(),
                        PageRequest.of(0, 6)
                )
                .map(TeamActivityResponse::from)
                .getContent();

        long relevantTasks = summary.openTasks() + summary.completedTasks();
        int completionRate = relevantTasks == 0
                ? 0
                : (int) Math.round(
                        summary.completedTasks() * 100.0 / relevantTasks
                );

        return new TeamDashboardResponse(
                context.scope().name(),
                context.scope() == DataScope.ALL ? null : context.teamId(),
                resolveTeamName(context),
                summary,
                taskRepository.countAccessibleOverdue(
                        LocalDateTime.now(appTimeZone),
                        isAllAccess(context),
                        isTeamAccess(context),
                        context.userId(),
                        context.teamId()
                ),
                completionRate,
                members,
                recentActivity
        );
    }

    private DashboardSummaryResponse buildSummary(DataScopeContext context) {
        return new DashboardSummaryResponse(
                userRepository.countAccessibleUsers(
                        isAllAccess(context),
                        isTeamAccess(context),
                        context.userId(),
                        context.teamId()
                ),
                countCustomers(CustomerStatus.ACTIVE, context),
                countCustomers(CustomerStatus.ARCHIVED, context),
                countLeads(LeadStatus.NEW, context)
                        + countLeads(LeadStatus.CONTACTED, context)
                        + countLeads(LeadStatus.QUALIFIED, context),
                countTasks(TaskStatus.OPEN, context),
                countTasks(TaskStatus.COMPLETED, context)
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

    private long countLeads(LeadStatus status, DataScopeContext context) {
        return leadRepository.countAccessibleByStatus(
                status,
                isAllAccess(context),
                isTeamAccess(context),
                context.userId(),
                context.teamId()
        );
    }

    private long countTasks(TaskStatus status, DataScopeContext context) {
        return taskRepository.countAccessibleByStatus(
                status,
                isAllAccess(context),
                isTeamAccess(context),
                context.userId(),
                context.teamId()
        );
    }

    private String resolveTeamName(DataScopeContext context) {
        if (context.scope() == DataScope.ALL) {
            return "All teams";
        }

        if (context.teamId() == null) {
            return context.scope() == DataScope.OWN
                    ? "My work"
                    : "Unassigned team";
        }

        return teamRepository.findById(context.teamId())
                .map(team -> team.getName())
                .orElse("Unassigned team");
    }

    private boolean isAllAccess(DataScopeContext context) {
        return context.scope() == DataScope.ALL;
    }

    private boolean isTeamAccess(DataScopeContext context) {
        return context.scope() == DataScope.TEAM;
    }
}
