package com.ecommerce.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ecommerce.dto.request.UserRequestDto;
import com.ecommerce.dto.response.UserResponseDto;
import com.ecommerce.exception.UserAlreadyExistException;
import com.ecommerce.exception.UserNotFoundException;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.model.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponseDto> getAllUsers() {
        log.info("Fetching all active users");
        List<User> users = this.userRepository.findAll().stream()
                .filter(user -> !user.isDeleted())
                .toList();
        return this.userMapper.toDTOs(users);
    }

    @Override
    public Page<UserResponseDto> getAllUsers(final Pageable pageable) {
        log.info("Fetching active users with pagination: {}", pageable);
        return this.userRepository.findAll(pageable)
                .map(this.userMapper::toDTO);
    }

    @Override
    public UserResponseDto getUserById(final Integer id) {
        log.info("Fetching user by ID: {}", id);
        User user = this.userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new UserNotFoundException(String.format("User not found with id: %d", id)));
        return this.userMapper.toDTO(user);
    }

    @Override
    public UserResponseDto getUserByUsername(final String username) {
        log.info("Fetching user by username: {}", username);
        User user = this.userRepository.findByUserName(username)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new UserNotFoundException(String.format("User not found with username: %s", username)));
        return this.userMapper.toDTO(user);
    }

    @Override
    public User getUserEntityByUsername(final String username) {
        log.info("Fetching user entity by username: {}", username);
        return this.userRepository.findByUserName(username)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new UserNotFoundException(String.format("User not found with username: %s", username)));
    }

    @Override
    @Transactional
    public UserResponseDto updateCurrentUser(final String username, final UserRequestDto request) {
        log.info("Updating current user profile for username: '{}'", username);
        User user = getUserEntityByUsername(username);
        return updateUser(user.getId(), request);
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(final Integer id, final UserRequestDto request) {
        log.info("Updating user with ID: {}", id);

        User user = this.userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new UserNotFoundException(String.format("User not found with id: %d", id)));

        if (!user.getUserName().equals(request.getUserName()) && this.userRepository.existsByUserName(request.getUserName())) {
            log.warn("Update user failed: username '{}' already exists", request.getUserName());
            throw new UserAlreadyExistException(String.format("User with username '%s' already exists", request.getUserName()));
        }

        if (!user.getEmail().equals(request.getEmail()) && this.userRepository.existsByEmail(request.getEmail())) {
            log.warn("Update user failed: email '{}' already exists", request.getEmail());
            throw new UserAlreadyExistException(String.format("User with email '%s' already exists", request.getEmail()));
        }

        this.userMapper.updateEntityFromDto(request, user);

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(this.passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = this.userRepository.save(user);
        log.info("User with ID: {} updated successfully", id);

        return this.userMapper.toDTO(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(final Integer id) {
        log.info("Soft deleting user with ID: {}", id);

        User user = this.userRepository.findById(id)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new UserNotFoundException(String.format("User not found with id: %d", id)));

        user.setDeleted(true);
        this.userRepository.save(user);
        log.info("User with ID: {} successfully marked as deleted", id);
    }

    @Override
    public Page<UserResponseDto> searchUsers(final String keyword, final Pageable pageable) {
        log.info("Searching active users by keyword '{}' with pagination: {}", keyword, pageable);
        return this.userRepository.findByNameContainingIgnoreCase(keyword, pageable)
                .map(this.userMapper::toDTO);
    }
}
