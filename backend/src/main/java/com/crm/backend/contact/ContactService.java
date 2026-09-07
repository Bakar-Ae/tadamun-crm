package com.crm.backend.contact;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.contact.dto.ContactResponse;
import com.crm.backend.contact.dto.CreateContactRequest;
import com.crm.backend.contact.dto.UpdateContactRequest;
import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.webhook.WebhookDomainEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ContactService {

    private final ContactRepository contactRepository;
    private final CustomerRepository customerRepository;
    private final ContactMapper contactMapper;
    private final AuditLogService auditLogService;
    private final DataScopeService dataScopeService;
    private final CurrentOrganizationProvider currentOrganizationProvider;
    private final WebhookDomainEventPublisher webhookEventPublisher;

    public ContactService(
            ContactRepository contactRepository,
            CustomerRepository customerRepository,
            ContactMapper contactMapper,
            AuditLogService auditLogService,
            DataScopeService dataScopeService,
            CurrentOrganizationProvider currentOrganizationProvider,
            WebhookDomainEventPublisher webhookEventPublisher
    ) {
        this.contactRepository = contactRepository;
        this.customerRepository = customerRepository;
        this.contactMapper = contactMapper;
        this.auditLogService = auditLogService;
        this.dataScopeService = dataScopeService;
        this.currentOrganizationProvider = currentOrganizationProvider;
        this.webhookEventPublisher = webhookEventPublisher;
    }

    @Transactional
    public ContactResponse createContact(CreateContactRequest request) {
        DataScopeContext context = dataScopeService.currentContext();
        Customer customer = findAccessibleCustomerOrThrow(request.customerId(), context);

        Contact contact = new Contact();
        contact.setOrganization(
                currentOrganizationProvider.getOrganizationReference()
        );
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
        webhookEventPublisher.contactCreated(savedContact);

        return contactMapper.toResponse(savedContact);
    }

    @Transactional(readOnly = true)
    public Page<ContactResponse> getContacts(Long customerId, String keyword, ContactStatus status, Pageable pageable) {
        DataScopeContext context = dataScopeService.currentContext();
        return contactRepository.searchAccessibleContactsInOrganization(
                        currentOrganizationProvider.getOrganizationId(),
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
        Map<String, Object> previousValues = webhookState(contact);

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
        webhookEventPublisher.contactUpdated(
                contact,
                changedFields(previousValues, webhookState(contact))
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
        webhookEventPublisher.contactUpdated(contact, List.of("status"));

        return contactMapper.toResponse(contact);
    }

    private Contact findAccessibleContactOrThrow(Long id, DataScopeContext context) {
        return contactRepository.findAccessibleByIdInOrganization(
                        id,
                        currentOrganizationProvider.getOrganizationId(),
                        isAllAccess(context),
                        isTeamAccess(context),
                        context.userId(),
                        context.teamId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Contact not found"));
    }

    private Customer findAccessibleCustomerOrThrow(Long id, DataScopeContext context) {
        return customerRepository.findAccessibleByIdInOrganization(
                        id,
                        currentOrganizationProvider.getOrganizationId(),
                        isAllAccess(context),
                        isTeamAccess(context),
                        context.userId(),
                        context.teamId()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private boolean isAllAccess(DataScopeContext context) {
        return context.scope() == DataScope.ALL;
    }

    private boolean isTeamAccess(DataScopeContext context) {
        return context.scope() == DataScope.TEAM;
    }

    private Map<String, Object> webhookState(Contact contact) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("fullName", contact.getFullName());
        values.put("position", contact.getPosition());
        values.put("status", contact.getStatus());
        return values;
    }

    private List<String> changedFields(
            Map<String, Object> before,
            Map<String, Object> after
    ) {
        return before.keySet().stream()
                .filter(key -> !Objects.equals(before.get(key), after.get(key)))
                .toList();
    }
}
