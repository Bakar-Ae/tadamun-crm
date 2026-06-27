package com.crm.backend.role;

import com.crm.backend.role.dto.RolePermissionResponse;
import com.crm.backend.role.dto.UpdateRolePermissionsRequest;
import com.crm.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@PreAuthorize("hasAuthority('PERMISSION_MANAGE')")
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    public RolePermissionController(
            RolePermissionService rolePermissionService
    ) {
        this.rolePermissionService = rolePermissionService;
    }

    @GetMapping
    public ResponseEntity<List<RolePermissionResponse>> getRoles() {
        return ResponseEntity.ok(rolePermissionService.getRoles());
    }

    @PutMapping("/{roleName}/permissions")
    public ResponseEntity<RolePermissionResponse> updateRolePermissions(
            @PathVariable RoleName roleName,
            @Valid @RequestBody UpdateRolePermissionsRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                rolePermissionService.updateRolePermissions(
                        roleName,
                        request,
                        currentUser.getId()
                )
        );
    }
}