package com.finera.dto;

import com.finera.service.ai.AiProvider; // Enum import
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class SavingsRequestDto {
    @NotNull @Positive
    private BigDecimal desiredSavingsAmount; // Hedeflenen tasarruf tutarı

    @NotEmpty
    @Valid // İçindeki DTO'ları da doğrula
    private List<SpendingCategoryDto> currentSpending; // Mevcut harcama dökümü

    @NotNull
    private AiProvider provider = AiProvider.GEMINI; // Varsayılan sağlayıcı veya zorunlu
}