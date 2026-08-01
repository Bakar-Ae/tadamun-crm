package com.crm.backend.task;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.organization.Organization;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.task.dto.CalendarTaskResponse;
import com.crm.backend.task.dto.CreateTaskRequest;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    private TaskRepository taskRepository;
    private TaskMapper taskMapper;
    private UserRepository userRepository;
    private DataScopeService dataScopeService;
    private CurrentOrganizationProvider currentOrganizationProvider;
    private Organization organization;
    private TaskService taskService;

    private final DataScopeContext allAccessContext =
            new DataScopeContext(1L, null, DataScope.ALL);

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        taskMapper = mock(TaskMapper.class);
        userRepository = mock(UserRepository.class);
        dataScopeService = mock(DataScopeService.class);
        currentOrganizationProvider =
                mock(CurrentOrganizationProvider.class);
        organization = new Organization();
        organization.setId(1L);

        when(dataScopeService.currentContext()).thenReturn(allAccessContext);
        when(currentOrganizationProvider.getOrganizationId())
                .thenReturn(1L);
        when(currentOrganizationProvider.getOrganizationReference())
                .thenReturn(organization);

        taskService = new TaskService(
                taskRepository,
                userRepository,
                mock(CustomerRepository.class),
                mock(LeadRepository.class),
                taskMapper,
                mock(AuditLogService.class),
                dataScopeService,
                currentOrganizationProvider,
                "Africa/Mogadishu"
        );
    }

    @Test
    void calendarShouldRejectInvalidRange() {
        OffsetDateTime time =
                OffsetDateTime.parse("2026-07-01T00:00:00Z");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.getCalendarTasks(
                        time,
                        time,
                        null,
                        PageRequest.of(0, 20)
                )
        );

        assertEquals("'from' must be before 'to'", exception.getMessage());
        verifyNoInteractions(taskRepository);
    }

    @Test
    void calendarShouldConvertRangeToBusinessTimezone() {
        OffsetDateTime from =
                OffsetDateTime.parse("2026-07-01T00:00:00Z");

        OffsetDateTime to =
                OffsetDateTime.parse("2026-07-02T00:00:00Z");

        LocalDateTime localFrom =
                LocalDateTime.of(2026, 7, 1, 3, 0);

        LocalDateTime localTo =
                LocalDateTime.of(2026, 7, 2, 3, 0);

        PageRequest pageable = PageRequest.of(0, 100);

        CrmTask task = new CrmTask();
        task.setId(7L);

        CalendarTaskResponse mapped = new CalendarTaskResponse(
                7L,
                "Customer follow-up",
                TaskStatus.OPEN,
                TaskPriority.HIGH,
                localFrom,
                2L,
                "Sales User",
                null,
                null,
                null,
                null
        );

        when(taskRepository.findAccessibleCalendarTasksInOrganization(
                1L,
                localFrom,
                localTo,
                2L,
                true,
                false,
                1L,
                null,
                pageable
        )).thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        when(taskMapper.toCalendarResponse(task)).thenReturn(mapped);

        var result = taskService.getCalendarTasks(
                from,
                to,
                2L,
                pageable
        );

        assertEquals(1, result.getTotalElements());
        assertSame(mapped, result.getContent().get(0));

        verify(taskRepository).findAccessibleCalendarTasksInOrganization(
                1L,
                localFrom,
                localTo,
                2L,
                true,
                false,
                1L,
                null,
                pageable
        );
    }

    @Test
    void createTaskShouldDefaultAssigneeToCurrentUser() {
        DataScopeContext ownContext =
                new DataScopeContext(7L, 3L, DataScope.OWN);
        when(dataScopeService.currentContext()).thenReturn(ownContext);

        User currentUser = new User();
        currentUser.setId(7L);
        currentUser.setStatus(UserStatus.ACTIVE);
        when(userRepository.findById(7L)).thenReturn(Optional.of(currentUser));
        when(taskRepository.save(any(CrmTask.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateTaskRequest request = new CreateTaskRequest(
                "Call customer",
                null,
                TaskPriority.HIGH,
                null,
                null,
                null,
                null
        );

        taskService.createTask(request);

        verify(taskRepository).save(argThat(task ->
                task.getAssignedToUser() == currentUser
                        && task.getOrganization() == organization
                        && task.getStatus() == TaskStatus.OPEN
        ));
        verify(dataScopeService).requireAccess(
                ownContext,
                7L,
                null
        );
    }

    @Test
    void getTaskShouldHideTaskOutsideCurrentScope() {
        DataScopeContext ownContext =
                new DataScopeContext(7L, 3L, DataScope.OWN);
        when(dataScopeService.currentContext()).thenReturn(ownContext);
        when(taskRepository.findAccessibleByIdInOrganization(
                99L,
                1L,
                false,
                false,
                7L,
                3L
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.getTaskById(99L)
        );

        assertEquals("Task not found", exception.getMessage());
    }
}
