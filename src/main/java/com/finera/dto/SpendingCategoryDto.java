package com.finera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SpendingCategoryDto {
    @NotBlank
    private String categoryName; // Kategori adı (örn: "Yeme İçme / Restoran")
    @NotNull @Positive // Harcama pozitif olmalı
    private BigDecimal amount;   // Bu kategorideki mevcut harcama tutarı
}