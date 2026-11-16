package com.ecommerce.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.dto.request.ChangePasswordRequest;
import com.ecommerce.dto.request.UserRequestDto;
import com.ecommerce.dto.response.UserResponseDto;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.handler.GlobalExceptionHandler;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.UserService;
import com.ecommerce.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@EnableMethodSecurity
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private User sampleUser;
    private CustomUserDetails customUserDetails;
    private UserResponseDto sampleUserDto;
    private UserRequestDto sampleUserRequest;

    private User adminUser;
    private CustomUserDetails adminUserDetails;

    @BeforeEach
    void setUpCommon() {
        Role userRole = Role.builder().id(1).name("USER").build();
        this.sampleUser = User.builder()
                .id(1)
                .name("John Doe")
                .userName("johndoe")
                .email("john@example.com")
                .password("encoded_pass")
                .roles(List.of(userRole))
                .deleted(false)
                .build();
        this.customUserDetails = new CustomUserDetails(sampleUser);

        Role adminRole = Role.builder().id(2).name("ADMIN").build();
        this.adminUser = User.builder()
                .id(2)
                .name("Admin User")
                .userName("admin")
                .email("admin@example.com")
                .password("encoded_pass")
                .roles(List.of(adminRole))
                .deleted(false)
                .build();
        this.adminUserDetails = new CustomUserDetails(adminUser);

        this.sampleUserDto = UserResponseDto.builder()
                .id(1)
                .name("John Doe")
                .userName("johndoe")
                .email("john@example.com")
                .roles(List.of("USER"))
                .deleted(false)
                .build();

        this.sampleUserRequest = UserRequestDto.builder()
                .name("John Doe Updated")
                .userName("johndoe")
                .email("john.updated@example.com")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Current User Profile Tests (@AuthenticationPrincipal)")
    class CurrentUserProfileTests {

        @BeforeEach
        void setUpUserContext() {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    customUserDetails, null, customUserDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        @Test
        @DisplayName("GET /api/users/me - Returns current user profile")
        void getCurrentUserProfile_Success() throws Exception {
            when(userService.getUserByUsername("johndoe")).thenReturn(sampleUserDto);

            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.userName").value("johndoe"));

            verify(userService).getUserByUsername("johndoe");
        }

        @Test
        @DisplayName("PUT /api/users/me - Updates current user profile and returns 200 OK")
        void updateCurrentUserProfile_Success() throws Exception {
            when(userService.updateCurrentUser(eq("johndoe"), any(UserRequestDto.class))).thenReturn(sampleUserDto);

            mockMvc.perform(put("/api/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleUserRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.userName").value("johndoe"));

            verify(userService).updateCurrentUser(eq("johndoe"), any(UserRequestDto.class));
        }

        @Test
        @DisplayName("PUT /api/users/me - When email format invalid, returns 400 Bad Request")
        void updateCurrentUserProfile_ValidationFailed_InvalidEmail() throws Exception {
            UserRequestDto invalidRequest = UserRequestDto.builder()
                    .name("John")
                    .userName("johndoe")
                    .email("not-an-email")
                    .build();

            mockMvc.perform(put("/api/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.operationType").value("FAILURE"))
                    .andExpect(jsonPath("$.code").value("INVALID"))
                    .andExpect(jsonPath("$.details.email").value("Invalid email format"));

            verify(userService, never()).updateCurrentUser(any(), any());
        }

        @Test
        @DisplayName("PUT /api/users/change-password - When valid, changes password and returns 200 OK")
        void changePassword_Success() throws Exception {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPass123")
                    .newPassword("newPass456")
                    .confirmPassword("newPass456")
                    .build();

            doNothing().when(userService).changePassword(eq("johndoe"), any(ChangePasswordRequest.class));

            mockMvc.perform(put("/api/users/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data").value("Password changed successfully"));

            verify(userService).changePassword(eq("johndoe"), any(ChangePasswordRequest.class));
        }

        @Test
        @DisplayName("PUT /api/users/change-password - When mismatch confirm password, returns 400 Bad Request")
        void changePassword_MismatchConfirm_Returns400() throws Exception {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPass123")
                    .newPassword("newPass456")
                    .confirmPassword("wrongConfirm")
                    .build();

            doThrow(new BadRequestException("New password and confirmation password do not match"))
                    .when(userService).changePassword(eq("johndoe"), any(ChangePasswordRequest.class));

            mockMvc.perform(put("/api/users/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.operationType").value("FAILURE"))
                    .andExpect(jsonPath("$.message").value("New password and confirmation password do not match"));
        }
    }

    @Nested
    @DisplayName("Admin Management Tests (@PreAuthorize('hasRole(ADMIN)'))")
    class AdminManagementTests {

        @BeforeEach
        void setUpAdminContext() {
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    adminUserDetails, null, adminUserDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        @Test
        @DisplayName("GET /api/users - When user has ROLE_ADMIN, returns 200 OK with all users")
        void getAllUsers_AsAdmin_Success() throws Exception {
            Page<UserResponseDto> page = new PageImpl<>(List.of(sampleUserDto));
            when(userService.getAllUsers(any(Pageable.class))).thenReturn(page);
            when(userService.getAllUsers()).thenReturn(List.of(sampleUserDto));

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.operationType").value("SUCCESS"));
        }

        @Test
        @DisplayName("GET /api/users - When user has ROLE_USER, returns 403 Forbidden")
        void getAllUsers_AsUser_Returns403Forbidden() throws Exception {
            Authentication userAuth = new UsernamePasswordAuthenticationToken(
                    customUserDetails, null, customUserDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(userAuth);

            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.operationType").value("FAILURE"))
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));

            verify(userService, never()).getAllUsers();
        }

        @Test
        @DisplayName("GET /api/users/{id} - When user has ROLE_ADMIN, returns user by ID")
        void getUserById_AsAdmin_Success() throws Exception {
            when(userService.getUserById(1)).thenReturn(sampleUserDto);

            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(1));

            verify(userService).getUserById(1);
        }

        @Test
        @DisplayName("DELETE /api/users/{id} - When user has ROLE_ADMIN, soft deletes user")
        void deleteUser_AsAdmin_Success() throws Exception {
            doNothing().when(userService).deleteUser(1);

            mockMvc.perform(delete("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data").value("User deleted successfully"));

            verify(userService).deleteUser(1);
        }

        @Test
        @DisplayName("PUT /api/users/{id}/restore - When user has ROLE_ADMIN, restores user")
        void restoreUser_AsAdmin_Success() throws Exception {
            when(userService.restoreUser(1)).thenReturn(sampleUserDto);

            mockMvc.perform(put("/api/users/1/restore"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(1));

            verify(userService).restoreUser(1);
        }
    }
}
