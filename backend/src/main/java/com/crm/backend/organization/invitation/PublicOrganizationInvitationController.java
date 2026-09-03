package com.crm.backend.organization.invitation;

import com.crm.backend.organization.invitation.dto.AcceptOrganizationInvitationRequest;
import com.crm.backend.organization.invitation.dto.OrganizationInvitationAcceptanceResponse;
import com.crm.backend.organization.invitation.dto.OrganizationInvitationPreviewResponse;
import com.crm.backend.organization.invitation.dto.OrganizationInvitationTokenRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/organization-invitations")
public class PublicOrganizationInvitationController {

    private final OrganizationInvitationService invitationService;

    public PublicOrganizationInvitationController(
            OrganizationInvitationService invitationService
    ) {
        this.invitationService = invitationService;
    }

    @PostMapping("/preview")
    public ResponseEntity<OrganizationInvitationPreviewResponse>
    previewInvitation(
            @Valid @RequestBody OrganizationInvitationTokenRequest request
    ) {
        return ResponseEntity.ok(
                invitationService.previewInvitation(request.token())
        );
    }

    @PostMapping("/accept")
    public ResponseEntity<OrganizationInvitationAcceptanceResponse>
    acceptInvitation(
            @Valid @RequestBody AcceptOrganizationInvitationRequest request
    ) {
        return ResponseEntity.ok(
                invitationService.acceptInvitation(request)
        );
    }
}