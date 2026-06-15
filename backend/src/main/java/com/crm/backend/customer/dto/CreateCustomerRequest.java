package com.crm.backend.customer.dto;

import com.crm.backend.customer.CustomerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @NotBlank(message = "Customer name is required")
        @Size(max = 150)
        String name,

        @Email(message = "Email must be valid")
        @Size(max = 150)
        String email,

        @Size(max = 50)
        String phone,

        @Size(max = 150)
        String companyName,

        @NotNull(message = "Customer type is required")
        CustomerType customerType
) {
}