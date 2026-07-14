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
import com.crm.backend.team.Team;
import com.crm.backend.team.TeamRepository;
import com.crm.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private UserRepository userRepository;
    private CustomerRepository customerRepository;
    private LeadRepository leadRepository;
    private TaskRepository taskRepository;
    private AuditLogRepository auditLogRepository;
    private TeamRepository teamRepository;
    private DataScopeService dataScopeService;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        customerRepository = mock(CustomerRepository.class);
        leadRepository = mock(LeadRepository.class);
        taskRepository = mock(TaskRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        teamRepository = mock(TeamRepository.class);
        dataScopeService = mock(DataScopeService.class);

        dashboardService = new DashboardService(
                userRepository,
                customerRepository,
                leadRepository,
                taskRepository,
                auditLogRepository,
                teamRepository,
                dataScopeService,
                "Africa/Mogadishu"
        );
    }

    @Test
    void teamDashboardShouldUseCurrentTeamScope() {
        DataScopeContext context =
                new DataScopeContext(3L, 7L, DataScope.TEAM);
        when(dataScopeService.currentContext()).thenReturn(context);

        when(userRepository.countAccessibleUsers(false, true, 3L, 7L))
                .thenReturn(4L);
        when(customerRepository.countAccessibleByStatus(
                CustomerStatus.ACTIVE, false, true, 3L, 7L
        )).thenReturn(5L);
        when(customerRepository.countAccessibleByStatus(
                CustomerStatus.ARCHIVED, false, true, 3L, 7L
        )).thenReturn(1L);
        when(leadRepository.countAccessibleByStatus(
                LeadStatus.NEW, false, true, 3L, 7L
        )).thenReturn(2L);
        when(leadRepository.countAccessibleByStatus(
                LeadStatus.CONTACTED, false, true, 3L, 7L
        )).thenReturn(1L);
        when(leadRepository.countAccessibleByStatus(
                LeadStatus.QUALIFIED, false, true, 3L, 7L
        )).thenReturn(1L);
        when(taskRepository.countAccessibleByStatus(
                TaskStatus.OPEN, false, true, 3L, 7L
        )).thenReturn(3L);
        when(taskRepository.countAccessibleByStatus(
                TaskStatus.COMPLETED, false, true, 3L, 7L
        )).thenReturn(9L);
        when(taskRepository.countAccessibleOverdue(
                any(LocalDateTime.class),
                eq(false),
                eq(true),
                eq(3L),
                eq(7L)
        )).thenReturn(2L);

        TeamMemberWorkloadProjection member =
                mock(TeamMemberWorkloadProjection.class);
        when(member.getUserId()).thenReturn(3L);
        when(member.getFullName()).thenReturn("Sales Manager");
        when(member.getActiveCustomers()).thenReturn(5L);
        when(member.getActiveLeads()).thenReturn(4L);
        when(member.getOpenTasks()).thenReturn(3L);
        when(member.getCompletedTasks()).thenReturn(9L);
        when(member.getRecentActivities()).thenReturn(12L);
        when(userRepository.findDashboardMemberWorkloads(
                false, true, 3L, 7L, PageRequest.of(0, 8)
        )).thenReturn(List.of(member));

        when(auditLogRepository.findAccessibleRecentActivity(
                false, true, 3L, 7L, PageRequest.of(0, 6)
        )).thenReturn(new PageImpl<>(List.of()));

        Team team = new Team();
        team.setId(7L);
        team.setName("Enterprise Sales");
        when(teamRepository.findById(7L)).thenReturn(Optional.of(team));

        TeamDashboardResponse result = dashboardService.getTeamDashboard();

        assertEquals("TEAM", result.scope());
        assertEquals("Enterprise Sales", result.teamName());
        assertEquals(4, result.summary().totalUsers());
        assertEquals(4, result.summary().activeLeads());
        assertEquals(75, result.taskCompletionRate());
        assertEquals(2, result.overdueTasks());
        assertEquals(1, result.members().size());
        assertEquals(12, result.members().getFirst().recentActivities());
    }
}
