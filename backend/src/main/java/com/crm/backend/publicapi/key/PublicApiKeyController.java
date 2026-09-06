package com.crm.backend.publicapi.key;

import com.crm.backend.publicapi.key.dto.CreatePublicApiKeyRequest;
import com.crm.backend.publicapi.key.dto.CreatedPublicApiKeyResponse;
import com.crm.backend.publicapi.key.dto.PublicApiKeyResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/v1/public-api-keys")
@PreAuthorize("hasAuthority('SUBSCRIPTION_MANAGE')")
public class PublicApiKeyController {

    private final PublicApiKeyService apiKeyService;

    public PublicApiKeyController(PublicApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    public ResponseEntity<Page<PublicApiKeyResponse>> getKeys(
            @PageableDefault(size = 20, sort = "createdAt", direction = DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(apiKeyService.getKeys(pageable));
    }

    @PostMapping
    public ResponseEntity<CreatedPublicApiKeyResponse> createKey(
            @Valid @RequestBody CreatePublicApiKeyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                apiKeyService.createKey(request, userDetails.getId())
        );
    }

    @PostMapping("/{id}/rotate")
    public ResponseEntity<CreatedPublicApiKeyResponse> rotateKey(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                apiKeyService.rotateKey(id, userDetails.getId())
        );
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<PublicApiKeyResponse> revokeKey(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                apiKeyService.revokeKey(id, userDetails.getId())
        );
    }
}
