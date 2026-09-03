package com.crm.backend.platform;

import com.crm.backend.security.PlatformAuthorities;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform")
@PreAuthorize("hasAuthority('" + PlatformAuthorities.ADMIN + "')")
public class PlatformAdministratorController {

    @GetMapping("/me")
    public ResponseEntity<PlatformAdministratorResponse> me(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                new PlatformAdministratorResponse(
                        authentication.getName(),
                        true
                )
        );
    }
}
