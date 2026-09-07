package com.crm.backend.customer;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.customer.dto.CreateCustomerRequest;
import com.crm.backend.customer.dto.CustomerResponse;
import com.crm.backend.customer.dto.UpdateCustomerRequest;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import com.crm.backend.webhook.WebhookDomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final DataScopeService dataScopeService;
    private final CurrentOrganizationProvider currentOrganizationProvider;
    private final WebhookDomainEventPublisher webhookEventPublisher;

    public CustomerService(
            CustomerRepository customerRepository,
            CustomerMapper customerMapper,
            AuditLogService auditLogService,
            UserRepository userRepository,
            DataScopeService dataScopeService,
            CurrentOrganizationProvider currentOrganizationProvider,
            WebhookDomainEventPublisher webhookEventPublisher
    ) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
        this.dataScopeService = dataScopeService;
        this.currentOrganizationProvider = currentOrganizationProvider;
        this.webhookEventPublisher = webhookEventPublisher;
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        DataScopeContext context = dataScopeService.currentContext();
        Long organizationId =
                currentOrganizationProvider.getOrganizationId();

        if (hasText(request.email()) && customerRepository.existsByOrganizationIdAndEmail(
                organizationId,
                request.email()
        )) {
            throw new IllegalArgumentException("Customer email already exists");
        }

        User owner = resolveOwner(request.ownerUserId(), context);

        Customer customer = new Customer();
        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setCompanyName(request.companyName());
        customer.setCustomerType(request.customerType());
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setOwnerUser(owner);
        customer.setOrganization(
                currentOrganizationProvider.getOrganizationReference()
        );

        Customer savedCustomer = customerRepository.save(customer);

        auditLogService.log(
                context.userId(),
                "CUSTOMER_CREATED",
                "CUSTOMER",
                savedCustomer.getId(),
                "{\"name\":\"" + savedCustomer.getName() + "\"}"
        );
        log.info("Customer created. customerId={}, actorUserId={}",
                savedCustomer.getId(), context.userId());
        webhookEventPublisher.customerCreated(savedCustomer);

        return customerMapper.toResponse(savedCustomer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(String keyword, CustomerStatus status, CustomerType customerType, Pageable pageable) {
        DataScopeContext context = dataScopeService.currentContext();

        return customerRepository.searchAccessibleCustomersInOrganization(
                        currentOrganizationProvider.getOrganizationId(),
                        keyword,
                        status,
                        customerType,
                        context.scope() == DataScope.ALL,
                        context.scope() == DataScope.TEAM,
                        context.userId(),
                        context.teamId(),
                        pageable
                )
                .map(customerMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        DataScopeContext context = dataScopeService.currentContext();
        return customerMapper.toResponse(findAccessibleCustomerOrThrow(id, context));
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        DataScopeContext context = dataScopeService.currentContext();
        Customer customer = findAccessibleCustomerOrThrow(id, context);
        CustomerStatus previousStatus = customer.getStatus();
        Map<String, Object> previousValues = webhookState(customer);

        if (hasText(request.email())
                && customerRepository.existsByOrganizationIdAndEmailAndIdNot(
                currentOrganizationProvider.getOrganizationId(),
                request.email(),
                id
        )) {
            throw new IllegalArgumentException(
                    "Customer email already exists"
            );
        }

        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setCompanyName(request.companyName());
        customer.setCustomerType(request.customerType());
        customer.setStatus(request.status());

        if (request.ownerUserId() != null || customer.getOwnerUser() == null) {
            customer.setOwnerUser(resolveOwner(request.ownerUserId(), context));
        }

        Customer savedCustomer = customerRepository.save(customer);

        String action = previousStatus == CustomerStatus.ARCHIVED && savedCustomer.getStatus() == CustomerStatus.ACTIVE
                ? "CUSTOMER_RESTORED"
                : "CUSTOMER_UPDATED";

        auditLogService.log(
                context.userId(),
                action,
                "CUSTOMER",
                savedCustomer.getId(),
                "{\"name\":\"" + savedCustomer.getName() + "\",\"status\":\"" + savedCustomer.getStatus() + "\"}"
        );
        log.info("Customer updated. customerId={}, actorUserId={}, status={}",
                savedCustomer.getId(), context.userId(), savedCustomer.getStatus());
        if (previousStatus == CustomerStatus.ARCHIVED
                && savedCustomer.getStatus() == CustomerStatus.ACTIVE) {
            webhookEventPublisher.customerRestored(savedCustomer);
        } else {
            webhookEventPublisher.customerUpdated(
                    savedCustomer,
                    changedFields(previousValues, webhookState(savedCustomer))
            );
        }

        return customerMapper.toResponse(savedCustomer);

    }

    @Transactional
    public CustomerResponse archiveCustomer(Long id) {
        DataScopeContext context = dataScopeService.currentContext();
        Customer customer = findAccessibleCustomerOrThrow(id, context);

        customer.setStatus(CustomerStatus.ARCHIVED);

        Customer savedCustomer = customerRepository.save(customer);

        auditLogService.log(
                context.userId(),
                "CUSTOMER_ARCHIVED",
                "CUSTOMER",
                savedCustomer.getId(),
                "{\"name\":\"" + savedCustomer.getName() + "\"}"
        );
        log.info("Customer archived. customerId={}, actorUserId={}",
                savedCustomer.getId(), context.userId());
        webhookEventPublisher.customerArchived(savedCustomer);

        return customerMapper.toResponse(savedCustomer);
    }


    private Customer findAccessibleCustomerOrThrow(Long id, DataScopeContext context) {
        return customerRepository.findAccessibleByIdInOrganization(
                        id,
                        currentOrganizationProvider.getOrganizationId(),
                        context.scope() == DataScope.ALL,
                        context.scope() == DataScope.TEAM,
                        context.userId(),
                        context.teamId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer not found")
                );
    }

    private User resolveOwner(Long requestedOwnerUserId, DataScopeContext context) {
        Long ownerUserId = requestedOwnerUserId == null
                ? context.userId()
                : requestedOwnerUserId;

        User owner = userRepository.findById(ownerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Customer owner not found"));

        currentOrganizationProvider.requireActiveUserMembership(owner.getId());

        if (owner.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Customer owner must be active");
        }

        Long ownerTeamId = owner.getTeam() == null
                ? null
                : owner.getTeam().getId();

        dataScopeService.requireAccess(context, owner.getId(), ownerTeamId);
        return owner;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Map<String, Object> webhookState(Customer customer) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("name", customer.getName());
        values.put("companyName", customer.getCompanyName());
        values.put("customerType", customer.getCustomerType());
        values.put("status", customer.getStatus());
        values.put("ownerUserId", customer.getOwnerUser() == null
                ? null
                : customer.getOwnerUser().getId());
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
