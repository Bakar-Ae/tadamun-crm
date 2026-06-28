package com.crm.backend.attachment;

import com.crm.backend.security.CustomUserDetails;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping(
            value = "/customers/{customerId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize(
            "hasAuthority('ATTACHMENT_UPLOAD') and " +
                    "hasAuthority('CUSTOMER_VIEW')"
    )
    public ResponseEntity<AttachmentResponse> uploadToCustomer(
            @PathVariable Long customerId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                attachmentService.uploadToCustomer(
                        customerId,
                        file,
                        currentUser.getId()
                )
        );
    }

    @PostMapping(
            value = "/leads/{leadId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize(
            "hasAuthority('ATTACHMENT_UPLOAD') and " +
                    "hasAuthority('LEAD_VIEW')"
    )
    public ResponseEntity<AttachmentResponse> uploadToLead(
            @PathVariable Long leadId,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                attachmentService.uploadToLead(
                        leadId,
                        file,
                        currentUser.getId()
                )
        );
    }

    @GetMapping("/customers/{customerId}")
    @PreAuthorize(
            "hasAuthority('ATTACHMENT_VIEW') and " +
                    "hasAuthority('CUSTOMER_VIEW')"
    )
    public ResponseEntity<Page<AttachmentResponse>> getCustomerAttachments(
            @PathVariable Long customerId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                attachmentService.getCustomerAttachments(
                        customerId,
                        pageable
                )
        );
    }

    @GetMapping("/leads/{leadId}")
    @PreAuthorize(
            "hasAuthority('ATTACHMENT_VIEW') and " +
                    "hasAuthority('LEAD_VIEW')"
    )
    public ResponseEntity<Page<AttachmentResponse>> getLeadAttachments(
            @PathVariable Long leadId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                attachmentService.getLeadAttachments(
                        leadId,
                        pageable
                )
        );
    }

    @GetMapping("/{attachmentId}/download")
    @PreAuthorize("hasAuthority('ATTACHMENT_VIEW')")
    public ResponseEntity<Resource> download(
            @PathVariable Long attachmentId
    ) {
        AttachmentDownload download =
                attachmentService.download(attachmentId);

        ContentDisposition disposition = ContentDisposition
                .attachment()
                .filename(
                        download.fileName(),
                        StandardCharsets.UTF_8
                )
                .build();

        MediaType mediaType;

        try {
            mediaType = MediaType.parseMediaType(
                    download.contentType()
            );
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(download.sizeBytes())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .header("X-Content-Type-Options", "nosniff")
                .body(download.resource());
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAuthority('ATTACHMENT_DELETE')")
    public ResponseEntity<Void> delete(
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        attachmentService.delete(
                attachmentId,
                currentUser.getId()
        );

        return ResponseEntity.noContent().build();
    }
}