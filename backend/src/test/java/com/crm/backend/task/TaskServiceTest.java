package com.crm.backend.task;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.task.dto.CalendarTaskResponse;
import com.crm.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class TaskServiceTest {

    private TaskRepository taskRepository;
    private TaskMapper taskMapper;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        taskMapper = mock(TaskMapper.class);

        taskService = new TaskService(
                taskRepository,
                mock(UserRepository.class),
                mock(CustomerRepository.class),
                mock(LeadRepository.class),
                taskMapper,
                mock(AuditLogService.class),
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

        when(taskRepository.findCalendarTasks(
                localFrom,
                localTo,
                2L,
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

        verify(taskRepository).findCalendarTasks(
                localFrom,
                localTo,
                2L,
                pageable
        );
    }
}