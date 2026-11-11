package com.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ecommerce.constant.SecurityRole;
import com.ecommerce.dto.request.SignInRequest;
import com.ecommerce.dto.request.SignUpRequest;
import com.ecommerce.dto.response.SignInResponse;
import com.ecommerce.dto.response.UserResponseDto;
import com.ecommerce.exception.UserAlreadyExistException;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.impl.AuthServiceImpl;
import com.ecommerce.utils.JwtUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;
    private Role sampleRole;

    @BeforeEach
    void setUp() {
        this.sampleRole = Role.builder()
                .id(1)
                .name("USER")
                .build();

        this.sampleUser = User.builder()
                .id(1)
                .name("Alice Smith")
                .userName("alice")
                .email("alice@example.com")
                .password("encoded_pass")
                .roles(List.of(sampleRole))
                .deleted(false)
                .build();
    }

    @Nested
    @DisplayName("Sign In Tests")
    class SignInTests {

        @Test
        @DisplayName("signIn - When credentials valid, should authenticate and return JWT token")
        void signIn_Success() {
            SignInRequest request = SignInRequest.builder()
                    .userName("alice")
                    .password("secretPass")
                    .build();

            CustomUserDetails userDetails = new CustomUserDetails(sampleUser);
            Authentication authentication = mock(Authentication.class);
            Date expiry = new Date(System.currentTimeMillis() + 3600000);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            when(jwtUtils.generateToken(userDetails)).thenReturn("mock.jwt.token");
            when(jwtUtils.extractExpiration("mock.jwt.token")).thenReturn(expiry);

            SignInResponse response = authService.signIn(request);

            assertNotNull(response);
            assertEquals("mock.jwt.token", response.getToken());
            assertEquals(expiry, response.getExpiredDate());
            verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("signIn - When authentication fails, should propagate BadCredentialsException")
        void signIn_BadCredentials_ThrowsException() {
            SignInRequest request = SignInRequest.builder()
                    .userName("alice")
                    .password("wrongPass")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid username or password"));

            assertThrows(BadCredentialsException.class, () -> authService.signIn(request));
            verify(jwtUtils, never()).generateToken(any());
        }
    }

    @Nested
    @DisplayName("Sign Up Tests")
    class SignUpTests {

        @Test
        @DisplayName("signUp - When valid request, should encode password, assign ROLE_USER, init cart, and save")
        void signUp_Success() {
            SignUpRequest request = SignUpRequest.builder()
                    .name("Alice Smith")
                    .userName("alice")
                    .email("alice@example.com")
                    .password("rawPass123")
                    .build();

            User mappedUser = User.builder()
                    .name("Alice Smith")
                    .userName("alice")
                    .email("alice@example.com")
                    .build();

            UserResponseDto responseDto = UserResponseDto.builder()
                    .id(1)
                    .name("Alice Smith")
                    .userName("alice")
                    .email("alice@example.com")
                    .roles(List.of("USER"))
                    .deleted(false)
                    .build();

            when(userRepository.existsByUserName("alice")).thenReturn(false);
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(userMapper.toModel(request)).thenReturn(mappedUser);
            when(passwordEncoder.encode("rawPass123")).thenReturn("encoded_rawPass123");
            when(roleRepository.findByName(SecurityRole.ROLE_USER)).thenReturn(List.of(sampleRole));
            when(userRepository.save(mappedUser)).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(1);
                return u;
            });
            when(userMapper.toDTO(any(User.class))).thenReturn(responseDto);

            UserResponseDto result = authService.signUp(request);

            assertNotNull(result);
            assertEquals("alice", result.getUserName());
            assertEquals("encoded_rawPass123", mappedUser.getPassword());
            assertFalse(mappedUser.isDeleted());
            assertEquals(List.of(sampleRole), mappedUser.getRoles());
            assertNotNull(mappedUser.getCart());
            assertEquals(mappedUser, mappedUser.getCart().getUser());
            verify(userRepository, times(1)).save(mappedUser);
        }

        @Test
        @DisplayName("signUp - When ROLE_USER is prefixed with ROLE_, fallback should assign role")
        void signUp_RoleFallback_Success() {
            SignUpRequest request = SignUpRequest.builder()
                    .name("Alice Smith")
                    .userName("alice")
                    .email("alice@example.com")
                    .password("rawPass123")
                    .build();

            User mappedUser = User.builder()
                    .name("Alice Smith")
                    .userName("alice")
                    .email("alice@example.com")
                    .build();

            when(userRepository.existsByUserName("alice")).thenReturn(false);
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(userMapper.toModel(request)).thenReturn(mappedUser);
            when(passwordEncoder.encode("rawPass123")).thenReturn("encoded_pass");
            when(roleRepository.findByName(SecurityRole.ROLE_USER)).thenReturn(Collections.emptyList());
            when(roleRepository.findByName("ROLE_" + SecurityRole.ROLE_USER)).thenReturn(List.of(sampleRole));
            when(userRepository.save(mappedUser)).thenReturn(mappedUser);
            when(userMapper.toDTO(any())).thenReturn(UserResponseDto.builder().build());

            UserResponseDto result = authService.signUp(request);

            assertNotNull(result);
            assertEquals(List.of(sampleRole), mappedUser.getRoles());
            verify(roleRepository, times(1)).findByName("ROLE_" + SecurityRole.ROLE_USER);
        }

        @Test
        @DisplayName("signUp - When username already exists, should throw UserAlreadyExistException")
        void signUp_DuplicateUsername_ThrowsException() {
            SignUpRequest request = SignUpRequest.builder()
                    .name("Alice")
                    .userName("takenUser")
                    .email("alice@example.com")
                    .password("pass")
                    .build();

            when(userRepository.existsByUserName("takenUser")).thenReturn(true);

            UserAlreadyExistException ex = assertThrows(UserAlreadyExistException.class,
                    () -> authService.signUp(request));
            assertEquals("User with username 'takenUser' already exists", ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("signUp - When email already exists, should throw UserAlreadyExistException")
        void signUp_DuplicateEmail_ThrowsException() {
            SignUpRequest request = SignUpRequest.builder()
                    .name("Alice")
                    .userName("alice")
                    .email("taken@example.com")
                    .password("pass")
                    .build();

            when(userRepository.existsByUserName("alice")).thenReturn(false);
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            UserAlreadyExistException ex = assertThrows(UserAlreadyExistException.class,
                    () -> authService.signUp(request));
            assertEquals("User with email 'taken@example.com' already exists", ex.getMessage());
            verify(userRepository, never()).save(any());
        }
    }
}
