package com.crm.backend.attachment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttachmentRepository
        extends JpaRepository<Attachment, Long> {

    Page<Attachment> findByCustomerIdAndStatusOrderByCreatedAtDesc(
            Long customerId,
            AttachmentStatus status,
            Pageable pageable
    );

    Page<Attachment> findByLeadIdAndStatusOrderByCreatedAtDesc(
            Long leadId,
            AttachmentStatus status,
            Pageable pageable
    );

    Optional<Attachment> findByIdAndStatus(
            Long id,
            AttachmentStatus status
    );
}