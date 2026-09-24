package com.crm.backend.security.tenant;

import com.crm.backend.customer.Customer;
import com.crm.backend.customer.CustomerRepository;
import com.crm.backend.customer.CustomerStatus;
import com.crm.backend.customer.CustomerType;
import com.crm.backend.lead.Lead;
import com.crm.backend.lead.LeadRepository;
import com.crm.backend.lead.LeadStatus;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.organization.membership.OrganizationMembership;
import com.crm.backend.organization.membership.OrganizationMembershipRepository;
import com.crm.backend.organization.membership.OrganizationMembershipStatus;
import com.crm.backend.role.RoleName;
import com.crm.backend.role.RoleRepository;
import com.crm.backend.security.CustomUserDetails;
import com.crm.backend.security.JwtService;
import com.crm.backend.support.MySqlTestContainerConfiguration;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.tenant.enforcement-enabled=true")
@AutoConfigureMockMvc
@Import(MySqlTestContainerConfiguration.class)
@Transactional
class TenantApiIsolationIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private OrganizationRepository organizations;
    @Autowired private OrganizationMembershipRepository memberships;
    @Autowired private CustomerRepository customers;
    @Autowired private LeadRepository leads;

    private User ownerA;
    private User ownerB;
    private Organization companyA;
    private Organization companyB;
    private OrganizationMembership membershipA;
    private OrganizationMembership membershipB;
    private Customer customerA;
    private Customer customerB;
    private Lead leadA;
    private Lead leadB;

    @BeforeEach
    void setUpCompanies() {
        ownerA = createUser("a");
        ownerB = createUser("b");
        companyA = createCompany("a", ownerA);
        companyB = createCompany("b", ownerB);
        membershipA = createMembership(companyA, ownerA, RoleName.OWNER);
        membershipB = createMembership(companyB, ownerB, RoleName.OWNER);
        customerA = createCustomer(companyA, ownerA, "A");
        customerB = createCustomer(companyB, ownerB, "B");
        leadA = createLead(companyA, ownerA, "A");
        leadB = createLead(companyB, ownerB, "B");
    }

    @AfterEach
    void clearRequestContext() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void customerLeadAndMembershipListsShouldStayWithinEachCompany() throws Exception {
        assertLists(ownerA, companyA, customerA, leadA, membershipA);
        assertLists(ownerB, companyB, customerB, leadB, membershipB);
    }

    @Test
    void crossCompanyCustomerAndLeadIdsShouldRejectReadsAndWrites() throws Exception {
        mvc.perform(as(ownerA, companyA, get("/api/v1/customers/{id}", customerB.getId())))
                .andExpect(status().isNotFound());
        mvc.perform(as(ownerA, companyA, put("/api/v1/customers/{id}", customerB.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Changed","customerType":"COMPANY","status":"ACTIVE"}
                                """))
                .andExpect(status().isNotFound());
        mvc.perform(as(ownerA, companyA, patch("/api/v1/customers/{id}/archive", customerB.getId())))
                .andExpect(status().isNotFound());
        mvc.perform(as(ownerA, companyA, get("/api/v1/leads/{id}", leadB.getId())))
                .andExpect(status().isNotFound());
        mvc.perform(as(ownerA, companyA, put("/api/v1/leads/{id}", leadB.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Changed","status":"NEW"}
                                """))
                .andExpect(status().isNotFound());
        mvc.perform(as(ownerA, companyA, patch("/api/v1/leads/{id}/convert", leadB.getId())))
                .andExpect(status().isNotFound());
        assertEquals("Company B Customer", customers.findById(customerB.getId()).orElseThrow().getName());
        assertEquals(CustomerStatus.ACTIVE, customerB.getStatus());
        assertEquals("Company B Lead", leads.findById(leadB.getId()).orElseThrow().getFullName());
        assertNull(leadB.getConvertedCustomer());
    }

    @Test
    void organizationHeaderMustNotGrantNonMemberAccess() throws Exception {
        mvc.perform(as(ownerA, companyB, get("/api/v1/customers")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_ACCESS_DENIED"));
    }

    @Test
    void multiCompanyUserMustSelectACompanyAndUseItsMembershipPermissions() throws Exception {
        createMembership(companyB, ownerA, RoleName.SALES_REP);
        mvc.perform(authenticate(ownerA, get("/api/v1/customers")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TENANT_CONTEXT_REQUIRED"));
        mvc.perform(as(ownerA, companyA, get("/api/v1/organization/members")))
                .andExpect(status().isOk());
        mvc.perform(as(ownerA, companyB, get("/api/v1/organization/members")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivatedMembershipMustRejectAnExistingToken() throws Exception {
        String token = jwtService.generateToken(new CustomUserDetails(ownerA));
        membershipA.setStatus(OrganizationMembershipStatus.INACTIVE);
        memberships.saveAndFlush(membershipA);
        mvc.perform(get("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header(TenantResolutionFilter.ORGANIZATION_HEADER, companyA.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_ACCESS_DENIED"));
    }

    @Test
    void membershipChangesMustNotReachAnotherCompany() throws Exception {
        mvc.perform(as(ownerA, companyA,
                        patch("/api/v1/organization/members/{id}", membershipB.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SALES_REP\",\"version\":" + membershipB.getVersion() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Organization membership not found"));
        assertEquals(RoleName.OWNER, membershipB.getRole().getName());
    }

    @Test
    void legacyUserDirectoryMustNotExposeOtherCompanies() throws Exception {
        mvc.perform(as(ownerA, companyA, get("/api/v1/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ownerA.getId()));
        mvc.perform(as(ownerA, companyA, get("/api/v1/users/{id}", ownerB.getId())))
                .andExpect(status().isNotFound());
        mvc.perform(as(ownerB, companyB, get("/api/v1/users/{id}", ownerA.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void directoryFiltersAndResponsesMustUseTheCurrentMembershipRole() throws Exception {
        mvc.perform(as(ownerA, companyA, get("/api/v1/users")
                        .param("role", "OWNER").param("keyword", ownerA.getEmail())
                        .param("sort", "createdAt,desc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].role").value("OWNER"));
        mvc.perform(as(ownerA, companyA, get("/api/v1/users").param("keyword", ownerB.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(as(ownerA, companyA, get("/api/v1/users/{id}", ownerA.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void directorySortingAndPaginationMustUseUserFields() throws Exception {
        User newestUser = createUser("c");
        newestUser.setFullName("Alphabetical First");
        users.saveAndFlush(newestUser);
        createMembership(companyA, newestUser, RoleName.SALES_REP);
        createMembership(companyA, ownerB, RoleName.SALES_REP);

        mvc.perform(as(ownerA, companyA, get("/api/v1/users")
                        .param("sort", "id,desc").param("size", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].id").value(newestUser.getId()));
        mvc.perform(as(ownerA, companyA, get("/api/v1/users")
                        .param("sort", "fullName,asc").param("size", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(newestUser.getId()));
    }

    @Test
    void directoryMustNotIncludeDeactivatedMemberships() throws Exception {
        OrganizationMembership inactive = createMembership(companyA, ownerB, RoleName.SALES_REP);
        inactive.setStatus(OrganizationMembershipStatus.INACTIVE);
        memberships.saveAndFlush(inactive);
        mvc.perform(as(ownerA, companyA, get("/api/v1/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(as(ownerA, companyA, get("/api/v1/users/{id}", ownerB.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void companyOwnerMustNotCreateOrEditGlobalAccounts() throws Exception {
        mvc.perform(as(ownerA, companyA, post("/api/v1/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Blocked User","email":"blocked@example.test",
                                 "password":"UnusedTestPassword123!","role":"ADMIN"}
                                """))
                .andExpect(status().isForbidden());
        mvc.perform(as(ownerA, companyA, put("/api/v1/users/{id}", ownerB.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Changed","email":"changed@example.test",
                                 "role":"SALES_REP","status":"INACTIVE"}
                                """))
                .andExpect(status().isForbidden());
        assertEquals("isolation-b@example.test", ownerB.getEmail());
        assertEquals(RoleName.ADMIN, ownerB.getRole().getName());
    }

    @Test
    void companyOwnerMustNotDeactivateAnotherCompanysGlobalAccount() throws Exception {
        mvc.perform(as(ownerA, companyA, patch("/api/v1/users/{id}/deactivate", ownerB.getId())))
                .andExpect(status().isForbidden());
        assertEquals(UserStatus.ACTIVE, users.findById(ownerB.getId()).orElseThrow().getStatus());
    }

    private void assertLists(User actor, Organization company, Customer customer,
                             Lead lead, OrganizationMembership membership) throws Exception {
        mvc.perform(as(actor, company, get("/api/v1/customers")))
                .andExpect(status().isOk())
                .andExpect(header().string(TenantResolutionFilter.ORGANIZATION_HEADER, company.getId().toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(customer.getId()));
        mvc.perform(as(actor, company, get("/api/v1/leads")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(lead.getId()));
        mvc.perform(as(actor, company, get("/api/v1/organization/members")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(membership.getId()));
    }

    private MockHttpServletRequestBuilder as(User actor, Organization company,
                                             MockHttpServletRequestBuilder request) {
        return authenticate(actor, request)
                .header(TenantResolutionFilter.ORGANIZATION_HEADER, company.getId());
    }

    private MockHttpServletRequestBuilder authenticate(User actor, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.generateToken(new CustomUserDetails(actor)));
    }

    private User createUser(String suffix) {
        User user = new User();
        user.setFullName("Isolation Owner " + suffix);
        user.setEmail("isolation-" + suffix + "@example.test");
        user.setPasswordHash("unused-isolation-test-password-hash");
        user.setRole(roles.findByName(RoleName.ADMIN).orElseThrow());
        user.setStatus(UserStatus.ACTIVE);
        return users.saveAndFlush(user);
    }

    private Organization createCompany(String suffix, User owner) {
        Organization company = new Organization();
        company.setName("Isolation Company " + suffix);
        company.setSlug("isolation-company-" + suffix);
        company.setStatus(OrganizationStatus.ACTIVE);
        company.setTimeZone("Africa/Mogadishu");
        company.setCreatedByUser(owner);
        return organizations.saveAndFlush(company);
    }

    private OrganizationMembership createMembership(Organization company, User user, RoleName role) {
        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(company);
        membership.setUser(user);
        membership.setRole(roles.findByName(role).orElseThrow());
        membership.setStatus(OrganizationMembershipStatus.ACTIVE);
        return memberships.saveAndFlush(membership);
    }

    private Customer createCustomer(Organization company, User owner, String suffix) {
        Customer customer = new Customer();
        customer.setOrganization(company);
        customer.setName("Company " + suffix + " Customer");
        customer.setCustomerType(CustomerType.COMPANY);
        customer.setOwnerUser(owner);
        customer.setStatus(CustomerStatus.ACTIVE);
        return customers.saveAndFlush(customer);
    }

    private Lead createLead(Organization company, User owner, String suffix) {
        Lead lead = new Lead();
        lead.setOrganization(company);
        lead.setFullName("Company " + suffix + " Lead");
        lead.setAssignedToUser(owner);
        lead.setStatus(LeadStatus.NEW);
        return leads.saveAndFlush(lead);
    }
}
