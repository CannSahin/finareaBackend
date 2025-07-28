package com.finera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavingsRecommendationDto {
    private String categoryName; // Tasarruf önerilen kategori
    private BigDecimal suggestedReduction; // Önerilen azaltma miktarı (pozitif)
    private String reason; // (Opsiyonel) Neden bu kategoriden kesinti önerildiği
}