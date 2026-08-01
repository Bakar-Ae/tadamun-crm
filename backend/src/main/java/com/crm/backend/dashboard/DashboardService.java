package com.crm.backend.dashboard;

import com.crm.backend.audit.AuditLogRepository;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.lead.LeadStatus;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
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
    private final CurrentOrganizationProvider currentOrganizationProvider;
    private final ZoneId appTimeZone;

    public DashboardService(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            LeadRepository leadRepository,
            TaskRepository taskRepository,
            AuditLogRepository auditLogRepository,
            TeamRepository teamRepository,
            DataScopeService dataScopeService,
            CurrentOrganizationProvider currentOrganizationProvider,
            @Value("${app.time-zone:Africa/Mogadishu}") String appTimeZone
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.taskRepository = taskRepository;
        this.auditLogRepository = auditLogRepository;
        this.teamRepository = teamRepository;
        this.dataScopeService = dataScopeService;
        this.currentOrganizationProvider = currentOrganizationProvider;
        this.appTimeZone = ZoneId.of(appTimeZone);
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        return buildSummary(
                dataScopeService.currentContext(),
                currentOrganizationProvider.getOrganizationId()
        );
    }

    @Transactional(readOnly = true)
    public TeamDashboardResponse getTeamDashboard() {
        DataScopeContext context = dataScopeService.currentContext();
        Long organizationId = currentOrganizationProvider.getOrganizationId();
        DashboardSummaryResponse summary = buildSummary(
                context,
                organizationId
        );

        List<TeamMemberWorkloadResponse> members = userRepository
                .findDashboardMemberWorkloadsInOrganization(
                        organizationId,
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
                .findAccessibleRecentActivityInOrganization(
                        organizationId,
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
                resolveTeamName(context, organizationId),
                summary,
                taskRepository.countAccessibleOverdueInOrganization(
                        organizationId,
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

    private DashboardSummaryResponse buildSummary(
            DataScopeContext context,
            Long organizationId
    ) {
        return new DashboardSummaryResponse(
                userRepository.countAccessibleUsersInOrganization(
                        organizationId,
                        isAllAccess(context),
                        isTeamAccess(context),
                        context.userId(),
                        context.teamId()
                ),
                countCustomers(CustomerStatus.ACTIVE, context, organizationId),
                countCustomers(CustomerStatus.ARCHIVED, context, organizationId),
                countLeads(LeadStatus.NEW, context, organizationId)
                        + countLeads(LeadStatus.CONTACTED, context, organizationId)
                        + countLeads(LeadStatus.QUALIFIED, context, organizationId),
                countTasks(TaskStatus.OPEN, context, organizationId),
                countTasks(TaskStatus.COMPLETED, context, organizationId)
        );
    }

    private long countCustomers(
            CustomerStatus status,
            DataScopeContext context,
            Long organizationId
    ) {
        return customerRepository.countAccessibleByStatusInOrganization(
                organizationId,
                status,
                isAllAccess(context),
                isTeamAccess(context),
                context.userId(),
                context.teamId()
        );
    }

    private long countLeads(
            LeadStatus status,
            DataScopeContext context,
            Long organizationId
    ) {
        return leadRepository.countAccessibleByStatusInOrganization(
                organizationId,
                status,
                isAllAccess(context),
                isTeamAccess(context),
                context.userId(),
                context.teamId()
        );
    }

    private long countTasks(
            TaskStatus status,
            DataScopeContext context,
            Long organizationId
    ) {
        return taskRepository.countAccessibleByStatusInOrganization(
                organizationId,
                status,
                isAllAccess(context),
                isTeamAccess(context),
                context.userId(),
                context.teamId()
        );
    }

    private String resolveTeamName(
            DataScopeContext context,
            Long organizationId
    ) {
        if (context.scope() == DataScope.ALL) {
            return "All teams";
        }

        if (context.teamId() == null) {
            return context.scope() == DataScope.OWN
                    ? "My work"
                    : "Unassigned team";
        }

        return teamRepository.findByIdAndOrganizationId(
                        context.teamId(),
                        organizationId
                )
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
