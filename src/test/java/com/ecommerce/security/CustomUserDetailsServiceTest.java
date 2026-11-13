package com.ecommerce.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        Role userRole = Role.builder().id(1).name("USER").build();
        this.sampleUser = User.builder()
                .id(1)
                .name("John Doe")
                .userName("johndoe")
                .password("encoded_pass")
                .roles(List.of(userRole))
                .deleted(false)
                .build();
    }

    @Test
    @DisplayName("loadUserByUsername - When user exists, should return CustomUserDetails")
    void loadUserByUsername_UserExists_ReturnsUserDetails() {
        when(userRepository.findByUserName("johndoe")).thenReturn(Optional.of(sampleUser));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("johndoe");

        assertNotNull(userDetails);
        assertEquals("johndoe", userDetails.getUsername());
        assertEquals("encoded_pass", userDetails.getPassword());
        verify(userRepository, times(1)).findByUserName("johndoe");
    }

    @Test
    @DisplayName("loadUserByUsername - When user not found, should throw UsernameNotFoundException")
    void loadUserByUsername_UserNotFound_ThrowsException() {
        when(userRepository.findByUserName("unknown")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("unknown"));

        assertEquals("Cannot find user with userName: unknown", ex.getMessage());
        verify(userRepository, times(1)).findByUserName("unknown");
    }
}
