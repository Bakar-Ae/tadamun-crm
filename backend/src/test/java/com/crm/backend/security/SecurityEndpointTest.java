package com.crm.backend.security;

import com.crm.backend.organization.invitation.OrganizationInvitationService;
import com.crm.backend.organization.invitation.dto.AcceptOrganizationInvitationRequest;
import com.crm.backend.organization.invitation.dto.OrganizationInvitationAcceptanceResponse;
import com.crm.backend.organization.invitation.dto.OrganizationInvitationPreviewResponse;
import com.crm.backend.role.DataScope;
import com.crm.backend.role.RoleName;
import com.crm.backend.search.GlobalSearchResponse;
import com.crm.backend.search.GlobalSearchService;
import com.crm.backend.search.SearchModule;
import com.crm.backend.search.SearchResultResponse;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.support.MySqlTestContainerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(MySqlTestContainerConfiguration.class)
class SecurityEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataScopeService dataScopeService;

    @MockitoBean
    private GlobalSearchService globalSearchService;

    @MockitoBean
    private CurrentOrganizationProvider currentOrganizationProvider;

    @MockitoBean
    private OrganizationInvitationService organizationInvitationService;

    @BeforeEach
    void setUpRequestContext() {
        when(dataScopeService.currentContext())
                .thenReturn(new DataScopeContext(1L, 1L, DataScope.ALL));
        when(currentOrganizationProvider.getOrganizationId())
                .thenReturn(1L);
    }
    @Test
    void invitationPreviewShouldBePublic() throws Exception {
        when(organizationInvitationService.previewInvitation("valid-token"))
                .thenReturn(new OrganizationInvitationPreviewResponse(
                        "Tadamun",
                        "sales@example.com",
                        RoleName.SALES_REP,
                        LocalDateTime.of(2026, 9, 6, 12, 0),
                        false
                ));

        mockMvc.perform(
                        post("/api/v1/public/organization-invitations/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "token": "valid-token"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value("Tadamun"))
                .andExpect(jsonPath("$.email").value("sales@example.com"))
                .andExpect(jsonPath("$.role").value("SALES_REP"))
                .andExpect(jsonPath("$.requiresAccountCreation").value(false));
    }

    @Test
    void invitationAcceptanceShouldBePublic() throws Exception {
        when(organizationInvitationService.acceptInvitation(
                any(AcceptOrganizationInvitationRequest.class)
        )).thenReturn(new OrganizationInvitationAcceptanceResponse(
                10L,
                "Tadamun",
                2L,
                "sales@example.com",
                RoleName.SALES_REP
        ));

        mockMvc.perform(
                        post("/api/v1/public/organization-invitations/accept")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "token": "valid-token"
                                    }
                                    """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(10))
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.email").value("sales@example.com"));
    }

    @Test
    void invitationPreviewShouldRejectBlankToken() throws Exception {
        mockMvc.perform(
                        post("/api/v1/public/organization-invitations/preview")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "token": ""
                                    }
                                    """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void administrativeInvitationEndpointShouldStillRequireAuthentication()
            throws Exception {
        mockMvc.perform(
                        post("/api/v1/organization-invitations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "email": "new.user@example.com",
                                      "role": "SALES_REP"
                                    }
                                    """)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointShouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void globalSearchShouldRejectRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", "tadamun"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(
            username = "viewer@crm.com",
            authorities = {"CUSTOMER_VIEW"}
    )
    void globalSearchShouldReturnRequestedModuleResults() throws Exception {
        GlobalSearchResponse response = new GlobalSearchResponse(
                "tadamun",
                List.of(new SearchResultResponse(
                        SearchModule.CUSTOMER,
                        42L,
                        "Tadamun Company",
                        "Tadamun Business Solutions",
                        "ACTIVE",
                        null,
                        null
                ))
        );

        when(globalSearchService.search(
                "tadamun",
                5,
                Set.of(SearchModule.CUSTOMER)
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/v1/search")
                                .param("q", "tadamun")
                                .param("limitPerModule", "5")
                                .param("modules", "CUSTOMER")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("tadamun"))
                .andExpect(jsonPath("$.results[0].module").value("CUSTOMER"))
                .andExpect(jsonPath("$.results[0].id").value(42))
                .andExpect(jsonPath("$.results[0].title").value("Tadamun Company"));
    }

    @Test
    @WithMockUser(username = "viewer@crm.com")
    void globalSearchShouldReturnBadRequestForInvalidQuery() throws Exception {
        when(globalSearchService.search("a", 5, null))
                .thenThrow(new IllegalArgumentException(
                        "Search query must contain between 2 and 100 characters"
                ));

        mockMvc.perform(get("/api/v1/search").param("q", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Search query must contain between 2 and 100 characters"
                ));
    }

    @Test
    @WithMockUser(username = "sales@crm.com", roles = {"SALES_REP"})
    void adminEndpointShouldRejectNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
            username = "admin@crm.com",
            authorities = {"ROLE_ADMIN", "USER_VIEW"}
    )
    void adminEndpointShouldAllowAdminUser() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());
    }
    @Test
    @WithMockUser(
            username = "manager@crm.com",
            authorities = {"CUSTOMER_VIEW"}
    )
    void customerEndpointShouldAllowUserWithViewPermission() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(
            username = "restricted@crm.com",
            authorities = {"CUSTOMER_UPDATE"}
    )
    void customerEndpointShouldRejectUserWithoutViewPermission() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
            username = "viewer@crm.com",
            authorities = {"USER_VIEW"}
    )
    void invitationCreationShouldRejectUserWithoutCreatePermission()
            throws Exception {
        mockMvc.perform(
                        post("/api/v1/organization-invitations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "new.user@example.com",
                                          "role": "SALES_REP"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
            username = "admin@crm.com",
            authorities = {"USER_CREATE"}
    )
    void invitationCreationShouldAllowUserWithCreatePermission()
            throws Exception {
        mockMvc.perform(
                        post("/api/v1/organization-invitations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "new.user@example.com",
                                          "role": "SALES_REP"
                                        }
                                        """)
                )
                .andExpect(status().isCreated());
    }
    @Test
    @WithMockUser(
            username = "viewer@crm.com",
            authorities = {"REPORT_VIEW"}
    )
    void reportExportShouldRejectUserWithoutExportPermission()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/reports/advanced/export/excel")
                                .param(
                                        "from",
                                        "2026-07-01T00:00:00Z"
                                )
                                .param(
                                        "to",
                                        "2026-07-02T00:00:00Z"
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(
            username = "manager@crm.com",
            authorities = {"REPORT_EXPORT"}
    )
    void excelExportShouldAllowUserWithExportPermission()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/reports/advanced/export/excel")
                                .param(
                                        "from",
                                        "2026-07-01T00:00:00Z"
                                )
                                .param(
                                        "to",
                                        "2026-07-02T00:00:00Z"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString(".xlsx")
                ));
    }

    @Test
    @WithMockUser(
            username = "manager@crm.com",
            authorities = {"REPORT_EXPORT"}
    )
    void pdfExportShouldAllowUserWithExportPermission()
            throws Exception {
        mockMvc.perform(
                        get("/api/v1/reports/advanced/export/pdf")
                                .param(
                                        "from",
                                        "2026-07-01T00:00:00Z"
                                )
                                .param(
                                        "to",
                                        "2026-07-02T00:00:00Z"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString(".pdf")
                ));
    }
}
