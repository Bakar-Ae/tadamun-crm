package com.crm.backend.contact;

import com.crm.backend.contact.dto.ContactResponse;
import org.springframework.stereotype.Component;

@Component
public class ContactMapper {

    public ContactResponse toResponse(Contact contact) {
        return new ContactResponse(
                contact.getId(),
                contact.getCustomer().getId(),
                contact.getCustomer().getName(),
                contact.getFullName(),
                contact.getEmail(),
                contact.getPhone(),
                contact.getPosition(),
                contact.getStatus(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }
}