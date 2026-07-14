package com.crm.backend.activity;

import com.crm.backend.audit.AuditLog;
import com.crm.backend.audit.AuditLogRepository;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.note.Note;
import com.crm.backend.note.NoteRepository;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.task.CrmTask;
import com.crm.backend.task.TaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ActivityService {

    private static final int MAX_TIMELINE_WINDOW = 1000;

    private final NoteRepository noteRepository;
    private final TaskRepository taskRepository;
    private final AuditLogRepository auditLogRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final DataScopeService dataScopeService;

    public ActivityService(
            NoteRepository noteRepository,
            TaskRepository taskRepository,
            AuditLogRepository auditLogRepository,
            CustomerRepository customerRepository,
            LeadRepository leadRepository,
            DataScopeService dataScopeService
    ) {
        this.noteRepository = noteRepository;
        this.taskRepository = taskRepository;
        this.auditLogRepository = auditLogRepository;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.dataScopeService = dataScopeService;
    }

    @Transactional(readOnly = true)
    public Page<ActivityEventResponse> getCustomerActivity(
            Long customerId,
            Pageable pageable
    ) {
        DataScopeContext context = dataScopeService.currentContext();
        requireCustomerAccess(customerId, context);

        PageRequest sourcePage = createSourcePage(pageable);

        Page<Note> notes = noteRepository.findByCustomerId(
                customerId,
                sourcePage
        );

        Page<CrmTask> tasks = taskRepository.searchAccessibleTasks(
                null,
                null,
                null,
                null,
                customerId,
                null,
                isAllAccess(context),
                isTeamAccess(context),
                context.userId(),
                context.teamId(),
                sourcePage
        );

        Page<AuditLog> audits =
                auditLogRepository.findByEntityTypeAndEntityId(
                        "CUSTOMER",
                        customerId,
                        sourcePage
                );

        return combineEvents(notes, tasks, audits, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ActivityEventResponse> getLeadActivity(
            Long leadId,
            Pageable pageable
    ) {
        DataScopeContext context = dataScopeService.currentContext();
        requireLeadAccess(leadId, context);

        PageRequest sourcePage = createSourcePage(pageable);

        Page<Note> notes = noteRepository.findByLeadId(
                leadId,
                sourcePage
        );

        Page<CrmTask> tasks = taskRepository.searchAccessibleTasks(
                null,
                null,
                null,
                null,
                null,
                leadId,
                isAllAccess(context),
                isTeamAccess(context),
                context.userId(),
                context.teamId(),
                sourcePage
        );

        Page<AuditLog> audits =
                auditLogRepository.findByEntityTypeAndEntityId(
                        "LEAD",
                        leadId,
                        sourcePage
                );

        return combineEvents(notes, tasks, audits, pageable);
    }

    private Page<ActivityEventResponse> combineEvents(
            Page<Note> notes,
            Page<CrmTask> tasks,
            Page<AuditLog> audits,
            Pageable pageable
    ) {
        List<ActivityEventResponse> events = new ArrayList<>();

        notes.forEach(note -> events.add(toResponse(note)));
        tasks.forEach(task -> events.add(toResponse(task)));
        audits.forEach(audit -> events.add(toResponse(audit)));

        events.sort(
                Comparator.comparing(
                        ActivityEventResponse::occurredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), events.size());

        List<ActivityEventResponse> pageContent =
                start >= events.size()
                        ? List.of()
                        : List.copyOf(events.subList(start, end));

        long totalElements =
                notes.getTotalElements()
                        + tasks.getTotalElements()
                        + audits.getTotalElements();

        return new PageImpl<>(
                pageContent,
                pageable,
                totalElements
        );
    }

    private ActivityEventResponse toResponse(Note note) {
        return new ActivityEventResponse(
                "NOTE-" + note.getId(),
                ActivityEventType.NOTE,
                note.getId(),
                "Note added",
                note.getContent(),
                note.getCreatedByUser() == null
                        ? null
                        : note.getCreatedByUser().getFullName(),
                null,
                note.getCreatedAt()
        );
    }

    private ActivityEventResponse toResponse(CrmTask task) {
        return new ActivityEventResponse(
                "TASK-" + task.getId(),
                ActivityEventType.TASK,
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getAssignedToUser() == null
                        ? null
                        : task.getAssignedToUser().getFullName(),
                task.getStatus() == null
                        ? null
                        : task.getStatus().name(),
                task.getCreatedAt()
        );
    }

    private ActivityEventResponse toResponse(AuditLog auditLog) {
        return new ActivityEventResponse(
                "AUDIT-" + auditLog.getId(),
                ActivityEventType.AUDIT,
                auditLog.getId(),
                humanizeAction(auditLog.getAction()),
                null,
                auditLog.getActorUser() == null
                        ? "System"
                        : auditLog.getActorUser().getFullName(),
                null,
                auditLog.getCreatedAt()
        );
    }

    private void requireCustomerAccess(
            Long customerId,
            DataScopeContext context
    ) {
        customerRepository.findAccessibleById(
                customerId,
                isAllAccess(context),
                isTeamAccess(context),
                context.userId(),
                context.teamId()
        ).orElseThrow(
                () -> new IllegalArgumentException("Customer not found")
        );
    }

    private void requireLeadAccess(
            Long leadId,
            DataScopeContext context
    ) {
        leadRepository.findAccessibleById(
                leadId,
                isAllAccess(context),
                isTeamAccess(context),
                context.userId(),
                context.teamId()
        ).orElseThrow(
                () -> new IllegalArgumentException("Lead not found")
        );
    }

    private PageRequest createSourcePage(Pageable pageable) {
        long requestedEnd =
                pageable.getOffset() + pageable.getPageSize();

        if (requestedEnd > MAX_TIMELINE_WINDOW) {
            throw new IllegalArgumentException(
                    "Activity timeline supports the first 1000 events"
            );
        }

        return PageRequest.of(
                0,
                (int) requestedEnd,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private String humanizeAction(String action) {
        if (action == null || action.isBlank()) {
            return "Record activity";
        }

        String text = action
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ');

        return Character.toUpperCase(text.charAt(0))
                + text.substring(1);
    }

    private boolean isAllAccess(DataScopeContext context) {
        return context.scope() == DataScope.ALL;
    }

    private boolean isTeamAccess(DataScopeContext context) {
        return context.scope() == DataScope.TEAM;
    }
}