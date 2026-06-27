package com.crm.backend.customer;

import com.crm.backend.customer.dto.CreateCustomerRequest;
import com.crm.backend.customer.dto.CustomerResponse;
import com.crm.backend.customer.dto.UpdateCustomerRequest;
import com.crm.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE')")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerService.createCustomer(request, currentUser.getId()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<Page<CustomerResponse>> getCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) CustomerType customerType,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                customerService.getCustomers(
                        keyword,
                        status,
                        customerType,
                        pageable
                )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_VIEW')")
    public ResponseEntity<CustomerResponse> getCustomerById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCustomerRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                customerService.updateCustomer(
                        id,
                        request,
                        currentUser.getId()
                )
        );
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('CUSTOMER_ARCHIVE')")
    public ResponseEntity<CustomerResponse> archiveCustomer(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                customerService.archiveCustomer(
                        id,
                        currentUser.getId()
                )
        );
    }
}