package com.finera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserPasswordUpdateDto {
    @NotBlank
    private String currentPassword; // Required for verification
    @NotBlank @Size(min = 6, message = "New password must be at least 6 characters long")
    private String newPassword;
}