package com.crm.backend.note;

import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.note.dto.CreateNoteRequest;
import com.crm.backend.organization.Organization;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NoteServiceTest {

    private LeadRepository leadRepository;
    private DataScopeService dataScopeService;
    private CurrentOrganizationProvider currentOrganizationProvider;
    private NoteService noteService;

    @BeforeEach
    void setUp() {
        leadRepository = mock(LeadRepository.class);
        dataScopeService = mock(DataScopeService.class);
        currentOrganizationProvider =
                mock(CurrentOrganizationProvider.class);
        Organization organization = new Organization();
        organization.setId(1L);
        when(currentOrganizationProvider.getOrganizationId())
                .thenReturn(1L);
        when(currentOrganizationProvider.getOrganizationReference())
                .thenReturn(organization);

        noteService = new NoteService(
                mock(NoteRepository.class),
                mock(CustomerRepository.class),
                leadRepository,
                mock(UserRepository.class),
                mock(NoteMapper.class),
                dataScopeService,
                currentOrganizationProvider
        );
    }

    @Test
    void createNoteShouldRejectLeadOutsideCurrentScope() {
        DataScopeContext context =
                new DataScopeContext(4L, 2L, DataScope.TEAM);
        when(dataScopeService.currentContext()).thenReturn(context);
        when(leadRepository.findAccessibleByIdInOrganization(
                15L,
                1L,
                false,
                true,
                4L,
                2L
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> noteService.createNote(
                        new CreateNoteRequest("Private note", null, 15L)
                )
        );

        assertEquals("Lead not found", exception.getMessage());
    }
}
