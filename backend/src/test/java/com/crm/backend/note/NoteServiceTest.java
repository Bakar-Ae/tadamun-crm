package com.crm.backend.note;

import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.note.dto.CreateNoteRequest;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
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
    private NoteService noteService;

    @BeforeEach
    void setUp() {
        leadRepository = mock(LeadRepository.class);
        dataScopeService = mock(DataScopeService.class);
        noteService = new NoteService(
                mock(NoteRepository.class),
                mock(CustomerRepository.class),
                leadRepository,
                mock(UserRepository.class),
                mock(NoteMapper.class),
                dataScopeService
        );
    }

    @Test
    void createNoteShouldRejectLeadOutsideCurrentScope() {
        DataScopeContext context =
                new DataScopeContext(4L, 2L, DataScope.TEAM);
        when(dataScopeService.currentContext()).thenReturn(context);
        when(leadRepository.findAccessibleById(
                15L,
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
