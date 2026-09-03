package com.crm.backend.organization;

import com.crm.backend.organization.dto.OrganizationResponse;
import com.crm.backend.organization.dto.UpdateOrganizationRequest;
import com.crm.backend.organization.membership.OrganizationMembershipService;
import com.crm.backend.organization.membership.dto.DeactivateOrganizationMembershipRequest;
import com.crm.backend.organization.membership.dto.OrganizationMembershipResponse;
import com.crm.backend.organization.membership.dto.UpdateOrganizationMembershipRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organization")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationMembershipService membershipService;

    public OrganizationController(
            OrganizationService organizationService,
            OrganizationMembershipService membershipService
    ) {
        this.organizationService = organizationService;
        this.membershipService = membershipService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_VIEW')")
    public ResponseEntity<OrganizationResponse> getOrganization() {
        return ResponseEntity.ok(
                organizationService.getCurrentOrganization()
        );
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ORGANIZATION_UPDATE')")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        return ResponseEntity.ok(
                organizationService.updateCurrentOrganization(request)
        );
    }

    @GetMapping("/members")
    @PreAuthorize("hasAuthority('MEMBERSHIP_VIEW')")
    public ResponseEntity<Page<OrganizationMembershipResponse>> getMembers(
            @PageableDefault(sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(
                membershipService.getCurrentOrganizationMembers(pageable)
        );
    }

    @PatchMapping("/members/{membershipId}")
    @PreAuthorize("hasAuthority('MEMBERSHIP_UPDATE')")
    public ResponseEntity<OrganizationMembershipResponse> updateMemberRole(
            @PathVariable Long membershipId,
            @Valid @RequestBody UpdateOrganizationMembershipRequest request
    ) {
        return ResponseEntity.ok(
                membershipService.updateMembershipRole(membershipId, request)
        );
    }

    @PatchMapping("/members/{membershipId}/deactivate")
    @PreAuthorize("hasAuthority('MEMBERSHIP_DEACTIVATE')")
    public ResponseEntity<OrganizationMembershipResponse> deactivateMember(
            @PathVariable Long membershipId,
            @Valid @RequestBody DeactivateOrganizationMembershipRequest request
    ) {
        return ResponseEntity.ok(
                membershipService.deactivateMembership(membershipId, request)
        );
    }
}
