package com.gable.tddpipeline.security;

import com.gable.tddpipeline.domain.Role;
import com.gable.tddpipeline.domain.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Wraps the domain User for Spring Security + exposes department scoping. */
public class AppUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final Long departmentId;
    private final boolean active;

    public AppUserPrincipal(User u) {
        this.id = u.getId();
        this.username = u.getUsername();
        this.passwordHash = u.getPasswordHash();
        this.role = u.getRole();
        this.departmentId = u.getDepartment() != null ? u.getDepartment().getId() : null;
        this.active = u.isActive();
    }

    public Long getId() { return id; }
    public Role getRole() { return role; }
    public Long getDepartmentId() { return departmentId; }
    public boolean isAdmin() { return role == Role.ADMIN; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return active; }
}
