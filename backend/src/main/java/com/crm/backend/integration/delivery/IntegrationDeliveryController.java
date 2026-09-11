package com.crm.backend.integration.delivery;

import com.crm.backend.integration.delivery.dto.IntegrationDeliveryAttemptResponse;
import com.crm.backend.integration.delivery.dto.IntegrationDeliveryResponse;
import com.crm.backend.integration.delivery.dto.QueueIntegrationDeliveryRequest;
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

import java.util.List;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/v1/integrations")
public class IntegrationDeliveryController {

    private final IntegrationDeliveryService deliveryService;

    public IntegrationDeliveryController(
            IntegrationDeliveryService deliveryService
    ) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/{connectionId}/deliveries")
    @PreAuthorize("hasAuthority('INTEGRATION_VIEW')")
    public Page<IntegrationDeliveryResponse> getDeliveries(
            @PathVariable Long connectionId,
            @PageableDefault(size = 20, sort = "createdAt", direction = DESC)
            Pageable pageable
    ) {
        return deliveryService.getDeliveries(connectionId, pageable);
    }

    @PostMapping("/{connectionId}/deliveries")
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    public ResponseEntity<IntegrationDeliveryResponse> queue(
            @PathVariable Long connectionId,
            @Valid @RequestBody QueueIntegrationDeliveryRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                deliveryService.queue(
                        connectionId,
                        request,
                        userDetails.getId()
                )
        );
    }

    @GetMapping("/deliveries/{publicDeliveryId}")
    @PreAuthorize("hasAuthority('INTEGRATION_VIEW')")
    public IntegrationDeliveryResponse getDelivery(
            @PathVariable String publicDeliveryId
    ) {
        return deliveryService.getDelivery(publicDeliveryId);
    }

    @GetMapping("/deliveries/{publicDeliveryId}/attempts")
    @PreAuthorize("hasAuthority('INTEGRATION_VIEW')")
    public List<IntegrationDeliveryAttemptResponse> getAttempts(
            @PathVariable String publicDeliveryId
    ) {
        return deliveryService.getAttempts(publicDeliveryId);
    }

    @PostMapping("/deliveries/{publicDeliveryId}/retry")
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    public IntegrationDeliveryResponse retry(
            @PathVariable String publicDeliveryId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return deliveryService.retry(publicDeliveryId, userDetails.getId());
    }

    @PostMapping("/deliveries/{publicDeliveryId}/cancel")
    @PreAuthorize("hasAuthority('INTEGRATION_MANAGE')")
    public IntegrationDeliveryResponse cancel(
            @PathVariable String publicDeliveryId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return deliveryService.cancel(publicDeliveryId, userDetails.getId());
    }
}
