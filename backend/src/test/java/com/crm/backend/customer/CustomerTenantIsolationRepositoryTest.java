package com.crm.backend.customer;

import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.role.RoleRepository;
import com.crm.backend.support.MySqlTestContainerConfiguration;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(MySqlTestContainerConfiguration.class)
@Transactional
class CustomerTenantIsolationRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void customerQueriesShouldNotCrossOrganizationBoundary() {
        Role adminRole = roleRepository
                .findByName(RoleName.ADMIN)
                .orElseThrow();

        User owner = createUser(adminRole);

        Organization organizationA = createOrganization(
                "Phase 77 Organization A",
                "phase-77-organization-a",
                owner
        );
        Organization organizationB = createOrganization(
                "Phase 77 Organization B",
                "phase-77-organization-b",
                owner
        );

        Customer customerA = createCustomer(
                "Organization A Customer",
                "organization-a@crm.test",
                organizationA,
                owner
        );
        Customer customerB = createCustomer(
                "Organization B Customer",
                "organization-b@crm.test",
                organizationB,
                owner
        );

        customerRepository.flush();

        Page<Customer> organizationAResults =
                customerRepository
                        .searchAccessibleCustomersInOrganization(
                                organizationA.getId(),
                                "Organization",
                                null,
                                null,
                                true,
                                false,
                                owner.getId(),
                                null,
                                PageRequest.of(0, 10)
                        );

        assertEquals(1, organizationAResults.getTotalElements());
        assertEquals(
                customerA.getId(),
                organizationAResults.getContent().getFirst().getId()
        );

        assertTrue(
                customerRepository
                        .findAccessibleByIdInOrganization(
                                customerB.getId(),
                                organizationA.getId(),
                                true,
                                false,
                                owner.getId(),
                                null
                        )
                        .isEmpty()
        );

        assertFalse(
                customerRepository.existsByOrganizationIdAndEmail(
                        organizationA.getId(),
                        customerB.getEmail()
                )
        );
        assertTrue(
                customerRepository.existsByOrganizationIdAndEmail(
                        organizationB.getId(),
                        customerB.getEmail()
                )
        );
    }

    private User createUser(Role role) {
        User user = new User();
        user.setFullName("Phase 77 Owner");
        user.setEmail("phase77.owner@crm.test");
        user.setPasswordHash("integration-test-password-hash");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private Organization createOrganization(
            String name,
            String slug,
            User creator
    ) {
        Organization organization = new Organization();
        organization.setName(name);
        organization.setSlug(slug);
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setTimeZone("Africa/Mogadishu");
        organization.setCreatedByUser(creator);
        return organizationRepository.save(organization);
    }

    private Customer createCustomer(
            String name,
            String email,
            Organization organization,
            User owner
    ) {
        Customer customer = new Customer();
        customer.setOrganization(organization);
        customer.setName(name);
        customer.setEmail(email);
        customer.setCompanyName(name);
        customer.setCustomerType(CustomerType.COMPANY);
        customer.setOwnerUser(owner);
        customer.setStatus(CustomerStatus.ACTIVE);
        return customerRepository.save(customer);
    }
}
