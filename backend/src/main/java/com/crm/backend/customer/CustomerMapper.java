package com.crm.backend.customer;

import com.crm.backend.customer.dto.CustomerResponse;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
        var owner = customer.getOwnerUser();
        var team = owner == null ? null : owner.getTeam();

        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getCompanyName(),
                customer.getCustomerType(),
                customer.getStatus(),
                owner == null ? null : owner.getId(),
                owner == null ? null : owner.getFullName(),
                team == null ? null : team.getId(),
                team == null ? null : team.getName(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
