package com.ecommerce.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.request.UserRequestDto;
import com.ecommerce.dto.response.UserResponseDto;
import com.ecommerce.response.SuccessResponse;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public SuccessResponse<UserResponseDto> getCurrentUserProfile(@AuthenticationPrincipal final CustomUserDetails userDetails) {
        log.info("Received request to get current user profile for: '{}'", userDetails.getUsername());
        return SuccessResponse.of(this.userService.getUserByUsername(userDetails.getUsername()));
    }

    @PutMapping("/me")
    public SuccessResponse<UserResponseDto> updateCurrentUserProfile(
            @AuthenticationPrincipal final CustomUserDetails userDetails,
            @Valid @RequestBody final UserRequestDto request) {
        log.info("Received request to update current user profile for: '{}'", userDetails.getUsername());
        return SuccessResponse.of(this.userService.updateCurrentUser(userDetails.getUsername(), request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SuccessResponse<?> getAllUsers(final Pageable pageable) {
        log.info("Received request to get all users with pageable: {}", pageable);
        if (pageable.isPaged()) {
            return SuccessResponse.of(this.userService.getAllUsers(pageable));
        }
        return SuccessResponse.of(this.userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SuccessResponse<UserResponseDto> getUserById(@PathVariable final Integer id) {
        log.info("Received request to get user by ID: {}", id);
        return SuccessResponse.of(this.userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SuccessResponse<UserResponseDto> updateUser(
            @PathVariable final Integer id,
            @Valid @RequestBody final UserRequestDto request) {
        log.info("Received request to update user ID: {}", id);
        return SuccessResponse.of(this.userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SuccessResponse<String> deleteUser(@PathVariable final Integer id) {
        log.info("Received request to delete user ID: {}", id);
        this.userService.deleteUser(id);
        return SuccessResponse.of("User deleted successfully");
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public SuccessResponse<?> searchUsers(
            @RequestParam final String keyword,
            final Pageable pageable) {
        log.info("Received request to search users by keyword '{}'", keyword);
        return SuccessResponse.of(this.userService.searchUsers(keyword, pageable));
    }
}
