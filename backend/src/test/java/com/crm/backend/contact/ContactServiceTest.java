package com.crm.backend.contact;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.contact.dto.CreateContactRequest;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactServiceTest {

    private CustomerRepository customerRepository;
    private DataScopeService dataScopeService;
    private CurrentOrganizationProvider currentOrganizationProvider;
    private ContactService contactService;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        dataScopeService = mock(DataScopeService.class);
        currentOrganizationProvider =
                mock(CurrentOrganizationProvider.class);
        when(currentOrganizationProvider.getOrganizationId())
                .thenReturn(1L);

        contactService = new ContactService(
                mock(ContactRepository.class),
                customerRepository,
                mock(ContactMapper.class),
                mock(AuditLogService.class),
                dataScopeService,
                currentOrganizationProvider
        );
    }

    @Test
    void createContactShouldRejectCustomerOutsideCurrentScope() {
        DataScopeContext context =
                new DataScopeContext(4L, 2L, DataScope.OWN);
        when(dataScopeService.currentContext()).thenReturn(context);
        when(customerRepository.findAccessibleByIdInOrganization(
                9L,
                1L,
                false,
                false,
                4L,
                2L
        )).thenReturn(Optional.empty());

        CreateContactRequest request = new CreateContactRequest(
                9L,
                "Hidden Contact",
                null,
                null,
                null
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> contactService.createContact(request)
        );

        assertEquals("Customer not found", exception.getMessage());
        verify(customerRepository).findAccessibleByIdInOrganization(
                9L,
                1L,
                false,
                false,
                4L,
                2L
        );
    }
}
