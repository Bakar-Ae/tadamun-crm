package com.crm.backend.organization.invitation;

import com.crm.backend.organization.invitation.dto.CreateOrganizationInvitationRequest;
import com.crm.backend.organization.invitation.dto.OrganizationInvitationResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organization-invitations")
public class OrganizationInvitationController {

    private final OrganizationInvitationService invitationService;

    public OrganizationInvitationController(
            OrganizationInvitationService invitationService
    ) {
        this.invitationService = invitationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MEMBERSHIP_INVITE')")
    public ResponseEntity<OrganizationInvitationResponse> createInvitation(
            @Valid @RequestBody CreateOrganizationInvitationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationService.createInvitation(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MEMBERSHIP_VIEW')")
    public ResponseEntity<Page<OrganizationInvitationResponse>> getInvitations(
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                invitationService.getInvitations(pageable)
        );
    }

    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('MEMBERSHIP_UPDATE')")
    public ResponseEntity<OrganizationInvitationResponse> revokeInvitation(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                invitationService.revokeInvitation(id)
        );
    }
}
