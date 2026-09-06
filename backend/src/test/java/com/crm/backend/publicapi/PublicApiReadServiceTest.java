package com.crm.backend.publicapi;

import com.crm.backend.customer.CustomerMapper;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.LeadMapper;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.publicapi.key.PublicApiScope;
import com.crm.backend.publicapi.security.PublicApiContextHolder;
import com.crm.backend.publicapi.security.PublicApiPrincipal;
import com.crm.backend.publicapi.security.PublicApiScopeGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicApiReadServiceTest {

    private CustomerRepository customerRepository;
    private LeadRepository leadRepository;
    private PublicApiScopeGuard scopeGuard;
    private PublicApiReadService readService;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        leadRepository = mock(LeadRepository.class);
        scopeGuard = mock(PublicApiScopeGuard.class);
        readService = new PublicApiReadService(
                customerRepository,
                mock(CustomerMapper.class),
                leadRepository,
                mock(LeadMapper.class),
                scopeGuard
        );
        PublicApiContextHolder.set(new PublicApiPrincipal(
                5L,
                42L,
                "Integration",
                Set.of(PublicApiScope.CUSTOMERS_READ),
                60
        ));
    }

    @AfterEach
    void tearDown() {
        PublicApiContextHolder.clear();
    }

    @Test
    void customerQueryShouldAlwaysUseAuthenticatedOrganization() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(customerRepository.searchAccessibleCustomersInOrganization(
                eq(42L),
                eq("acme"),
                isNull(),
                isNull(),
                eq(true),
                eq(false),
                isNull(),
                isNull(),
                eq(pageable)
        )).thenReturn(Page.empty(pageable));

        readService.getCustomers(" acme ", null, null, pageable);

        verify(scopeGuard).require(PublicApiScope.CUSTOMERS_READ);
        verify(customerRepository)
                .searchAccessibleCustomersInOrganization(
                        eq(42L),
                        eq("acme"),
                        isNull(),
                        isNull(),
                        eq(true),
                        eq(false),
                        isNull(),
                        isNull(),
                        eq(pageable)
                );
    }

    @Test
    void oversizedPageShouldBeRejectedBeforeRepositoryQuery() {
        PageRequest pageable = PageRequest.of(0, 101);

        assertThrows(
                IllegalArgumentException.class,
                () -> readService.getLeads(null, null, pageable)
        );
        verify(scopeGuard).require(PublicApiScope.LEADS_READ);
        verify(leadRepository, never())
                .searchAccessibleLeadsInOrganization(
                        any(),
                        any(),
                        any(),
                        eq(true),
                        eq(false),
                        any(),
                        any(),
                        any()
                );
    }
}
