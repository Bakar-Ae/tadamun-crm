package com.crm.backend.user;

import com.crm.backend.role.RoleName;
import com.crm.backend.security.CustomUserDetails;
import com.crm.backend.user.dto.CreateUserRequest;
import com.crm.backend.user.dto.UpdateUserRequest;
import com.crm.backend.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")

public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize(
            "hasAuthority('USER_CREATE') and " +
                    "hasAuthority('USER_ROLE_CHANGE')"
    )
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(request, currentUser.getId()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) RoleName role,
            Pageable pageable
    ) {
        return ResponseEntity.ok(userService.getAllUsers(keyword, status, role, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(
            "hasAuthority('USER_UPDATE') and " +
                    "hasAuthority('USER_ROLE_CHANGE') and " +
                    "(#request.status() == null or " +
                    "#request.status().name() != 'INACTIVE' or " +
                    "hasAuthority('USER_DEACTIVATE'))"
    )
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request, currentUser.getId()));
    }



    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('USER_DEACTIVATE')")
    public ResponseEntity<UserResponse> deactivateUser(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(userService.deactivateUser(id, currentUser.getId()));
    }


}