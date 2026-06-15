package com.crm.backend.contact.dto;

import com.crm.backend.contact.ContactStatus;

import java.time.LocalDateTime;

public record ContactResponse(
        Long id,
        Long customerId,
        String customerName,
        String fullName,
        String email,
        String phone,
        String position,
        ContactStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}