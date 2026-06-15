package com.crm.backend.contact;

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

    public ContactService(
            ContactRepository contactRepository,
            CustomerRepository customerRepository,
            ContactMapper contactMapper
    ) {
        this.contactRepository = contactRepository;
        this.customerRepository = customerRepository;
        this.contactMapper = contactMapper;
    }

    @Transactional
    public ContactResponse createContact(CreateContactRequest request) {
        Customer customer = findCustomerOrThrow(request.customerId());

        Contact contact = new Contact();
        contact.setCustomer(customer);
        contact.setFullName(request.fullName());
        contact.setEmail(request.email());
        contact.setPhone(request.phone());
        contact.setPosition(request.position());
        contact.setStatus(ContactStatus.ACTIVE);

        return contactMapper.toResponse(contactRepository.save(contact));
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
    public ContactResponse updateContact(Long id, UpdateContactRequest request) {
        Contact contact = findContactOrThrow(id);

        contact.setFullName(request.fullName());
        contact.setEmail(request.email());
        contact.setPhone(request.phone());
        contact.setPosition(request.position());
        contact.setStatus(request.status());

        return contactMapper.toResponse(contact);
    }

    @Transactional
    public ContactResponse archiveContact(Long id) {
        Contact contact = findContactOrThrow(id);
        contact.setStatus(ContactStatus.ARCHIVED);
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