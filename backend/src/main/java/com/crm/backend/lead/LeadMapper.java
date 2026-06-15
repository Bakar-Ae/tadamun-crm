package com.crm.backend.lead;

import com.crm.backend.lead.dto.LeadResponse;
import org.springframework.stereotype.Component;

@Component
public class LeadMapper {

    public LeadResponse toResponse(Lead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getFullName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getCompanyName(),
                lead.getSource(),
                lead.getStatus(),
                lead.getEstimatedValue(),
                lead.getAssignedToUser() == null ? null : lead.getAssignedToUser().getId(),
                lead.getAssignedToUser() == null ? null : lead.getAssignedToUser().getFullName(),
                lead.getConvertedCustomer() == null ? null : lead.getConvertedCustomer().getId(),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }
}