package com.crm.backend.role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RolePermissionEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void permissionCatalogueShouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"CUSTOMER_VIEW"})
    void permissionCatalogueShouldRejectUserWithoutManagePermission()
            throws Exception {
        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"PERMISSION_MANAGE"})
    void permissionCatalogueShouldAllowPermissionManager() throws Exception {
        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails(
            value = "admin@crm.com",
            userDetailsServiceBeanName = "customUserDetailsService"
    )
    void administratorPermissionsShouldBeImmutable() throws Exception {
        mockMvc.perform(
                        put("/api/v1/roles/ADMIN/permissions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "permissionNames": [
                                            "CUSTOMER_VIEW"
                                          ]
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @WithUserDetails(
            value = "admin@crm.com",
            userDetailsServiceBeanName = "customUserDetailsService"
    )
    void managerPermissionsShouldUpdate() throws Exception {
        mockMvc.perform(
                        put("/api/v1/roles/MANAGER/permissions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "permissionNames": [
                                            "CUSTOMER_VIEW",
                                            "TASK_VIEW"
                                          ]
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("MANAGER"))
                .andExpect(jsonPath("$.editable").value(true))
                .andExpect(jsonPath("$.permissions").isArray());
    }

    @Test
    @WithUserDetails(
            value = "admin@crm.com",
            userDetailsServiceBeanName = "customUserDetailsService"
    )
    void invalidRoleNameShouldReturnBadRequest() throws Exception {
        mockMvc.perform(
                        put("/api/v1/roles/NOT_A_ROLE/permissions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "permissionNames": []
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }
}