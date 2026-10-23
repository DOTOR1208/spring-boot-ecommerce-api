package com.ecommerce.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ecommerce.dto.request.UserRequestDto;
import com.ecommerce.dto.response.UserResponseDto;

public interface UserService {

    List<UserResponseDto> getAllUsers();

    Page<UserResponseDto> getAllUsers(Pageable pageable);

    UserResponseDto getUserById(Integer id);

    UserResponseDto getUserByUsername(String username);

    UserResponseDto updateUser(Integer id, UserRequestDto request);

    void deleteUser(Integer id);

    Page<UserResponseDto> searchUsers(String keyword, Pageable pageable);
}
