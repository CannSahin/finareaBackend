package com.finera.controller;

import com.finera.dto.SavingsRequestDto;
import com.finera.dto.SavingsResponseDto;
import com.finera.service.SavingsRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/savings")
@RequiredArgsConstructor
@Tag(name = "Savings Recommendations", description = "API for getting AI-powered savings recommendations")
@SecurityRequirement(name = "bearerAuth") // Bu endpoint için JWT gerekebilir
@Slf4j
public class SavingsController {

    private final SavingsRecommendationService savingsRecommendationService;

    @PostMapping("/recommendations")
    @Operation(summary = "Get Savings Recommendations",
            description = "Provides AI-based recommendations on how to achieve a savings goal based on current spending.")
    @ApiResponse(responseCode = "200", description = "Recommendations generated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input data or unsupported AI provider")
    @ApiResponse(responseCode = "500", description = "Internal AI processing error")
    public ResponseEntity<SavingsResponseDto> getSavingsRecommendations(
            @Valid @RequestBody SavingsRequestDto request // Girdi DTO'sunu al
    ) {
        log.info("Received savings recommendation request. Goal: {}, Provider: {}", request.getDesiredSavingsAmount(), request.getProvider());
        try {
            SavingsResponseDto response = savingsRecommendationService.getRecommendations(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) { // Geçersiz provider vb.
            log.warn("Bad request for savings recommendations: {}", e.getMessage());
            // Hata detayını içeren bir yanıt döndürmek daha iyi olabilir
            return ResponseEntity.badRequest().body(new SavingsResponseDto(e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error generating savings recommendations: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new SavingsResponseDto("Error generating recommendations: " + e.getMessage(), null));
        }
    }
}