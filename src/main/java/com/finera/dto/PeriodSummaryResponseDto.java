package com.finera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PeriodSummaryResponseDto {
    private int year;
    private int month;
    private String periodName; // örn: "2024 Ocak"
    private List<SourceSummaryDto> sources;
    private List<CategorySummaryDto> overallCategoryTotals; // Kategori bazında genel toplamlar
    private BigDecimal grandTotal; // Dönem için toplam harcama
}