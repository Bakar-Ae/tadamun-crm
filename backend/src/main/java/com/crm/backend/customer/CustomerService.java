package com.crm.backend.customer;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.customer.dto.CreateCustomerRequest;
import com.crm.backend.customer.dto.CustomerResponse;
import com.crm.backend.customer.dto.UpdateCustomerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final AuditLogService auditLogService;

    public CustomerService(
            CustomerRepository customerRepository,
            CustomerMapper customerMapper,
            AuditLogService auditLogService
    ) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request, Long actorUserId) {
        if (hasText(request.email()) && customerRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Customer email already exists");
        }

        Customer customer = new Customer();
        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setCompanyName(request.companyName());
        customer.setCustomerType(request.customerType());
        customer.setStatus(CustomerStatus.ACTIVE);

        Customer savedCustomer = customerRepository.save(customer);

        auditLogService.log(
                actorUserId,
                "CUSTOMER_CREATED",
                "CUSTOMER",
                savedCustomer.getId(),
                "{\"name\":\"" + savedCustomer.getName() + "\"}"
        );
        log.info("Customer created. customerId={}, actorUserId={}",
                savedCustomer.getId(), actorUserId);

        CustomerResponse response = customerMapper.toResponse(savedCustomer);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(String keyword, CustomerStatus status, CustomerType customerType, Pageable pageable) {
        return customerRepository.searchCustomers(keyword, status, customerType, pageable)
                .map(customerMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        return customerMapper.toResponse(findCustomerOrThrow(id));
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request, Long actorUserId) {
        Customer customer = findCustomerOrThrow(id);
        CustomerStatus previousStatus = customer.getStatus();

        if (hasText(request.email()) && customerRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new IllegalArgumentException("Customer email already exists");
        }

        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setCompanyName(request.companyName());
        customer.setCustomerType(request.customerType());
        customer.setStatus(request.status());

        Customer savedCustomer = customerRepository.save(customer);

        String action = previousStatus == CustomerStatus.ARCHIVED && savedCustomer.getStatus() == CustomerStatus.ACTIVE
                ? "CUSTOMER_RESTORED"
                : "CUSTOMER_UPDATED";

        auditLogService.log(
                actorUserId,
                action,
                "CUSTOMER",
                savedCustomer.getId(),
                "{\"name\":\"" + savedCustomer.getName() + "\",\"status\":\"" + savedCustomer.getStatus() + "\"}"
        );
        log.info("Customer updated. customerId={}, actorUserId={}, status={}",
                savedCustomer.getId(), actorUserId, savedCustomer.getStatus());


        return customerMapper.toResponse(savedCustomer);

    }

    @Transactional
    public CustomerResponse archiveCustomer(Long id, Long actorUserId) {
        Customer customer = findCustomerOrThrow(id);

        customer.setStatus(CustomerStatus.ARCHIVED);

        Customer savedCustomer = customerRepository.save(customer);

        auditLogService.log(
                actorUserId,
                "CUSTOMER_ARCHIVED",
                "CUSTOMER",
                savedCustomer.getId(),
                "{\"name\":\"" + savedCustomer.getName() + "\"}"
        );
        log.info("Customer archived. customerId={}, actorUserId={}",
                savedCustomer.getId(), actorUserId);

        return customerMapper.toResponse(savedCustomer);
    }


    private Customer findCustomerOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}