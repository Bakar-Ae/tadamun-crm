package com.crm.backend.search;

import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.customer.CustomerType;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.role.RoleRepository;
import com.crm.backend.support.MySqlTestContainerConfiguration;
import com.crm.backend.team.Team;
import com.crm.backend.team.TeamRepository;
import com.crm.backend.team.TeamStatus;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(MySqlTestContainerConfiguration.class)
@Transactional
class GlobalSearchRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Test
    void customerSearchShouldEnforceOwnTeamAndAllScopes() {
        Role role = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        Team salesTeam = createTeam("Phase 67 Sales");
        Team supportTeam = createTeam("Phase 67 Support");

        User owner = createUser("phase67.owner@crm.test", role, salesTeam);
        User teammate = createUser("phase67.teammate@crm.test", role, salesTeam);
        User outsider = createUser("phase67.outsider@crm.test", role, supportTeam);

        createCustomer("Phase67Scope Owner", owner);
        createCustomer("Phase67Scope Teammate", teammate);
        createCustomer("Phase67Scope Outsider", outsider);

        assertEquals(
                Set.of("Phase67Scope Owner"),
                searchCustomerNames(false, false, owner.getId(), salesTeam.getId())
        );
        assertEquals(
                Set.of("Phase67Scope Owner", "Phase67Scope Teammate"),
                searchCustomerNames(false, true, owner.getId(), salesTeam.getId())
        );
        assertEquals(
                Set.of(
                        "Phase67Scope Owner",
                        "Phase67Scope Teammate",
                        "Phase67Scope Outsider"
                ),
                searchCustomerNames(true, false, owner.getId(), salesTeam.getId())
        );
    }

    private Team createTeam(String name) {
        Team team = new Team();
        team.setName(name);
        team.setDescription("Integration test team");
        team.setStatus(TeamStatus.ACTIVE);
        return teamRepository.save(team);
    }

    private User createUser(String email, Role role, Team team) {
        User user = new User();
        user.setFullName(email);
        user.setEmail(email);
        user.setPasswordHash("integration-test-password-hash");
        user.setRole(role);
        user.setTeam(team);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private void createCustomer(String name, User owner) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(name.toLowerCase().replace(' ', '.') + "@crm.test");
        customer.setCompanyName(name);
        customer.setCustomerType(CustomerType.COMPANY);
        customer.setOwnerUser(owner);
        customer.setStatus(CustomerStatus.ACTIVE);
        customerRepository.save(customer);
    }

    private Set<String> searchCustomerNames(
            boolean allAccess,
            boolean teamAccess,
            Long userId,
            Long teamId
    ) {
        Page<Customer> result = customerRepository.searchAccessibleCustomers(
                "phase67scope",
                null,
                null,
                allAccess,
                teamAccess,
                userId,
                teamId,
                PageRequest.of(0, 10)
        );

        return result.getContent().stream()
                .map(Customer::getName)
                .collect(Collectors.toSet());
    }
}
