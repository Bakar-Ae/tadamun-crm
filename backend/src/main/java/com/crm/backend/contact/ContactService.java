package com.crm.backend.contact;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.contact.dto.ContactResponse;
import com.crm.backend.contact.dto.CreateContactRequest;
import com.crm.backend.contact.dto.UpdateContactRequest;
import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final CustomerRepository customerRepository;
    private final ContactMapper contactMapper;
    private final AuditLogService auditLogService;

    public ContactService(
            ContactRepository contactRepository,
            CustomerRepository customerRepository,
            ContactMapper contactMapper,
            AuditLogService auditLogService
    ) {
        this.contactRepository = contactRepository;
        this.customerRepository = customerRepository;
        this.contactMapper = contactMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ContactResponse createContact(CreateContactRequest request, Long actorUserId) {
        Customer customer = findCustomerOrThrow(request.customerId());

        Contact contact = new Contact();
        contact.setCustomer(customer);
        contact.setFullName(request.fullName());
        contact.setEmail(request.email());
        contact.setPhone(request.phone());
        contact.setPosition(request.position());
        contact.setStatus(ContactStatus.ACTIVE);

        Contact savedContact = contactRepository.save(contact);

        auditLogService.log(
                actorUserId,
                "CONTACT_CREATED",
                "CONTACT",
                savedContact.getId(),
                "{\"name\":\"" + savedContact.getFullName() + "\",\"customerId\":" + savedContact.getCustomer().getId() + "}"
        );

        return contactMapper.toResponse(savedContact);
    }

    @Transactional(readOnly = true)
    public Page<ContactResponse> getContacts(Long customerId, String keyword, ContactStatus status, Pageable pageable) {
        return contactRepository.searchContacts(customerId, keyword, status, pageable)
                .map(contactMapper::toResponse);
    }
    @Transactional(readOnly = true)
    public ContactResponse getContactById(Long id) {
        return contactMapper.toResponse(findContactOrThrow(id));
    }

    @Transactional
    public ContactResponse updateContact(Long id, UpdateContactRequest request, Long actorUserId) {
        Contact contact = findContactOrThrow(id);
        ContactStatus previousStatus = contact.getStatus();

        contact.setFullName(request.fullName());
        contact.setEmail(request.email());
        contact.setPhone(request.phone());
        contact.setPosition(request.position());
        contact.setStatus(request.status());

        String action = previousStatus == ContactStatus.ARCHIVED && contact.getStatus() == ContactStatus.ACTIVE
                ? "CONTACT_RESTORED"
                : "CONTACT_UPDATED";

        auditLogService.log(
                actorUserId,
                action,
                "CONTACT",
                contact.getId(),
                "{\"name\":\"" + contact.getFullName() + "\",\"status\":\"" + contact.getStatus() + "\"}"
        );

        return contactMapper.toResponse(contact);
    }

    @Transactional
    public ContactResponse archiveContact(Long id, Long actorUserId) {
        Contact contact = findContactOrThrow(id);
        contact.setStatus(ContactStatus.ARCHIVED);

        auditLogService.log(
                actorUserId,
                "CONTACT_ARCHIVED",
                "CONTACT",
                contact.getId(),
                "{\"name\":\"" + contact.getFullName() + "\"}"
        );

        return contactMapper.toResponse(contact);
    }

    private Contact findContactOrThrow(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found"));
    }

    private Customer findCustomerOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }
}
