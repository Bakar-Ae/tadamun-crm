package com.crm.backend.security;

import com.crm.backend.role.DataScope;
import com.crm.backend.user.User;
import com.crm.backend.user.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;
    private final Long teamId;
    private final DataScope dataScope;
    private final boolean platformAdministrator;

    public CustomUserDetails(User user) {
        this(user, false);
    }

    public CustomUserDetails(
            User user,
            boolean platformAdministrator
    ) {
        this.user = user;
        this.teamId = user.getTeam() == null
                ? null
                : user.getTeam().getId();
        this.dataScope = user.getRole().getDataScope();
        this.platformAdministrator = platformAdministrator;
    }

    public Long getTeamId() {
        return teamId;
    }

    public DataScope getDataScope() {
        return dataScope;
    }

    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    public String getFullName() {
        return user.getFullName();
    }

    public boolean isPlatformAdministrator() {
        return platformAdministrator;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        authorities.add(
                new SimpleGrantedAuthority(
                        "ROLE_" + user.getRole().getName().name()
                )
        );

        user.getRole().getPermissions().forEach(permission ->
                authorities.add(
                        new SimpleGrantedAuthority(permission.getName().name())
                )
        );

        if (platformAdministrator) {
            authorities.add(new SimpleGrantedAuthority(
                    PlatformAuthorities.ADMIN
            ));
        }

        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }
}
