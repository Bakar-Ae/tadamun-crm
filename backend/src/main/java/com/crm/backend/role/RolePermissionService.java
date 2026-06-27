package com.crm.backend.role;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.permission.Permission;
import com.crm.backend.permission.PermissionName;
import com.crm.backend.permission.PermissionRepository;
import com.crm.backend.role.dto.RolePermissionResponse;
import com.crm.backend.role.dto.UpdateRolePermissionsRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public RolePermissionService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<RolePermissionResponse> getRoles() {
        return roleRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(role -> role.getName().name()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RolePermissionResponse updateRolePermissions(
            RoleName roleName,
            UpdateRolePermissionsRequest request,
            Long actorUserId
    ) {
        if (roleName == RoleName.ADMIN) {
            throw new IllegalArgumentException(
                    "Administrator permissions cannot be changed"
            );
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        Set<PermissionName> requestedNames =
                new HashSet<>(request.permissionNames());

        List<Permission> requestedPermissions =
                permissionRepository.findAllByOrderByNameAsc()
                        .stream()
                        .filter(permission ->
                                requestedNames.contains(permission.getName())
                        )
                        .toList();

        if (requestedPermissions.size() != requestedNames.size()) {
            throw new IllegalArgumentException(
                    "One or more permissions were not found"
            );
        }

        Set<PermissionName> previousNames = role.getPermissions()
                .stream()
                .map(Permission::getName)
                .collect(java.util.stream.Collectors.toSet());

        Set<PermissionName> addedPermissions = new HashSet<>(requestedNames);
        addedPermissions.removeAll(previousNames);

        Set<PermissionName> removedPermissions = new HashSet<>(previousNames);
        removedPermissions.removeAll(requestedNames);

        role.getPermissions().clear();
        role.getPermissions().addAll(requestedPermissions);

        roleRepository.save(role);

        auditLogService.log(
                actorUserId,
                "ROLE_PERMISSIONS_UPDATED",
                "ROLE",
                role.getId(),
                createAuditDetails(
                        role.getName(),
                        addedPermissions,
                        removedPermissions
                )
        );

        return toResponse(role);
    }

    private RolePermissionResponse toResponse(Role role) {
        List<PermissionName> permissionNames = role.getPermissions()
                .stream()
                .map(Permission::getName)
                .sorted(Comparator.comparing(PermissionName::name))
                .toList();

        return new RolePermissionResponse(
                role.getName(),
                role.getDescription(),
                role.getName() != RoleName.ADMIN,
                permissionNames
        );
    }

    private String createAuditDetails(
            RoleName roleName,
            Set<PermissionName> addedPermissions,
            Set<PermissionName> removedPermissions
    ) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of(
                            "role", roleName,
                            "addedPermissions", addedPermissions,
                            "removedPermissions", removedPermissions
                    )
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not create permission audit details",
                    exception
            );
        }
    }
}