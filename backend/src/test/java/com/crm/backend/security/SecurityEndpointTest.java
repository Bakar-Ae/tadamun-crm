package com.crm.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void protectedEndpointShouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
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