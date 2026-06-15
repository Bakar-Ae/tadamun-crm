package com.crm.backend.customer.dto;

import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.customer.CustomerType;

import java.time.LocalDateTime;

public record CustomerResponse(
        Long id,
        String name,
        String email,
        String phone,
        String companyName,
        CustomerType customerType,
        CustomerStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}