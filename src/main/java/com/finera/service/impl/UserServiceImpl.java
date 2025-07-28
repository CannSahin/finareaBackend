// --- Service Implementation ---

package com.finera.service.impl;

import com.finera.dto.*;
import com.finera.dto.UserCreateDto;
import com.finera.dto.UserResponseDto;
import com.finera.entities.User;
import com.finera.exception.InvalidCredentialsException;
import com.finera.exception.ResourceNotFoundException; // Simple custom exception
import com.finera.exception.EmailAlreadyExistsException; // Simple custom exception
import com.finera.repository.UserRepository;
import com.finera.service.UserService;
import com.finera.dto.UserUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
   private final PasswordEncoder passwordEncoder; // Inject PasswordEncoder

    @Override
    @Transactional
    public UserResponseDto createUser(UserCreateDto userCreateDto) {
        if (userRepository.existsByEmail(userCreateDto.getEmail())) {
            throw new EmailAlreadyExistsException("User with email " + userCreateDto.getEmail() + " already exists.");
        }

        User user = new User();
        user.setName(userCreateDto.getName());
        user.setSurname(userCreateDto.getSurname());
        user.setEmail(userCreateDto.getEmail());
        user.setTelNo(userCreateDto.getTelNo());
        // Encode the raw password before saving
        user.setPasswordHash(passwordEncoder.encode(userCreateDto.getPassword()));

        User savedUser = userRepository.save(user);
        return mapToResponseDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return mapToResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(UUID userId, UserUpdateDto userUpdateDto) {

        User existingUser = findUserByIdOrThrow(userId);
        existingUser.setName(userUpdateDto.getName());
        existingUser.setSurname(userUpdateDto.getSurname());
        existingUser.setTelNo(userUpdateDto.getTelNo());
        User updatedUser = userRepository.save(existingUser);
        return mapToResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public UserResponseDto updateUserEmail(UUID userId, UserEmailUpdateDto emailUpdateDto) {
        User user = findUserByIdOrThrow(userId);
        String newEmail = emailUpdateDto.getNewEmail();

        if (!user.getEmail().equalsIgnoreCase(newEmail)) { // Only proceed if email is different
            if (userRepository.existsByEmail(newEmail)) {
                throw new EmailAlreadyExistsException("Email " + newEmail + " is already in use.");
            }
            user.setEmail(newEmail);
        }
        // No need to save if email didn't change, but save() handles it anyway
        User updatedUser = userRepository.save(user);
        return mapToResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public void updateUserPassword(UUID userId, UserPasswordUpdateDto passwordUpdateDto) {
        User user = findUserByIdOrThrow(userId);

        // Verify current password
        if (!passwordEncoder.matches(passwordUpdateDto.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Incorrect current password.");
        }

        // Encode and set the new password
        user.setPasswordHash(passwordEncoder.encode(passwordUpdateDto.getNewPassword()));
        userRepository.save(user);
    }




    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }

    // --- Helper Mapping Method ---
    private UserResponseDto mapToResponseDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setUserId(user.getUserId());
        dto.setName(user.getName());
        dto.setSurname(user.getSurname());
        dto.setEmail(user.getEmail());
        dto.setTelNo(user.getTelNo());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
    private User findUserByIdOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }
}