package com.finera.service.impl;

import com.finera.dto.CategorySummaryDto;
import com.finera.dto.PeriodSummaryResponseDto;
import com.finera.dto.SourceSummaryDto;
import com.finera.projection.CategorySourceProjection;
import com.finera.repository.TransactionRepository;
import com.finera.service.PeriodSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Constructor Injection için
public class PeriodSummaryServiceImpl implements PeriodSummaryService {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional(readOnly = true) // Sadece okuma işlemi olduğu için
    public PeriodSummaryResponseDto getPeriodExpenseSummary(UUID userId, int year, int month) {

        List<CategorySourceProjection> rawSummaries = transactionRepository.findExpenseSummaryByUserAndPeriod(userId, year, month);

        // Veriyi Kaynak (Source) bazında gruplamak için Map kullan
        Map<String, List<CategorySummaryDto>> groupedBySource = new LinkedHashMap<>(); // Sırayı korumak için LinkedHashMap

        // Kategori bazında genel toplamları hesaplamak için Map
        Map<String, BigDecimal> overallCategoryTotalsMap = new HashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (CategorySourceProjection projection : rawSummaries) {
            String sourceName = projection.getSourceName();
            String categoryName = projection.getCategoryNameTr();
            // Harcamalar negatif, raporda pozitif göstermek için negate() kullan
            BigDecimal amount = projection.getTotalAmount().negate();

            // Kaynak bazlı gruplama
            groupedBySource.computeIfAbsent(sourceName, k -> new ArrayList<>())
                    .add(new CategorySummaryDto(categoryName, amount));

            // Genel kategori toplamlarını güncelle
            overallCategoryTotalsMap.merge(categoryName, amount, BigDecimal::add);

            // Genel Toplamı güncelle
            grandTotal = grandTotal.add(amount);
        }

        // Gruplanmış Map'i List<SourceSummaryDto>'ya dönüştür
        List<SourceSummaryDto> sourceSummaries = groupedBySource.entrySet().stream()
                .map(entry -> new SourceSummaryDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        // Genel Kategori Toplamları Map'ini List<CategorySummaryDto>'ya dönüştür ve sırala
        List<CategorySummaryDto> overallCategoryTotalsList = overallCategoryTotalsMap.entrySet().stream()
                .map(entry -> new CategorySummaryDto(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(CategorySummaryDto::getCategoryName)) // Alfabetik sırala
                .collect(Collectors.toList());


        // Dönem adını oluştur (örn: "2024 Ocak")
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, new Locale("tr", "TR"));
        String periodName = year + " " + monthName;

        return new PeriodSummaryResponseDto(
                year,
                month,
                periodName,
                sourceSummaries,
                overallCategoryTotalsList,
                grandTotal
        );
    }
}