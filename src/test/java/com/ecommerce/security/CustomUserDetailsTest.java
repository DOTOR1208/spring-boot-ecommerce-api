package com.ecommerce.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import com.ecommerce.model.Role;
import com.ecommerce.model.User;

class CustomUserDetailsTest {

    @Test
    @DisplayName("getAuthorities - When role is 'USER', should generate both 'ROLE_USER' and 'USER' authorities")
    void getAuthorities_BareRoleName_GeneratesDualAuthorities() {
        Role role = Role.builder().id(1).name("USER").build();
        User user = User.builder()
                .userName("john")
                .password("secret")
                .roles(List.of(role))
                .deleted(false)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Set<String> authorityNames = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals(2, authorityNames.size());
        assertTrue(authorityNames.contains("ROLE_USER"), "Should contain prefixed ROLE_USER");
        assertTrue(authorityNames.contains("USER"), "Should contain bare USER");
    }

    @Test
    @DisplayName("getAuthorities - When role is already 'ROLE_ADMIN', should generate both 'ROLE_ADMIN' and 'ADMIN'")
    void getAuthorities_PrefixedRoleName_GeneratesDualAuthorities() {
        Role role = Role.builder().id(2).name("ROLE_ADMIN").build();
        User user = User.builder()
                .userName("admin")
                .password("secret")
                .roles(List.of(role))
                .deleted(false)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Set<String> authorityNames = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        assertEquals(2, authorityNames.size());
        assertTrue(authorityNames.contains("ROLE_ADMIN"));
        assertTrue(authorityNames.contains("ADMIN"));
    }

    @Test
    @DisplayName("isEnabled - When user is not deleted, isEnabled should return true")
    void isEnabled_ActiveUser_ReturnsTrue() {
        User user = User.builder()
                .userName("activeUser")
                .password("secret")
                .roles(List.of())
                .deleted(false)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertTrue(userDetails.isEnabled());
    }

    @Test
    @DisplayName("isEnabled - When user is soft-deleted, isEnabled should return false")
    void isEnabled_SoftDeletedUser_ReturnsFalse() {
        User user = User.builder()
                .userName("deletedUser")
                .password("secret")
                .roles(List.of())
                .deleted(true)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertFalse(userDetails.isEnabled(), "Soft-deleted user must not be enabled");
    }

    @Test
    @DisplayName("Account status flags - Should all return true by default")
    void accountStatusFlags_ReturnTrue() {
        User user = User.builder()
                .userName("user")
                .password("pass")
                .roles(List.of())
                .deleted(false)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);

        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertEquals("user", userDetails.getUsername());
        assertEquals("pass", userDetails.getPassword());
    }
}
