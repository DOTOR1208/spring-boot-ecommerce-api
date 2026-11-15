package com.ecommerce.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.dto.request.SignInRequest;
import com.ecommerce.dto.request.SignUpRequest;
import com.ecommerce.dto.response.SignInResponse;
import com.ecommerce.dto.response.UserResponseDto;
import com.ecommerce.exception.UserAlreadyExistException;
import com.ecommerce.handler.GlobalExceptionHandler;
import com.ecommerce.service.AuthService;
import com.ecommerce.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/auth/signup - When valid request, returns 200 OK with UserResponseDto")
    void signUp_Success() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .name("Alice Smith")
                .userName("alice")
                .email("alice@example.com")
                .password("password123")
                .build();

        UserResponseDto responseDto = UserResponseDto.builder()
                .id(1)
                .name("Alice Smith")
                .userName("alice")
                .email("alice@example.com")
                .roles(List.of("USER"))
                .deleted(false)
                .build();

        when(authService.signUp(any(SignUpRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.userName").value("alice"))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));

        verify(authService).signUp(any(SignUpRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/signup - When blank fields, returns 400 Bad Request with field errors")
    void signUp_ValidationFailed_BlankFields() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .name("")
                .userName("")
                .email("")
                .password("")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.operationType").value("FAILURE"))
                .andExpect(jsonPath("$.code").value("INVALID"))
                .andExpect(jsonPath("$.details.name").value("Name cannot be blank"))
                .andExpect(jsonPath("$.details.userName").value("Username cannot be blank"))
                .andExpect(jsonPath("$.details.email").value("Email cannot be blank"))
                .andExpect(jsonPath("$.details.password").value("Password cannot be blank"));

        verify(authService, never()).signUp(any());
    }

    @Test
    @DisplayName("POST /api/auth/signup - When email format invalid, returns 400 Bad Request")
    void signUp_ValidationFailed_InvalidEmail() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .name("Alice")
                .userName("alice")
                .email("not-an-email")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.operationType").value("FAILURE"))
                .andExpect(jsonPath("$.code").value("INVALID"))
                .andExpect(jsonPath("$.details.email").value("Invalid email format"));

        verify(authService, never()).signUp(any());
    }

    @Test
    @DisplayName("POST /api/auth/signup - When username already exists, returns 409 Conflict")
    void signUp_DuplicateUsername_Returns409Conflict() throws Exception {
        SignUpRequest request = SignUpRequest.builder()
                .name("Alice")
                .userName("takenUser")
                .email("alice@example.com")
                .password("password123")
                .build();

        when(authService.signUp(any(SignUpRequest.class)))
                .thenThrow(new UserAlreadyExistException("User with username 'takenUser' already exists"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.operationType").value("FAILURE"))
                .andExpect(jsonPath("$.code").value("ALREADY_EXIST"))
                .andExpect(jsonPath("$.message").value("User with username 'takenUser' already exists"));
    }

    @Test
    @DisplayName("POST /api/auth/signin - When valid credentials, returns 200 OK with JWT token")
    void signIn_Success() throws Exception {
        SignInRequest request = SignInRequest.builder()
                .userName("alice")
                .password("password123")
                .build();

        SignInResponse response = SignInResponse.builder()
                .token("mock.jwt.token")
                .expiredDate(new Date(System.currentTimeMillis() + 3600000))
                .build();

        when(authService.signIn(any(SignInRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.data.expiredDate").exists());

        verify(authService).signIn(any(SignInRequest.class));
    }

    @Test
    @DisplayName("POST /api/auth/signin - When bad credentials, returns 401 Unauthorized")
    void signIn_BadCredentials_Returns401Unauthorized() throws Exception {
        SignInRequest request = SignInRequest.builder()
                .userName("alice")
                .password("wrongPassword")
                .build();

        when(authService.signIn(any(SignInRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.operationType").value("FAILURE"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}
