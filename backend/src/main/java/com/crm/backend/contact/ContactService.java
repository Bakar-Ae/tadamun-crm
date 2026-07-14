package com.crm.backend.contact;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.contact.dto.ContactResponse;
import com.crm.backend.contact.dto.CreateContactRequest;
import com.crm.backend.contact.dto.UpdateContactRequest;
import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
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
    private final DataScopeService dataScopeService;

    public ContactService(
            ContactRepository contactRepository,
            CustomerRepository customerRepository,
            ContactMapper contactMapper,
            AuditLogService auditLogService,
            DataScopeService dataScopeService
    ) {
        this.contactRepository = contactRepository;
        this.customerRepository = customerRepository;
        this.contactMapper = contactMapper;
        this.auditLogService = auditLogService;
        this.dataScopeService = dataScopeService;
    }

    @Transactional
    public ContactResponse createContact(CreateContactRequest request) {
        DataScopeContext context = dataScopeService.currentContext();
        Customer customer = findAccessibleCustomerOrThrow(request.customerId(), context);

        Contact contact = new Contact();
        contact.setCustomer(customer);
        contact.setFullName(request.fullName());
        contact.setEmail(request.email());
        contact.setPhone(request.phone());
        contact.setPosition(request.position());
        contact.setStatus(ContactStatus.ACTIVE);

        Contact savedContact = contactRepository.save(contact);

        auditLogService.log(
                context.userId(),
                "CONTACT_CREATED",
                "CONTACT",
                savedContact.getId(),
                "{\"name\":\"" + savedContact.getFullName() + "\",\"customerId\":" + savedContact.getCustomer().getId() + "}"
        );

        return contactMapper.toResponse(savedContact);
    }

    @Transactional(readOnly = true)
    public Page<ContactResponse> getContacts(Long customerId, String keyword, ContactStatus status, Pageable pageable) {
        DataScopeContext context = dataScopeService.currentContext();
        return contactRepository.searchAccessibleContacts(
                        customerId,
                        keyword,
                        status,
                        isAllAccess(context),
                        isTeamAccess(context),
                        context.userId(),
                        context.teamId(),
                        pageable
                )
                .map(contactMapper::toResponse);
    }
    @Transactional(readOnly = true)
    public ContactResponse getContactById(Long id) {
        DataScopeContext context = dataScopeService.currentContext();
        return contactMapper.toResponse(findAccessibleContactOrThrow(id, context));
    }

    @Transactional
    public ContactResponse updateContact(Long id, UpdateContactRequest request) {
        DataScopeContext context = dataScopeService.currentContext();
        Contact contact = findAccessibleContactOrThrow(id, context);
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
                context.userId(),
                action,
                "CONTACT",
                contact.getId(),
                "{\"name\":\"" + contact.getFullName() + "\",\"status\":\"" + contact.getStatus() + "\"}"
        );

        return contactMapper.toResponse(contact);
    }

    @Transactional
    public ContactResponse archiveContact(Long id) {
        DataScopeContext context = dataScopeService.currentContext();
        Contact contact = findAccessibleContactOrThrow(id, context);
        contact.setStatus(ContactStatus.ARCHIVED);

        auditLogService.log(
                context.userId(),
                "CONTACT_ARCHIVED",
                "CONTACT",
                contact.getId(),
                "{\"name\":\"" + contact.getFullName() + "\"}"
        );

        return contactMapper.toResponse(contact);
    }

    private Contact findAccessibleContactOrThrow(Long id, DataScopeContext context) {
        return contactRepository.findAccessibleById(
                        id,
                        isAllAccess(context),
                        isTeamAccess(context),
                        context.userId(),
                        context.teamId()
                )
                .orElseThrow(() -> new IllegalArgumentException("Contact not found"));
    }

    private Customer findAccessibleCustomerOrThrow(Long id, DataScopeContext context) {
        return customerRepository.findAccessibleById(
                        id,
                        isAllAccess(context),
                        isTeamAccess(context),
                        context.userId(),
                        context.teamId()
                )
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    private boolean isAllAccess(DataScopeContext context) {
        return context.scope() == DataScope.ALL;
    }

    private boolean isTeamAccess(DataScopeContext context) {
        return context.scope() == DataScope.TEAM;
    }
}
