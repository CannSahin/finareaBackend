package com.finera.controller;

import com.finera.dto.PeriodSummaryResponseDto;
import com.finera.entities.User;
import com.finera.exception.ResourceNotFoundException;
import com.finera.repository.UserRepository;
import com.finera.service.PeriodSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;

@RestController
@RequestMapping("/api/v1/summaries")
@RequiredArgsConstructor
@Tag(name = "Period Summaries", description = "API for generating financial period summaries")
public class PeriodSummaryController {

    private final PeriodSummaryService periodSummaryService;
    private final UserRepository userRepository;

    @GetMapping("/expenses/{year}/{month}")
    @Operation(summary = "Get Expense Summary by User and Period",
            description = "Retrieves a detailed expense summary grouped by source and category for a specific user and period (year/month).")
    @ApiResponse(responseCode = "200", description = "Summary retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized, token missing or invalid")
    @ApiResponse(responseCode = "403", description = "Forbidden, user authenticated but not authorized (should not happen here if token is valid)")
    @ApiResponse(responseCode = "404", description = "User not found for the token's email")
    public ResponseEntity<PeriodSummaryResponseDto> getExpenseSummary(
            @Parameter(description = "Year of the period", required = true, example = "2024") @PathVariable int year,
            @Parameter(description = "Month of the period (1-12)", required = true, example = "1") @PathVariable int month,
            // categoryId query parametresi olarak kalabilir (opsiyonel filtreleme için)
            @Parameter(description = "Optional category ID to filter by") @RequestParam(required = false) Integer categoryId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        if (userDetails == null) {
            // Bu durum JwtAuthenticationFilter tarafından zaten engellenmiş olmalı,
            // ama ek bir kontrol olarak kalabilir.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userEmail = userDetails.getUsername(); // Token'dan e-postayı al
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + userEmail + " from token."));
        UUID currentUserId = currentUser.getUserId(); // Veritabanından kullanıcının UUID'sini al

        // Ay validasyonu
        if (month < 1 || month > 12) {
            return ResponseEntity.badRequest().build();
        }

        // Servise artık path'ten değil, token'dan elde edilen userId'yi gönder
        PeriodSummaryResponseDto summary = periodSummaryService.getPeriodExpenseSummary(currentUserId, year, month);
        // Eğer service metodunuz categoryId de alıyorsa, onu da ekleyin:
        // PeriodSummaryResponseDto summary = periodSummaryService.getPeriodExpenseSummary(currentUserId, year, month, categoryId);


        return ResponseEntity.ok(summary);
    }
}