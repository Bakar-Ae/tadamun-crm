package com.crm.backend.attachment;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AttachmentServiceTest {

    private AttachmentRepository attachmentRepository;
    private LocalAttachmentStorageService storageService;
    private DataScopeService dataScopeService;
    private AttachmentService attachmentService;

    @BeforeEach
    void setUp() {
        attachmentRepository = mock(AttachmentRepository.class);
        storageService = mock(LocalAttachmentStorageService.class);
        dataScopeService = mock(DataScopeService.class);
        attachmentService = new AttachmentService(
                attachmentRepository,
                storageService,
                mock(CustomerRepository.class),
                mock(LeadRepository.class),
                mock(UserRepository.class),
                mock(AuditLogService.class),
                mock(ObjectMapper.class),
                dataScopeService
        );
    }

    @Test
    void downloadShouldHideAttachmentOutsideCurrentScope() {
        DataScopeContext context =
                new DataScopeContext(4L, 2L, DataScope.OWN);
        when(dataScopeService.currentContext()).thenReturn(context);
        when(attachmentRepository.findAccessibleByIdAndStatus(
                22L,
                AttachmentStatus.ACTIVE,
                false,
                false,
                4L,
                2L
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> attachmentService.download(22L)
        );

        assertEquals("Attachment not found", exception.getMessage());
        verifyNoInteractions(storageService);
    }
}
