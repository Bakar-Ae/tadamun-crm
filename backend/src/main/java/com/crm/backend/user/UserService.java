package com.crm.backend.user;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.role.RoleRepository;
import com.crm.backend.user.dto.CreateUserRequest;
import com.crm.backend.user.dto.UpdateUserRequest;
import com.crm.backend.user.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.auditLogService = auditLogService;
    }


    @Transactional
    public UserResponse createUser(CreateUserRequest request, Long actorUserId) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }


        Role role = roleRepository.findByName(request.role())

                .orElseThrow(() -> new IllegalArgumentException("Role not found"));


        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);

        User savedUser = userRepository.save(user);
        auditLogService.log(
                actorUserId,
                "USER_CREATED",
                "USER",
                savedUser.getId(),
                "{\"email\":\"" + savedUser.getEmail() + "\"}"
        );
        log.info("User created. userId={}, actorUserId={}, role={}",
                savedUser.getId(), actorUserId, savedUser.getRole().getName());




        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(String keyword, UserStatus status, RoleName role, Pageable pageable) {
        return userRepository.searchUsers(keyword, status, role, pageable)
                .map(userMapper::toResponse);
    }


    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = findUserOrThrow(id);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, Long currentUserId) {
        User user = findUserOrThrow(id);

        if (id.equals(currentUserId)) {
            if (request.status() == UserStatus.INACTIVE) {
                throw new IllegalArgumentException("You cannot deactivate your own account");
            }

            if (request.role() != user.getRole().getName()) {
                throw new IllegalArgumentException("You cannot change your own role");
            }

            if (!user.getEmail().equals(request.email())) {
                throw new IllegalArgumentException("You cannot change your own email");
            }
        }

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setRole(role);
        user.setStatus(request.status());
        auditLogService.log(
                currentUserId,
                "USER_UPDATED",
                "USER",
                user.getId(),
                "{\"email\":\"" + user.getEmail() + "\"}"
        );

        log.info("User updated. userId={}, actorUserId={}, status={}, role={}",
                user.getId(), currentUserId, user.getStatus(), user.getRole().getName());

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse deactivateUser(Long id, Long currentUserId) {
        if (id.equals(currentUserId)) {
            throw new IllegalArgumentException("You cannot deactivate your own account");
        }

        User user = findUserOrThrow(id);
        user.setStatus(UserStatus.INACTIVE);
        auditLogService.log(
                currentUserId,
                "USER_DEACTIVATED",
                "USER",
                user.getId(),
                "{\"email\":\"" + user.getEmail() + "\"}"
        );
        log.info("User deactivated. userId={}, actorUserId={}",
                user.getId(), currentUserId);
        return userMapper.toResponse(user);

    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }


    /* it's not in this list but it's as; a note (its about api test)
    Request -> Backend -> Database -> Response
    example : POST /api/v1/auth/login
    JWT token
    */

}
