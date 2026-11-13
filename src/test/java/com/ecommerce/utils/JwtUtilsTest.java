package com.ecommerce.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.security.CustomUserDetails;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

class JwtUtilsTest {

    private static final String TEST_SECRET = "ZEdocGMwbHpRVVJsZG1Wc2IzQnRaVzUwUzJWNVJtOXlURzlqWVd4VVpYTjBhVzVuVDI1c2VVNXZkRVp2Y2xCeWIyUjFZM1JwYjI0eE1qTTBOUT09";
    private static final long TEST_DURATION = 3600L; // 1 hour

    private JwtUtils jwtUtils;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        this.jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(this.jwtUtils, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(this.jwtUtils, "jwtDuration", TEST_DURATION);

        Role userRole = Role.builder().id(1).name("USER").build();
        User user = User.builder()
                .id(1)
                .userName("johndoe")
                .password("encoded_pass")
                .roles(List.of(userRole))
                .deleted(false)
                .build();

        this.userDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("generateToken - Should return valid compact JWT with 3 segments")
    void generateToken_Success() {
        String token = jwtUtils.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isBlank());
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT token must have 3 parts separated by dots");
    }

    @Test
    @DisplayName("extractUsername - Should correctly extract subject from token")
    void extractUsername_Success() {
        String token = jwtUtils.generateToken(userDetails);

        String username = jwtUtils.extractUsername(token);

        assertEquals("johndoe", username);
    }

    @Test
    @DisplayName("extractExpiration - Should extract future expiration date")
    void extractExpiration_Success() {
        String token = jwtUtils.generateToken(userDetails);

        Date expiration = jwtUtils.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()), "Expiration date must be in the future");
    }

    @Test
    @DisplayName("extractClaim - Should extract roles claim containing user role")
    void extractClaim_Roles_Success() {
        String token = jwtUtils.generateToken(userDetails);

        @SuppressWarnings("unchecked")
        List<String> roles = jwtUtils.extractClaim(token, claims -> (List<String>) claims.get("roles"));

        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("USER", roles.get(0));
    }

    @Test
    @DisplayName("validateToken - When token and UserDetails match, should return true")
    void validateToken_ValidTokenAndUser_ReturnsTrue() {
        String token = jwtUtils.generateToken(userDetails);

        boolean isValid = jwtUtils.validateToken(token, userDetails);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("validateToken - When username does not match, should return false")
    void validateToken_UsernameMismatch_ReturnsFalse() {
        String token = jwtUtils.generateToken(userDetails);

        User anotherUser = User.builder()
                .id(2)
                .userName("janedoe")
                .password("pass")
                .roles(List.of())
                .build();
        UserDetails anotherUserDetails = new CustomUserDetails(anotherUser);

        boolean isValid = jwtUtils.validateToken(token, anotherUserDetails);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("validateToken - When token is expired, should throw ExpiredJwtException")
    void validateToken_ExpiredToken_ThrowsExpiredJwtException() {
        // Set negative duration to generate already-expired token
        ReflectionTestUtils.setField(this.jwtUtils, "jwtDuration", -60L);
        String expiredToken = jwtUtils.generateToken(userDetails);

        assertThrows(ExpiredJwtException.class, () -> jwtUtils.validateToken(expiredToken, userDetails));
    }

    @Test
    @DisplayName("validateToken - When token signature is tampered, should throw JwtException")
    void validateToken_TamperedSignature_ThrowsJwtException() {
        String token = jwtUtils.generateToken(userDetails);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThrows(JwtException.class, () -> jwtUtils.validateToken(tamperedToken, userDetails));
    }
}
