package com.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ecommerce.dto.request.ChangePasswordRequest;
import com.ecommerce.dto.request.UserRequestDto;
import com.ecommerce.dto.response.UserResponseDto;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.UserAlreadyExistException;
import com.ecommerce.exception.UserNotFoundException;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.model.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.service.impl.UserServiceImpl;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;
    private UserResponseDto sampleUserDto;

    @BeforeEach
    void setUp() {
        this.sampleUser = User.builder()
                .id(1)
                .name("John Doe")
                .userName("johndoe")
                .email("john@example.com")
                .password("encoded_pass")
                .deleted(false)
                .build();

        this.sampleUserDto = UserResponseDto.builder()
                .id(1)
                .name("John Doe")
                .userName("johndoe")
                .email("john@example.com")
                .deleted(false)
                .roles(List.of("USER"))
                .build();
    }

    @Nested
    @DisplayName("Fetch & Search Users Tests")
    class FetchAndSearchTests {

        @Test
        @DisplayName("getAllUsers - Should return list of UserResponseDto")
        void getAllUsers_Success() {
            when(userRepository.findAll()).thenReturn(List.of(sampleUser));
            when(userMapper.toDTOs(List.of(sampleUser))).thenReturn(List.of(sampleUserDto));

            List<UserResponseDto> result = userService.getAllUsers();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("johndoe", result.get(0).getUserName());
            verify(userRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("getAllUsers (Paged) - Should return paged UserResponseDto")
        void getAllUsers_Paged_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(sampleUser), pageable, 1);

            when(userRepository.findAll(pageable)).thenReturn(userPage);
            when(userMapper.toDTO(sampleUser)).thenReturn(sampleUserDto);

            Page<UserResponseDto> result = userService.getAllUsers(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("johndoe", result.getContent().get(0).getUserName());
            verify(userRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("getUserById - When user exists, should return UserResponseDto")
        void getUserById_Success() {
            when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
            when(userMapper.toDTO(sampleUser)).thenReturn(sampleUserDto);

            UserResponseDto result = userService.getUserById(1);

            assertNotNull(result);
            assertEquals(1, result.getId());
            assertEquals("johndoe", result.getUserName());
        }

        @Test
        @DisplayName("getUserById - When user does not exist, should throw UserNotFoundException")
        void getUserById_NotFound_ThrowsException() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.getUserById(99));
        }

        @Test
        @DisplayName("getUserByUsername - When active user exists, should return UserResponseDto")
        void getUserByUsername_Success() {
            when(userRepository.findByUserName("johndoe")).thenReturn(Optional.of(sampleUser));
            when(userMapper.toDTO(sampleUser)).thenReturn(sampleUserDto);

            UserResponseDto result = userService.getUserByUsername("johndoe");

            assertNotNull(result);
            assertEquals("johndoe", result.getUserName());
        }

        @Test
        @DisplayName("getUserByUsername - When user is soft-deleted, should throw UserNotFoundException")
        void getUserByUsername_WhenDeleted_ThrowsException() {
            sampleUser.setDeleted(true);
            when(userRepository.findByUserName("johndoe")).thenReturn(Optional.of(sampleUser));

            assertThrows(UserNotFoundException.class, () -> userService.getUserByUsername("johndoe"));
        }

        @Test
        @DisplayName("getUserByUsername - When user does not exist, should throw UserNotFoundException")
        void getUserByUsername_NotFound_ThrowsException() {
            when(userRepository.findByUserName("unknown")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.getUserByUsername("unknown"));
        }

        @Test
        @DisplayName("getUserEntityByUsername - When active user exists, should return User entity")
        void getUserEntityByUsername_Success() {
            when(userRepository.findByUserName("johndoe")).thenReturn(Optional.of(sampleUser));

            User result = userService.getUserEntityByUsername("johndoe");

            assertNotNull(result);
            assertEquals("johndoe", result.getUserName());
        }

        @Test
        @DisplayName("searchUsers - Should return matching users with pagination")
        void searchUsers_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(sampleUser), pageable, 1);

            when(userRepository.findByNameContainingIgnoreCase("John", pageable)).thenReturn(userPage);
            when(userMapper.toDTO(sampleUser)).thenReturn(sampleUserDto);

            Page<UserResponseDto> result = userService.searchUsers("John", pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(userRepository, times(1)).findByNameContainingIgnoreCase("John", pageable);
        }
    }

    @Nested
    @DisplayName("Update & Profile Management Tests")
    class UpdateAndProfileTests {

        @Test
        @DisplayName("updateCurrentUser - Should delegate to getUserEntityByUsername and updateUser")
        void updateCurrentUser_Success() {
            UserRequestDto request = UserRequestDto.builder()
                    .name("Updated Name")
                    .userName("johndoe")
                    .email("john@example.com")
                    .build();

            when(userRepository.findByUserName("johndoe")).thenReturn(Optional.of(sampleUser));
            when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(sampleUser)).thenReturn(sampleUser);
            when(userMapper.toDTO(sampleUser)).thenReturn(sampleUserDto);

            UserResponseDto result = userService.updateCurrentUser("johndoe", request);

            assertNotNull(result);
            verify(userMapper, times(1)).updateEntityFromDto(eq(request), eq(sampleUser));
            verify(userRepository, times(1)).save(sampleUser);
        }

        @Test
        @DisplayName("updateUser - When valid input and no duplicates, should update and return DTO")
        void updateUser_Success() {
            UserRequestDto request = UserRequestDto.builder()
                    .name("New Name")
                    .userName("newusername")
                    .email("new@example.com")
                    .build();

            when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
            when(userRepository.existsByUserName("newusername")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(userRepository.save(sampleUser)).thenReturn(sampleUser);
            when(userMapper.toDTO(sampleUser)).thenReturn(sampleUserDto);

            UserResponseDto result = userService.updateUser(1, request);

            assertNotNull(result);
            verify(userRepository, times(1)).save(sampleUser);
        }

        @Test
        @DisplayName("updateUser - When new username already exists, should throw UserAlreadyExistException")
        void updateUser_DuplicateUsername_ThrowsException() {
            UserRequestDto request = UserRequestDto.builder()
                    .name("New Name")
                    .userName("takenusername")
                    .email("john@example.com")
                    .build();

            when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
            when(userRepository.existsByUserName("takenusername")).thenReturn(true);

            assertThrows(UserAlreadyExistException.class, () -> userService.updateUser(1, request));
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("updateUser - When new email already exists, should throw UserAlreadyExistException")
        void updateUser_DuplicateEmail_ThrowsException() {
            UserRequestDto request = UserRequestDto.builder()
                    .name("New Name")
                    .userName("johndoe")
                    .email("taken@example.com")
                    .build();

            when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
            when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

            assertThrows(UserAlreadyExistException.class, () -> userService.updateUser(1, request));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Change Password Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("changePassword - When credentials valid and match, should encode and save")
        void changePassword_Success() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPass123")
                    .newPassword("newPass456")
                    .confirmPassword("newPass456")
                    .build();

            when(userRepository.findByUserName("johndoe")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("oldPass123", "encoded_pass")).thenReturn(true);
            when(passwordEncoder.matches("newPass456", "encoded_pass")).thenReturn(false);
            when(passwordEncoder.encode("newPass456")).thenReturn("new_encoded_pass");

            userService.changePassword("johndoe", request);

            assertEquals("new_encoded_pass", sampleUser.getPassword());
            verify(userRepository, times(1)).save(sampleUser);
        }

        @Test
        @DisplayName("changePassword - When newPassword and confirmPassword do not match, should throw BadRequestException")
        void changePassword_ConfirmMismatch_ThrowsBadRequest() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPass123")
                    .newPassword("newPass456")
                    .confirmPassword("wrongConfirm")
                    .build();

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> userService.changePassword("johndoe", request));
            assertEquals("New password and confirmation password do not match", ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("changePassword - When current password is wrong, should throw BadRequestException")
        void changePassword_WrongCurrentPassword_ThrowsBadRequest() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("wrongCurrentPass")
                    .newPassword("newPass456")
                    .confirmPassword("newPass456")
                    .build();

            when(userRepository.findByUserName("johndoe")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("wrongCurrentPass", "encoded_pass")).thenReturn(false);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> userService.changePassword("johndoe", request));
            assertEquals("Current password is incorrect", ex.getMessage());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("changePassword - When new password same as current password, should throw BadRequestException")
        void changePassword_NewPasswordSameAsCurrent_ThrowsBadRequest() {
            ChangePasswordRequest request = ChangePasswordRequest.builder()
                    .currentPassword("oldPass123")
                    .newPassword("oldPass123")
                    .confirmPassword("oldPass123")
                    .build();

            when(userRepository.findByUserName("johndoe")).thenReturn(Optional.of(sampleUser));
            when(passwordEncoder.matches("oldPass123", "encoded_pass")).thenReturn(true);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> userService.changePassword("johndoe", request));
            assertEquals("New password cannot be the same as current password", ex.getMessage());
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Soft Delete & Restore Tests")
    class SoftDeleteAndRestoreTests {

        @Test
        @DisplayName("deleteUser - When user active, should set deleted = true and save")
        void deleteUser_Success() {
            when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));

            userService.deleteUser(1);

            assertTrue(sampleUser.isDeleted());
            verify(userRepository, times(1)).save(sampleUser);
        }

        @Test
        @DisplayName("deleteUser - When user already deleted or not found, should throw UserNotFoundException")
        void deleteUser_WhenAlreadyDeleted_ThrowsException() {
            sampleUser.setDeleted(true);
            when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));

            assertThrows(UserNotFoundException.class, () -> userService.deleteUser(1));
            verify(userRepository, never()).save(sampleUser);
        }

        @Test
        @DisplayName("restoreUser - When user was deleted, should set deleted = false and return restored DTO")
        void restoreUser_WhenDeleted_Success() {
            sampleUser.setDeleted(true);
            when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
            when(userRepository.save(sampleUser)).thenReturn(sampleUser);
            when(userMapper.toDTO(sampleUser)).thenReturn(sampleUserDto);

            UserResponseDto result = userService.restoreUser(1);

            assertNotNull(result);
            assertFalse(sampleUser.isDeleted());
            verify(userRepository, times(1)).save(sampleUser);
        }

        @Test
        @DisplayName("restoreUser - When user is already active, should return DTO directly without saving")
        void restoreUser_WhenAlreadyActive_ReturnsDirectly() {
            sampleUser.setDeleted(false);
            when(userRepository.findById(1)).thenReturn(Optional.of(sampleUser));
            when(userMapper.toDTO(sampleUser)).thenReturn(sampleUserDto);

            UserResponseDto result = userService.restoreUser(1);

            assertNotNull(result);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("restoreUser - When user not found, should throw UserNotFoundException")
        void restoreUser_NotFound_ThrowsException() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.restoreUser(99));
        }
    }
}
