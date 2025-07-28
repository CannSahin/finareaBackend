package com.finera.service;

import com.finera.dto.CategoryTotalDto;

import java.util.List;
import java.util.UUID;

public interface CategorySummaryService {
    /**
     * Belirtilen kullanıcı, yıl ve ay için kategori bazında net harcama/kazanç
     * toplamlarını getirir (Giderler - Gelirler).
     *
     * @param userId Kullanıcı ID'si.
     * @param year   Dönem yılı.
     * @param month  Dönem ayı (1-12).
     * @return Kategori bazında net toplamları içeren CategoryTotalDto listesi.
     */
    List<CategoryTotalDto> getNetCategoryTotalsForPeriod(UUID userId, int year, int month);
}