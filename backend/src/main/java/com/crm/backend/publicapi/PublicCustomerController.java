package com.crm.backend.publicapi;

import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.customer.CustomerType;
import com.crm.backend.customer.dto.CustomerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/v1/customers")
public class PublicCustomerController {

    private final PublicApiReadService readService;

    public PublicCustomerController(PublicApiReadService readService) {
        this.readService = readService;
    }

    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> getCustomers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) CustomerType customerType,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(readService.getCustomers(
                keyword,
                status,
                customerType,
                pageable
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(readService.getCustomer(id));
    }
}
