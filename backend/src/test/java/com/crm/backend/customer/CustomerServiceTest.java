package com.crm.backend.customer;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.customer.dto.CreateCustomerRequest;
import com.crm.backend.customer.dto.CustomerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerServiceTest {

    private CustomerRepository customerRepository;
    private CustomerMapper customerMapper;
    private AuditLogService auditLogService;
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        customerMapper = mock(CustomerMapper.class);
        auditLogService = mock(AuditLogService.class);

        customerService = new CustomerService(
                customerRepository,
                customerMapper,
                auditLogService
        );
    }

    @Test
    void createCustomerShouldRejectDuplicateEmail() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Test Company",
                "test@company.com",
                "+252610000000",
                "Mogadishu",
                CustomerType.COMPANY
        );

        when(customerRepository.existsByEmail("test@company.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.createCustomer(request, 1L)
        );

        assertEquals("Customer email already exists", exception.getMessage());
    }

    @Test
    void archiveCustomerShouldChangeStatusToArchived() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Test Company");
        customer.setEmail("test@company.com");
        customer.setStatus(CustomerStatus.ACTIVE);

        CustomerResponse response = new CustomerResponse(
                1L,
                "Test Company",
                "test@company.com",
                "+252610000000",
                "Mogadishu",
                CustomerType.COMPANY,
                CustomerStatus.ARCHIVED,
                null,
                null
        );

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(customerMapper.toResponse(any(Customer.class))).thenReturn(response);

        CustomerResponse result = customerService.archiveCustomer(1L, 1L);

        assertEquals(CustomerStatus.ARCHIVED, result.status());
    }

    @Test
    void getCustomerByIdShouldRejectMissingCustomer() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.getCustomerById(99L)
        );

        assertEquals("Customer not found", exception.getMessage());
    }
}