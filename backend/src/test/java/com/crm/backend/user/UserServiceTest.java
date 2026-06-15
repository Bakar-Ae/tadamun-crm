package com.crm.backend.user;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.role.RoleRepository;
import com.crm.backend.user.dto.CreateUserRequest;
import com.crm.backend.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private UserMapper userMapper;
    private AuditLogService auditLogService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userMapper = mock(UserMapper.class);
        auditLogService = mock(AuditLogService.class);

        userService = new UserService(
                userRepository,
                roleRepository,
                passwordEncoder,
                userMapper,
                auditLogService
        );
    }

    @Test
    void createUserShouldRejectDuplicateEmail() {
        CreateUserRequest request = new CreateUserRequest(
                "Sales User",
                "sales@crm.com",
                "Password@123",
                RoleName.SALES_REP
        );

        when(userRepository.existsByEmail("sales@crm.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(request, 1L)
        );

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void deactivateUserShouldRejectCurrentUser() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.deactivateUser(1L, 1L)
        );

        assertEquals("You cannot deactivate your own account", exception.getMessage());
    }

    @Test
    void updateUserShouldRejectChangingOwnRole() {
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setFullName("System Administrator");
        currentUser.setEmail("admin@crm.com");
        currentUser.setRole(adminRole);
        currentUser.setStatus(UserStatus.ACTIVE);

        UpdateUserRequest request = new UpdateUserRequest(
                "System Administrator",
                "admin@crm.com",
                RoleName.SALES_REP,
                UserStatus.ACTIVE
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(currentUser));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateUser(1L, request, 1L)
        );

        assertEquals("You cannot change your own role", exception.getMessage());
    }
}