package com.finera.controller;

import com.finera.dto.CategoryTotalDto;
import com.finera.dto.PeriodSummaryResponseDto; // Önceki endpoint için
import com.finera.exception.ResourceNotFoundException;
import com.finera.repository.UserRepository;
import com.finera.service.CategorySummaryService; // YENİ SERVİSİ import et
import com.finera.service.PeriodSummaryService; // Mevcut servisiniz (eğer varsa)
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/summaries")
@RequiredArgsConstructor
@Tag(name = "Financial Summaries", description = "API for various financial summaries")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class SummaryController {

    private final PeriodSummaryService periodSummaryService; // Mevcut olabilir
    private final CategorySummaryService categorySummaryService; // YENİ SERVİSİ inject et
    private final UserRepository userRepository;

    // Mevcut endpoint (Excel benzeri özet için)
    @GetMapping("/expenses-by-source/{year}/{month}") // URL'yi biraz değiştirdim karışmaması için
    @Operation(summary = "Get Expense Summary by Source and Period",
            description = "Retrieves a detailed expense summary grouped by source and category for the authenticated user and a specific period (year/month).")
    @ApiResponse(responseCode = "200", description = "Summary retrieved successfully")
    public ResponseEntity<PeriodSummaryResponseDto> getExpenseSummaryBySource(
            @Parameter(description = "Year of the period", required = true, example = "2024") @PathVariable int year,
            @Parameter(description = "Month of the period (1-12)", required = true, example = "1") @PathVariable int month,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) return ResponseEntity.status(401).build();
        String userEmail = userDetails.getUsername();
        com.finera.entities.User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for email: " + userEmail));
        UUID userId = currentUser.getUserId();

        if (month < 1 || month > 12) return ResponseEntity.badRequest().build();

        PeriodSummaryResponseDto summary = periodSummaryService.getPeriodExpenseSummary(userId, year, month);
        return ResponseEntity.ok(summary);
    }


    // YENİ ENDPOINT (Kategori Toplamları İçin)
    @GetMapping("/category-net-totals/{year}/{month}")
    @Operation(summary = "Get Net Category-wise Totals for a Period",
            description = "Retrieves net totals (Expenses - Incomes) grouped by category for the authenticated user and a specific period (year/month).")
    @ApiResponse(responseCode = "200", description = "Net category totals retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized if JWT token is missing or invalid")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<List<CategoryTotalDto>> getNetCategoryTotals(
            @Parameter(description = "Year of the period", required = true, example = "2025") @PathVariable int year,
            @Parameter(description = "Month of the period (1-12)", required = true, example = "5") @PathVariable int month,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            log.warn("Unauthorized attempt to get category totals.");
            return ResponseEntity.status(401).build();
        }

        String userEmail = userDetails.getUsername();
        com.finera.entities.User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> {
                    log.error("User not found for email {} while fetching category totals.", userEmail);
                    return new ResourceNotFoundException("User not found for email: " + userEmail);
                });
        UUID userId = currentUser.getUserId();

        if (month < 1 || month > 12) {
            log.warn("Invalid month parameter for category totals: {}", month);
            return ResponseEntity.badRequest().build();
        }

        log.info("Request received for net category totals. User: {}, Year: {}, Month: {}", userId, year, month);
        List<CategoryTotalDto> categoryTotals = categorySummaryService.getNetCategoryTotalsForPeriod(userId, year, month);
        return ResponseEntity.ok(categoryTotals);
    }
}