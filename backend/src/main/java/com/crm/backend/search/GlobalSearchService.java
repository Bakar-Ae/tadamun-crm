package com.crm.backend.search;

import com.crm.backend.contact.ContactRepository;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.note.NoteRepository;
import com.crm.backend.permission.PermissionName;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.task.TaskRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class GlobalSearchService {

    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final ContactRepository contactRepository;
    private final TaskRepository taskRepository;
    private final NoteRepository noteRepository;
    private final DataScopeService dataScopeService;
    private final CurrentOrganizationProvider currentOrganizationProvider;

    public GlobalSearchService(
            CustomerRepository customerRepository,
            LeadRepository leadRepository,
            ContactRepository contactRepository,
            TaskRepository taskRepository,
            NoteRepository noteRepository,
            DataScopeService dataScopeService,
            CurrentOrganizationProvider currentOrganizationProvider
    ) {
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.contactRepository = contactRepository;
        this.taskRepository = taskRepository;
        this.noteRepository = noteRepository;
        this.dataScopeService = dataScopeService;
        this.currentOrganizationProvider = currentOrganizationProvider;
    }

    @Transactional(readOnly = true)
    public GlobalSearchResponse search(
            String rawQuery,
            Integer requestedLimit,
            Set<SearchModule> requestedModules
    ) {
        String query = validateQuery(rawQuery);
        int limit = validateLimit(requestedLimit);
        Set<SearchModule> modules = resolveModules(requestedModules);

        DataScopeContext context = dataScopeService.currentContext();
        Long organizationId = currentOrganizationProvider.getOrganizationId();

        Pageable pageable = PageRequest.of(
                0,
                limit,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        List<SearchResultResponse> results = new ArrayList<>();

        if (canSearch(modules, SearchModule.CUSTOMER, PermissionName.CUSTOMER_VIEW)) {
            customerRepository.searchAccessibleCustomersInOrganization(
                    organizationId, query, null, null,
                    isAllAccess(context),
                    isTeamAccess(context),
                    context.userId(),
                    context.teamId(),
                    pageable
            ).forEach(customer -> results.add(
                    new SearchResultResponse(
                            SearchModule.CUSTOMER,
                            customer.getId(),
                            customer.getName(),
                            firstNonBlank(
                                    customer.getCompanyName(),
                                    customer.getEmail(),
                                    customer.getPhone()
                            ),
                            enumName(customer.getStatus()),
                            null,
                            null
                    )
            ));
        }

        if (canSearch(modules, SearchModule.LEAD, PermissionName.LEAD_VIEW)) {
            leadRepository.searchAccessibleLeadsInOrganization(
                    organizationId, query, null,
                    isAllAccess(context),
                    isTeamAccess(context),
                    context.userId(),
                    context.teamId(),
                    pageable
            ).forEach(lead -> results.add(
                    new SearchResultResponse(
                            SearchModule.LEAD,
                            lead.getId(),
                            lead.getFullName(),
                            firstNonBlank(
                                    lead.getCompanyName(),
                                    lead.getEmail(),
                                    lead.getPhone()
                            ),
                            enumName(lead.getStatus()),
                            null,
                            null
                    )
            ));
        }

        if (canSearch(modules, SearchModule.CONTACT, PermissionName.CONTACT_VIEW)) {
            contactRepository.searchAccessibleContactsInOrganization(
                    organizationId, null, query, null,
                    isAllAccess(context),
                    isTeamAccess(context),
                    context.userId(),
                    context.teamId(),
                    pageable
            ).forEach(contact -> results.add(
                    new SearchResultResponse(
                            SearchModule.CONTACT,
                            contact.getId(),
                            contact.getFullName(),
                            firstNonBlank(
                                    contact.getPosition(),
                                    contact.getEmail(),
                                    contact.getPhone()
                            ),
                            enumName(contact.getStatus()),
                            SearchModule.CUSTOMER,
                            contact.getCustomer().getId()
                    )
            ));
        }

        if (canSearch(modules, SearchModule.TASK, PermissionName.TASK_VIEW)) {
            taskRepository.searchAccessibleTasksInOrganization(
                    organizationId,
                    query, null, null, null, null, null,
                    isAllAccess(context),
                    isTeamAccess(context),
                    context.userId(),
                    context.teamId(),
                    pageable
            ).forEach(task -> {
                SearchModule parentModule = null;
                Long parentId = null;

                if (task.getCustomer() != null) {
                    parentModule = SearchModule.CUSTOMER;
                    parentId = task.getCustomer().getId();
                } else if (task.getLead() != null) {
                    parentModule = SearchModule.LEAD;
                    parentId = task.getLead().getId();
                }

                results.add(new SearchResultResponse(
                        SearchModule.TASK,
                        task.getId(),
                        task.getTitle(),
                        firstNonBlank(
                                safePreview(task.getDescription()),
                                enumName(task.getPriority())
                        ),
                        enumName(task.getStatus()),
                        parentModule,
                        parentId
                ));
            });
        }

        if (canSearch(modules, SearchModule.NOTE, PermissionName.NOTE_VIEW)) {
            noteRepository.searchAccessibleNotesInOrganization(
                    organizationId, query,
                    isAllAccess(context),
                    isTeamAccess(context),
                    context.userId(),
                    context.teamId(),
                    pageable
            ).forEach(note -> {
                SearchModule parentModule = note.getCustomer() != null
                        ? SearchModule.CUSTOMER
                        : SearchModule.LEAD;

                Long parentId = note.getCustomer() != null
                        ? note.getCustomer().getId()
                        : note.getLead().getId();

                String parentName = note.getCustomer() != null
                        ? note.getCustomer().getName()
                        : note.getLead().getFullName();

                results.add(new SearchResultResponse(
                        SearchModule.NOTE,
                        note.getId(),
                        "Note for " + parentName,
                        safePreview(note.getContent()),
                        null,
                        parentModule,
                        parentId
                ));
            });
        }

        return new GlobalSearchResponse(query, List.copyOf(results));
    }

    private String validateQuery(String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.trim();

        if (query.length() < 2 || query.length() > 100) {
            throw new IllegalArgumentException(
                    "Search query must contain between 2 and 100 characters"
            );
        }

        return query;
    }

    private int validateLimit(Integer requestedLimit) {
        int limit = requestedLimit == null ? 5 : requestedLimit;

        if (limit < 1 || limit > 10) {
            throw new IllegalArgumentException(
                    "Search limit must be between 1 and 10"
            );
        }

        return limit;
    }

    private Set<SearchModule> resolveModules(
            Set<SearchModule> requestedModules
    ) {
        if (requestedModules == null || requestedModules.isEmpty()) {
            return EnumSet.allOf(SearchModule.class);
        }

        return EnumSet.copyOf(requestedModules);
    }

    private boolean canSearch(
            Set<SearchModule> modules,
            SearchModule module,
            PermissionName permission
    ) {
        return modules.contains(module) && hasAuthority(permission);
    }

    private boolean hasAuthority(PermissionName permission) {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals(permission.name())
                );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    private String safePreview(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.replaceAll("\\s+", " ").trim();

        return normalized.length() <= 120
                ? normalized
                : normalized.substring(0, 117) + "...";
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private boolean isAllAccess(DataScopeContext context) {
        return context.scope() == DataScope.ALL;
    }

    private boolean isTeamAccess(DataScopeContext context) {
        return context.scope() == DataScope.TEAM;
    }
}
