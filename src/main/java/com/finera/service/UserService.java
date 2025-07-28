package com.finera.service;

import com.finera.dto.*;
import com.finera.dto.UserCreateDto;
import com.finera.dto.UserResponseDto;
import com.finera.dto.UserUpdateDto;
import java.util.List;
import java.util.UUID;

public interface UserService {
    UserResponseDto createUser(UserCreateDto userCreateDto);
    UserResponseDto getUserById(UUID userId);
    UserResponseDto getUserByEmail(String email);
    List<UserResponseDto> getAllUsers();
    UserResponseDto updateUser(UUID userId, UserUpdateDto userUpdateDto);
    UserResponseDto updateUserEmail(UUID userId, UserEmailUpdateDto emailUpdateDto);
    void updateUserPassword(UUID userId, UserPasswordUpdateDto passwordUpdateDto);
    void deleteUser(UUID userId);
}