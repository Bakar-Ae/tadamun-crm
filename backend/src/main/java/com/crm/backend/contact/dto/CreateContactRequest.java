package com.crm.backend.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateContactRequest(
        @NotNull(message = "Customer id is required")
        Long customerId,

        @NotBlank(message = "Full name is required")
        @Size(max = 150)
        String fullName,

        @Email(message = "Email must be valid")
        @Size(max = 150)
        String email,

        @Size(max = 50)
        String phone,

        @Size(max = 100)
        String position
) {
}