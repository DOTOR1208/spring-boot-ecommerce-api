package com.ecommerce.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.request.SignInRequest;
import com.ecommerce.dto.request.SignUpRequest;
import com.ecommerce.dto.response.SignInResponse;
import com.ecommerce.dto.response.UserResponseDto;
import com.ecommerce.response.SuccessResponse;
import com.ecommerce.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public SuccessResponse<UserResponseDto> signUp(@Valid @RequestBody final SignUpRequest request) {
        log.info("Received sign-up request for username: {}", request.getUserName());
        return SuccessResponse.of(this.authService.signUp(request));
    }

    @PostMapping("/signin")
    public SuccessResponse<SignInResponse> signIn(@Valid @RequestBody final SignInRequest request) {
        log.info("Received sign-in request for username: {}", request.getUserName());
        return SuccessResponse.of(this.authService.signIn(request));
    }
}
