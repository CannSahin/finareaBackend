package com.finera.controller;

import com.finera.dto.ManualTransactionRequestDto;
import com.finera.dto.TransactionResponseDto;
import com.finera.entities.User;
import com.finera.exception.BadRequestException;
import com.finera.exception.ResourceNotFoundException; // Exception import
import com.finera.repository.UserRepository; // User Repo import
import com.finera.service.ManualTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions/manual") // Örnek endpoint yolu
@RequiredArgsConstructor
@Tag(name = "Manual Transactions", description = "API for adding manual transactions")
@SecurityRequirement(name = "bearerAuth") // Kimlik doğrulama gerektirir
@Slf4j
public class ManualTransactionController {

    private final ManualTransactionService manualTransactionService;
    private final UserRepository userRepository; // User ID'yi bulmak için

    @PostMapping
    @Operation(summary = "Add a manual transaction for the logged-in user")
    public ResponseEntity<TransactionResponseDto> addManualTransaction(
            @Valid @RequestBody ManualTransactionRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            log.warn("Unauthorized attempt to add manual transaction.");
            // SecurityConfig genellikle bunu engeller ama yine de kontrol edelim
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // UserDetails'den email alıp User ID'yi bul
            String userEmail = userDetails.getUsername();
            User currentUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> {
                        log.error("Authenticated user not found in database: {}", userEmail);
                        return new ResourceNotFoundException("Authenticated user not found: " + userEmail);
                    });
            UUID userId = currentUser.getUserId();

            TransactionResponseDto createdTransaction = manualTransactionService.addManualTransaction(userId, requestDto);
            return new ResponseEntity<>(createdTransaction, HttpStatus.CREATED);

        } catch (ResourceNotFoundException e) {
            log.warn("Failed to add manual transaction due to missing resource: {}", e.getMessage());
            // İsteğe bağlı olarak daha kullanıcı dostu bir mesaj döndürülebilir
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Veya 400 Bad Request
        } catch (BadRequestException e) {
            log.warn("Failed to add manual transaction due to bad request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // Veya mesajı body'de döndür
        } catch (Exception e) {
            log.error("Unexpected error adding manual transaction for user {}: {}", userDetails.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}