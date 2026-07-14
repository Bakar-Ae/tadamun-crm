package com.crm.backend.customer;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.customer.dto.CreateCustomerRequest;
import com.crm.backend.customer.dto.CustomerResponse;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.team.Team;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerServiceTest {

    private CustomerRepository customerRepository;
    private CustomerMapper customerMapper;
    private AuditLogService auditLogService;
    private UserRepository userRepository;
    private DataScopeService dataScopeService;
    private CustomerService customerService;
    private DataScopeContext context;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        customerMapper = mock(CustomerMapper.class);
        auditLogService = mock(AuditLogService.class);
        userRepository = mock(UserRepository.class);
        dataScopeService = mock(DataScopeService.class);

        customerService = new CustomerService(
                customerRepository,
                customerMapper,
                auditLogService,
                userRepository,
                dataScopeService
        );

        context = new DataScopeContext(1L, 10L, DataScope.ALL);
        when(dataScopeService.currentContext()).thenReturn(context);
    }

    @Test
    void createCustomerShouldRejectDuplicateEmail() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Test Company",
                "test@company.com",
                "+252610000000",
                "Mogadishu",
                CustomerType.COMPANY,
                null
        );

        when(customerRepository.existsByEmail("test@company.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.createCustomer(request)
        );

        assertEquals("Customer email already exists", exception.getMessage());
    }

    @Test
    void createCustomerShouldAssignCurrentUserAsOwner() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Test Company",
                "owner-test@company.com",
                "+252610000000",
                "Mogadishu",
                CustomerType.COMPANY,
                null
        );
        User owner = activeUser(1L, 10L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(customerMapper.toResponse(any(Customer.class)))
                .thenAnswer(invocation -> {
                    Customer customer = invocation.getArgument(0);
                    assertSame(owner, customer.getOwnerUser());
                    return response(CustomerStatus.ACTIVE);
                });

        CustomerResponse result = customerService.createCustomer(request);

        assertEquals(CustomerStatus.ACTIVE, result.status());
    }

    @Test
    void archiveCustomerShouldChangeStatusToArchived() {
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("Test Company");
        customer.setEmail("test@company.com");
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setOwnerUser(activeUser(1L, 10L));

        when(customerRepository.findAccessibleById(1L, true, false, 1L, 10L))
                .thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(customerMapper.toResponse(any(Customer.class)))
                .thenReturn(response(CustomerStatus.ARCHIVED));

        CustomerResponse result = customerService.archiveCustomer(1L);

        assertEquals(CustomerStatus.ARCHIVED, result.status());
    }

    @Test
    void getCustomerByIdShouldRejectMissingOrInaccessibleCustomer() {
        when(customerRepository.findAccessibleById(99L, true, false, 1L, 10L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> customerService.getCustomerById(99L)
        );

        assertEquals("Customer not found", exception.getMessage());
    }

    private User activeUser(Long userId, Long teamId) {
        Team team = new Team();
        team.setId(teamId);

        User user = new User();
        user.setId(userId);
        user.setFullName("Test Owner");
        user.setStatus(UserStatus.ACTIVE);
        user.setTeam(team);
        return user;
    }

    private CustomerResponse response(CustomerStatus status) {
        return new CustomerResponse(
                1L,
                "Test Company",
                "test@company.com",
                "+252610000000",
                "Mogadishu",
                CustomerType.COMPANY,
                status,
                1L,
                "Test Owner",
                10L,
                "Sales",
                null,
                null
        );
    }
}
