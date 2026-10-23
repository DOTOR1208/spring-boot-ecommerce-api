package com.ecommerce.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.constant.SecurityRole;
import com.ecommerce.dto.request.SignInRequest;
import com.ecommerce.dto.request.SignUpRequest;
import com.ecommerce.dto.response.SignInResponse;
import com.ecommerce.dto.response.UserResponseDto;
import com.ecommerce.exception.UserAlreadyExistException;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.model.Cart;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.repository.RoleRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.AuthService;
import com.ecommerce.utils.JwtUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    @Override
    public SignInResponse signIn(final SignInRequest request) {
        log.info("Authenticating user with username: {}", request.getUserName());

        Authentication authentication = this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserName(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = this.jwtUtils.generateToken(userDetails);
        Date expiredDate = this.jwtUtils.extractExpiration(token);

        log.info("User '{}' authenticated successfully", request.getUserName());

        return SignInResponse.builder()
                .token(token)
                .expiredDate(expiredDate)
                .build();
    }

    @Override
    @Transactional
    public UserResponseDto signUp(final SignUpRequest request) {
        log.info("Processing sign-up request for username: {}, email: {}", request.getUserName(), request.getEmail());

        if (this.userRepository.existsByUserName(request.getUserName())) {
            log.warn("Sign-up failed: username '{}' already exists", request.getUserName());
            throw new UserAlreadyExistException(String.format("User with username '%s' already exists", request.getUserName()));
        }

        if (this.userRepository.existsByEmail(request.getEmail())) {
            log.warn("Sign-up failed: email '{}' already exists", request.getEmail());
            throw new UserAlreadyExistException(String.format("User with email '%s' already exists", request.getEmail()));
        }

        User user = this.userMapper.toModel(request);
        user.setPassword(this.passwordEncoder.encode(request.getPassword()));
        user.setDeleted(false);

        List<Role> roles = this.roleRepository.findByName(SecurityRole.ROLE_USER);
        if (roles.isEmpty()) {
            roles = this.roleRepository.findByName("ROLE_" + SecurityRole.ROLE_USER);
        }
        user.setRoles(roles);

        Cart cart = Cart.builder()
                .user(user)
                .build();
        user.setCart(cart);

        User savedUser = this.userRepository.save(user);
        log.info("User registered successfully with ID: {}", savedUser.getId());

        return this.userMapper.toDTO(savedUser);
    }
}
