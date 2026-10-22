package com.ecommerce.security;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.ecommerce.model.Role;
import com.ecommerce.model.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class CustomUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final String userName;
    private final String password;
    private final Set<GrantedAuthority> authorities;
    private final Set<Role> roles;
    private final boolean deleted;

    public CustomUserDetails(final User user) {
        this.userName = user.getUserName();
        this.password = user.getPassword();
        this.authorities = user.getRoles().stream()
                .flatMap(role -> {
                    String roleName = role.getName();
                    String prefixed = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                    String bare = roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
                    return java.util.stream.Stream.of(
                            new SimpleGrantedAuthority(prefixed),
                            new SimpleGrantedAuthority(bare)
                    );
                })
                .collect(Collectors.toSet());
        this.roles = user.getRoles().stream().collect(Collectors.toSet());
        this.deleted = user.isDeleted();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !this.deleted;
    }
}
