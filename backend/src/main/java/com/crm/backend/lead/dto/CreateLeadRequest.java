package com.crm.backend.lead.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateLeadRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 150)
        String fullName,

        @Email(message = "Email must be valid")
        @Size(max = 150)
        String email,

        @Size(max = 50)
        String phone,

        @Size(max = 150)
        String companyName,

        @Size(max = 100)
        String source,

        BigDecimal estimatedValue,

        Long assignedToUserId
) {
}