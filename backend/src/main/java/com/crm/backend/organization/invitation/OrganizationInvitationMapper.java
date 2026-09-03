package com.crm.backend.organization.invitation;

import com.crm.backend.organization.invitation.dto.OrganizationInvitationResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrganizationInvitationMapper {

    public OrganizationInvitationResponse toResponse(
            OrganizationInvitation invitation
    ) {
        OrganizationInvitationStatus effectiveStatus = invitation.getStatus();

        if (effectiveStatus == OrganizationInvitationStatus.PENDING
                && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            effectiveStatus = OrganizationInvitationStatus.EXPIRED;
        }

        return new OrganizationInvitationResponse(
                invitation.getId(),
                invitation.getOrganization().getId(),
                invitation.getOrganization().getName(),
                invitation.getEmail(),
                invitation.getRole().getName(),
                effectiveStatus,
                invitation.getInvitedByUser().getId(),
                invitation.getInvitedByUser().getFullName(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                invitation.getRevokedAt(),
                invitation.getCreatedAt()
        );
    }
}
