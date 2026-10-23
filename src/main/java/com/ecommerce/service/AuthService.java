package com.ecommerce.service;

import com.ecommerce.dto.request.SignInRequest;
import com.ecommerce.dto.request.SignUpRequest;
import com.ecommerce.dto.response.SignInResponse;
import com.ecommerce.dto.response.UserResponseDto;

public interface AuthService {

    SignInResponse signIn(SignInRequest request);

    UserResponseDto signUp(SignUpRequest request);
}
