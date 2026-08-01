package com.crm.backend.search;

import com.crm.backend.contact.ContactRepository;
import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.note.NoteRepository;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.task.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GlobalSearchServiceTest {

    private CustomerRepository customerRepository;
    private LeadRepository leadRepository;
    private ContactRepository contactRepository;
    private TaskRepository taskRepository;
    private NoteRepository noteRepository;
    private DataScopeService dataScopeService;
    private CurrentOrganizationProvider currentOrganizationProvider;
    private GlobalSearchService globalSearchService;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        leadRepository = mock(LeadRepository.class);
        contactRepository = mock(ContactRepository.class);
        taskRepository = mock(TaskRepository.class);
        noteRepository = mock(NoteRepository.class);
        dataScopeService = mock(DataScopeService.class);
        currentOrganizationProvider = mock(CurrentOrganizationProvider.class);
        when(currentOrganizationProvider.getOrganizationId()).thenReturn(1L);

        globalSearchService = new GlobalSearchService(
                customerRepository,
                leadRepository,
                contactRepository,
                taskRepository,
                noteRepository,
                dataScopeService,
                currentOrganizationProvider
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchShouldRejectShortQuery() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> globalSearchService.search("a", 5, null)
        );

        assertEquals(
                "Search query must contain between 2 and 100 characters",
                exception.getMessage()
        );

        verifyNoInteractions(
                customerRepository,
                leadRepository,
                contactRepository,
                taskRepository,
                noteRepository,
                dataScopeService
        );
    }

    @Test
    void searchShouldRejectInvalidLimit() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> globalSearchService.search("customer", 11, null)
        );

        assertEquals(
                "Search limit must be between 1 and 10",
                exception.getMessage()
        );

        verifyNoInteractions(dataScopeService);
    }

    @Test
    void searchShouldUsePermissionAndTeamScope() {
        authenticate("CUSTOMER_VIEW");

        DataScopeContext context =
                new DataScopeContext(1L, 7L, DataScope.TEAM);

        when(dataScopeService.currentContext()).thenReturn(context);

        Customer customer = new Customer();
        customer.setId(10L);
        customer.setName("Tadamun Company");
        customer.setCompanyName("Tadamun Business Solutions");
        customer.setStatus(CustomerStatus.ACTIVE);

        PageRequest pageable = PageRequest.of(
                0,
                5,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        when(customerRepository.searchAccessibleCustomersInOrganization(
                eq(1L),
                eq("tadamun"),
                isNull(),
                isNull(),
                eq(false),
                eq(true),
                eq(1L),
                eq(7L),
                eq(pageable)
        )).thenReturn(
                new PageImpl<>(
                        List.of(customer),
                        pageable,
                        1
                )
        );

        GlobalSearchResponse response = globalSearchService.search(
                " tadamun ",
                5,
                Set.of(
                        SearchModule.CUSTOMER,
                        SearchModule.LEAD
                )
        );

        assertEquals("tadamun", response.query());
        assertEquals(1, response.results().size());
        assertEquals(
                SearchModule.CUSTOMER,
                response.results().get(0).module()
        );
        assertEquals(
                "Tadamun Company",
                response.results().get(0).title()
        );

        verifyNoInteractions(
                leadRepository,
                contactRepository,
                taskRepository,
                noteRepository
        );
    }

    private void authenticate(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test-user",
                        "not-used",
                        AuthorityUtils.createAuthorityList(authorities)
                )
        );
    }
}
