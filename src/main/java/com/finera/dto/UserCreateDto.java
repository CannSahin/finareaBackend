package com.finera.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserCreateDto {
    @NotBlank
    private String name;
    @NotBlank
    private String surname;
    @NotBlank @Email
    private String email;
    private String telNo;
    @NotBlank @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password; // Raw password
}