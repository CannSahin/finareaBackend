package com.finera.service;

import com.finera.dto.PeriodSummaryResponseDto;

import java.util.UUID;

public interface PeriodSummaryService {
    /**
     * Belirtilen kullanıcı ve dönem (yıl/ay) için harcama özet raporunu oluşturur.
     * Rapor, kaynak (belge türü) ve kategori bazında gruplanmış toplamları içerir.
     *
     * @param userId Kullanıcı ID'si
     * @param year   Dönem yılı
     * @param month  Dönem ayı (1-12)
     * @return PeriodSummaryResponseDto Rapor verisini içeren DTO. Veri yoksa boş listeler içerir.
     */
    PeriodSummaryResponseDto getPeriodExpenseSummary(UUID userId, int year, int month);
}