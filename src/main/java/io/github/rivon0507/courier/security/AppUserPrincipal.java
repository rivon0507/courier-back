package io.github.rivon0507.courier.security;

import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record AppUserPrincipal(
        Long id,
        String email,
        String displayName,
        String role,
        String passwordHash,
        boolean active
) implements UserDetails {

    @Override
    @NullMarked
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    @NullMarked
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
