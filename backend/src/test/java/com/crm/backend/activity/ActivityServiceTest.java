package com.crm.backend.activity;

import com.crm.backend.audit.AuditLog;
import com.crm.backend.audit.AuditLogRepository;
import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.note.Note;
import com.crm.backend.note.NoteRepository;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.task.CrmTask;
import com.crm.backend.task.TaskRepository;
import com.crm.backend.task.TaskStatus;
import com.crm.backend.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class ActivityServiceTest {

    private NoteRepository noteRepository;
    private TaskRepository taskRepository;
    private AuditLogRepository auditLogRepository;
    private CustomerRepository customerRepository;
    private LeadRepository leadRepository;
    private DataScopeService dataScopeService;
    private CurrentOrganizationProvider currentOrganizationProvider;
    private ActivityService activityService;

    @BeforeEach
    void setUp() {
        noteRepository = mock(NoteRepository.class);
        taskRepository = mock(TaskRepository.class);
        auditLogRepository = mock(AuditLogRepository.class);
        customerRepository = mock(CustomerRepository.class);
        leadRepository = mock(LeadRepository.class);
        dataScopeService = mock(DataScopeService.class);
        currentOrganizationProvider = mock(CurrentOrganizationProvider.class);
        when(currentOrganizationProvider.getOrganizationId()).thenReturn(1L);

        activityService = new ActivityService(
                noteRepository,
                taskRepository,
                auditLogRepository,
                customerRepository,
                leadRepository,
                dataScopeService,
                currentOrganizationProvider
        );
    }

    @Test
    void customerActivityShouldMergeAndSortEvents() {
        DataScopeContext context =
                new DataScopeContext(1L, null, DataScope.ALL);

        when(dataScopeService.currentContext()).thenReturn(context);
        when(customerRepository.findAccessibleByIdInOrganization(
                10L,
                1L,
                true,
                false,
                1L,
                null
        )).thenReturn(Optional.of(new Customer()));

        User user = new User();
        user.setId(1L);
        user.setFullName("System Administrator");

        Note note = new Note();
        note.setId(1L);
        note.setContent("Customer called the sales team");
        note.setCreatedByUser(user);
        note.setCreatedAt(LocalDateTime.of(2026, 7, 10, 8, 0));

        CrmTask task = new CrmTask();
        task.setId(2L);
        task.setTitle("Prepare proposal");
        task.setStatus(TaskStatus.OPEN);
        task.setAssignedToUser(user);
        task.setCreatedAt(LocalDateTime.of(2026, 7, 10, 9, 0));

        AuditLog auditLog = new AuditLog();
        auditLog.setId(3L);
        auditLog.setAction("CUSTOMER_UPDATED");
        auditLog.setActorUser(user);
        auditLog.setCreatedAt(LocalDateTime.of(2026, 7, 10, 10, 0));

        PageRequest sourcePage = PageRequest.of(
                0,
                20,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        when(noteRepository.findByOrganizationIdAndCustomerId(
                1L,
                10L,
                sourcePage
        ))
                .thenReturn(new PageImpl<>(
                        List.of(note),
                        sourcePage,
                        1
                ));

        when(taskRepository.searchAccessibleTasksInOrganization(
                eq(1L),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                eq(10L),
                isNull(),
                eq(true),
                eq(false),
                eq(1L),
                isNull(),
                eq(sourcePage)
        )).thenReturn(new PageImpl<>(
                List.of(task),
                sourcePage,
                1
        ));

        when(auditLogRepository.findByOrganizationIdAndEntityTypeAndEntityId(
                1L,
                "CUSTOMER",
                10L,
                sourcePage
        )).thenReturn(new PageImpl<>(
                List.of(auditLog),
                sourcePage,
                1
        ));

        var result = activityService.getCustomerActivity(
                10L,
                PageRequest.of(0, 20)
        );

        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getContent().size());
        assertEquals(
                ActivityEventType.AUDIT,
                result.getContent().get(0).type()
        );
        assertEquals(
                ActivityEventType.TASK,
                result.getContent().get(1).type()
        );
        assertEquals(
                ActivityEventType.NOTE,
                result.getContent().get(2).type()
        );
    }

    @Test
    void customerActivityShouldHideInaccessibleCustomer() {
        DataScopeContext context =
                new DataScopeContext(1L, 9L, DataScope.TEAM);

        when(dataScopeService.currentContext()).thenReturn(context);
        when(customerRepository.findAccessibleByIdInOrganization(
                99L,
                1L,
                false,
                true,
                1L,
                9L
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> activityService.getCustomerActivity(
                        99L,
                        PageRequest.of(0, 20)
                )
        );

        assertEquals("Customer not found", exception.getMessage());
        verifyNoInteractions(
                noteRepository,
                taskRepository,
                auditLogRepository
        );
    }
}
