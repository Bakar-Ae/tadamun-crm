package com.crm.backend.user;

import com.crm.backend.organization.membership.OrganizationMembership;
import com.crm.backend.user.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(OrganizationMembership membership) {
        User user = membership.getUser();
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                membership.getRole().getName(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getName(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
