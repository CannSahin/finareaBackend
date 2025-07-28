package com.finera.dto;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class UserResponseDto {
    private UUID userId;
    private String name;
    private String surname;
    private String email;
    private String telNo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}