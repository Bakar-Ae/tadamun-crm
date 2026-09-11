package com.crm.backend.integration;

import com.crm.backend.integration.dto.CreateIntegrationConnectionRequest;
import com.crm.backend.integration.dto.IntegrationConnectionResponse;
import com.crm.backend.integration.dto.UpdateIntegrationConnectionRequest;
import com.crm.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationConnectionController {

    private final IntegrationConnectionService connectionService;

    public IntegrationConnectionController(
            IntegrationConnectionService connectionService
    ) {
        this.connectionService = connectionService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INTEGRATION_VIEW')")
    public ResponseEntity<Page<IntegrationConnectionResponse>> getConnections(
            @PageableDefault(size = 20, sort = "createdAt", direction = DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(connectionService.getConnections(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INTEGRATION_VIEW')")
    public ResponseEntity<IntegrationConnectionResponse> getConnection(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(connectionService.getConnection(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    public ResponseEntity<IntegrationConnectionResponse> createConnection(
            @Valid @RequestBody CreateIntegrationConnectionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                connectionService.createConnection(request, userDetails.getId())
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    public ResponseEntity<IntegrationConnectionResponse> updateConnection(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIntegrationConnectionRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(connectionService.updateConnection(
                id,
                request,
                userDetails.getId()
        ));
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    public ResponseEntity<IntegrationConnectionResponse> verifyConnection(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(connectionService.verifyConnection(
                id,
                userDetails.getId()
        ));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    public ResponseEntity<IntegrationConnectionResponse> disableConnection(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(connectionService.disableConnection(
                id,
                userDetails.getId()
        ));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    public ResponseEntity<IntegrationConnectionResponse> revokeConnection(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(connectionService.revokeConnection(
                id,
                userDetails.getId()
        ));
    }
}
