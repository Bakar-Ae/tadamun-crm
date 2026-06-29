package com.crm.backend.attachment;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.Lead;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final LocalAttachmentStorageService storageService;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AttachmentService(
            AttachmentRepository attachmentRepository,
            LocalAttachmentStorageService storageService,
            CustomerRepository customerRepository,
            LeadRepository leadRepository,
            UserRepository userRepository,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.attachmentRepository = attachmentRepository;
        this.storageService = storageService;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AttachmentResponse uploadToCustomer(
            Long customerId,
            MultipartFile file,
            Long actorUserId
    ) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Customer not found")
                );

        return storeAttachment(
                file,
                customer,
                null,
                findUser(actorUserId)
        );
    }

    @Transactional
    public AttachmentResponse uploadToLead(
            Long leadId,
            MultipartFile file,
            Long actorUserId
    ) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Lead not found")
                );

        return storeAttachment(
                file,
                null,
                lead,
                findUser(actorUserId)
        );
    }

    @Transactional(readOnly = true)
    public Page<AttachmentResponse> getCustomerAttachments(
            Long customerId,
            Pageable pageable
    ) {
        if (!customerRepository.existsById(customerId)) {
            throw new IllegalArgumentException("Customer not found");
        }

        return attachmentRepository
                .findByCustomerIdAndStatusOrderByCreatedAtDesc(
                        customerId,
                        AttachmentStatus.ACTIVE,
                        pageable
                )
                .map(AttachmentResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<AttachmentResponse> getLeadAttachments(
            Long leadId,
            Pageable pageable
    ) {
        if (!leadRepository.existsById(leadId)) {
            throw new IllegalArgumentException("Lead not found");
        }

        return attachmentRepository
                .findByLeadIdAndStatusOrderByCreatedAtDesc(
                        leadId,
                        AttachmentStatus.ACTIVE,
                        pageable
                )
                .map(AttachmentResponse::from);
    }

    @Transactional(readOnly = true)
    public AttachmentDownload download(Long attachmentId) {
        Attachment attachment = findActiveAttachment(attachmentId);

        return new AttachmentDownload(
                storageService.load(attachment.getStorageKey()),
                attachment.getOriginalFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes()
        );
    }

    @Transactional
    public void delete(Long attachmentId, Long actorUserId) {
        Attachment attachment = findActiveAttachment(attachmentId);
        User actor = findUser(actorUserId);

        attachment.setStatus(AttachmentStatus.DELETED);
        attachment.setDeletedAt(LocalDateTime.now());
        attachment.setDeletedByUser(actor);

        auditLogService.log(
                actorUserId,
                "ATTACHMENT_DELETED",
                "ATTACHMENT",
                attachment.getId(),
                auditDetails(attachment)
        );
    }

    private AttachmentResponse storeAttachment(
            MultipartFile file,
            Customer customer,
            Lead lead,
            User actor
    ) {
        String originalFileName = cleanOriginalFileName(
                file.getOriginalFilename()
        );

        StoredFile storedFile = storageService.store(file);

        try {
            Attachment attachment = new Attachment();
            attachment.setOriginalFileName(originalFileName);
            attachment.setStorageKey(storedFile.storageKey());
            attachment.setContentType(storedFile.contentType());
            attachment.setSizeBytes(storedFile.sizeBytes());
            attachment.setChecksumSha256(storedFile.checksumSha256());
            attachment.setCustomer(customer);
            attachment.setLead(lead);
            attachment.setUploadedByUser(actor);
            attachment.setStatus(AttachmentStatus.ACTIVE);

            Attachment saved = attachmentRepository.saveAndFlush(attachment);

            auditLogService.log(
                    actor.getId(),
                    "ATTACHMENT_UPLOADED",
                    "ATTACHMENT",
                    saved.getId(),
                    auditDetails(saved)
            );

            return AttachmentResponse.from(saved);
        } catch (RuntimeException exception) {
            storageService.delete(storedFile.storageKey());
            throw exception;
        }
    }

    private Attachment findActiveAttachment(Long id) {
        return attachmentRepository
                .findByIdAndStatus(id, AttachmentStatus.ACTIVE)
                .orElseThrow(() ->
                        new IllegalArgumentException("Attachment not found")
                );
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );
    }

    private String cleanOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException(
                    "Attachment file name is required"
            );
        }

        String normalized = originalFileName.replace('\\', '/');
        String fileName = normalized.substring(
                normalized.lastIndexOf('/') + 1
        ).trim();

        if (fileName.isBlank() || fileName.length() > 255) {
            throw new IllegalArgumentException(
                    "Attachment file name is invalid"
            );
        }

        if (fileName.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException(
                    "Attachment file name is invalid"
            );
        }

        return fileName;
    }

    private String auditDetails(Attachment attachment) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "fileName", attachment.getOriginalFileName(),
                    "contentType", attachment.getContentType(),
                    "sizeBytes", attachment.getSizeBytes()
            ));
        } catch (JacksonException exception) {
            return "{}";
        }
    }
}
