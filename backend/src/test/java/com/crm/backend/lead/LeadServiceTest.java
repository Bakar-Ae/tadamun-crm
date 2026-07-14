package com.crm.backend.lead;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.lead.dto.CreateLeadRequest;
import com.crm.backend.lead.dto.LeadResponse;
import com.crm.backend.role.DataScope;
import com.crm.backend.security.DataScopeContext;
import com.crm.backend.security.DataScopeService;
import com.crm.backend.team.Team;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class LeadServiceTest {

    private LeadRepository leadRepository;
    private CustomerRepository customerRepository;
    private UserRepository userRepository;
    private LeadMapper leadMapper;
    private DataScopeService dataScopeService;
    private LeadService leadService;

    @BeforeEach
    void setUp() {
        leadRepository = mock(LeadRepository.class);
        customerRepository = mock(CustomerRepository.class);
        userRepository = mock(UserRepository.class);
        leadMapper = mock(LeadMapper.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        dataScopeService = spy(new DataScopeService());

        leadService = new LeadService(
                leadRepository,
                customerRepository,
                userRepository,
                leadMapper,
                auditLogService,
                dataScopeService
        );
    }

    @Test
    void createLeadShouldAssignCurrentUserWhenAssigneeIsMissing() {
        DataScopeContext context = context(1L, 10L, DataScope.OWN);
        User currentUser = activeUser(1L, 10L);
        CreateLeadRequest request = request(null);

        doReturn(context).when(dataScopeService).currentContext();
        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));
        when(leadRepository.save(any(Lead.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(leadMapper.toResponse(any(Lead.class)))
                .thenAnswer(invocation -> {
                    Lead lead = invocation.getArgument(0);
                    assertSame(currentUser, lead.getAssignedToUser());
                    return response(LeadStatus.NEW, 1L);
                });

        LeadResponse result = leadService.createLead(request);

        assertEquals(1L, result.assignedToUserId());
    }

    @Test
    void ownScopeShouldRejectAssigningLeadToAnotherUser() {
        DataScopeContext context = context(1L, 10L, DataScope.OWN);
        User otherUser = activeUser(2L, 10L);

        doReturn(context).when(dataScopeService).currentContext();
        when(userRepository.findById(2L)).thenReturn(Optional.of(otherUser));

        assertThrows(
                AccessDeniedException.class,
                () -> leadService.createLead(request(2L))
        );
    }

    @Test
    void getLeadByIdShouldHideMissingOrInaccessibleLead() {
        DataScopeContext context = context(1L, 10L, DataScope.TEAM);

        doReturn(context).when(dataScopeService).currentContext();
        when(leadRepository.findAccessibleById(
                99L,
                false,
                true,
                1L,
                10L
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> leadService.getLeadById(99L)
        );

        assertEquals("Lead not found", exception.getMessage());
    }

    @Test
    void convertLeadShouldPreserveAssigneeAsCustomerOwner() {
        DataScopeContext context = context(1L, 10L, DataScope.ALL);
        User assignee = activeUser(2L, 20L);
        Lead lead = new Lead();
        lead.setId(5L);
        lead.setFullName("Example Lead");
        lead.setEmail("lead@example.com");
        lead.setCompanyName("Example Company");
        lead.setStatus(LeadStatus.QUALIFIED);
        lead.setAssignedToUser(assignee);

        doReturn(context).when(dataScopeService).currentContext();
        when(leadRepository.findAccessibleById(5L, true, false, 1L, 10L))
                .thenReturn(Optional.of(lead));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> {
                    Customer customer = invocation.getArgument(0);
                    customer.setId(50L);
                    assertSame(assignee, customer.getOwnerUser());
                    return customer;
                });
        when(leadMapper.toResponse(lead))
                .thenReturn(response(LeadStatus.CONVERTED, 2L));

        LeadResponse result = leadService.convertLead(5L);

        assertEquals(LeadStatus.CONVERTED, result.status());
        assertEquals(50L, lead.getConvertedCustomer().getId());
    }

    private CreateLeadRequest request(Long assignedToUserId) {
        return new CreateLeadRequest(
                "Example Lead",
                "lead@example.com",
                "+252610000000",
                "Example Company",
                "Website",
                BigDecimal.valueOf(1000),
                assignedToUserId
        );
    }

    private LeadResponse response(LeadStatus status, Long assignedToUserId) {
        return new LeadResponse(
                5L,
                "Example Lead",
                "lead@example.com",
                "+252610000000",
                "Example Company",
                "Website",
                status,
                BigDecimal.valueOf(1000),
                assignedToUserId,
                "Assigned User",
                status == LeadStatus.CONVERTED ? 50L : null,
                null,
                null
        );
    }

    private User activeUser(Long userId, Long teamId) {
        Team team = new Team();
        team.setId(teamId);

        User user = new User();
        user.setId(userId);
        user.setFullName("Assigned User");
        user.setStatus(UserStatus.ACTIVE);
        user.setTeam(team);
        return user;
    }

    private DataScopeContext context(
            Long userId,
            Long teamId,
            DataScope scope
    ) {
        return new DataScopeContext(userId, teamId, scope);
    }
}
