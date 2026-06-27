package com.crm.backend.permission;

import com.crm.backend.permission.dto.PermissionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public PermissionService(
            PermissionRepository permissionRepository,
            PermissionMapper permissionMapper
    ) {
        this.permissionRepository = permissionRepository;
        this.permissionMapper = permissionMapper;
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissions() {
        return permissionRepository.findAllByOrderByNameAsc()
                .stream()
                .map(permissionMapper::toResponse)
                .toList();
    }
}