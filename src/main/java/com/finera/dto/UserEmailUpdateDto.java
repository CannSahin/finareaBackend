package com.finera.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserEmailUpdateDto {
    @NotBlank @Email
    private String newEmail;
}