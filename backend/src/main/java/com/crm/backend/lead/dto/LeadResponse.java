package com.crm.backend.lead.dto;

import com.crm.backend.lead.LeadStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LeadResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String companyName,
        String source,
        LeadStatus status,
        BigDecimal estimatedValue,
        Long assignedToUserId,
        String assignedToUserName,
        Long convertedCustomerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}