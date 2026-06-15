package com.crm.backend.contact.dto;

import com.crm.backend.contact.ContactStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateContactRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 150)
        String fullName,

        @Email(message = "Email must be valid")
        @Size(max = 150)
        String email,

        @Size(max = 50)
        String phone,

        @Size(max = 100)
        String position,

        @NotNull(message = "Status is required")
        ContactStatus status
) {
}