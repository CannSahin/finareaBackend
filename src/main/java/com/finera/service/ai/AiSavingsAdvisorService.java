package com.finera.service.ai;

import com.finera.dto.SavingsRecommendationDto;
import com.finera.dto.SpendingCategoryDto;

import java.math.BigDecimal;
import java.util.List;

public interface AiSavingsAdvisorService {

    /**
     * Verilen tasarruf hedefi ve harcama dökümüne göre tasarruf önerileri üretir.
     *
     * @param desiredSavings Hedeflenen tasarruf tutarı.
     * @param currentSpending Mevcut harcamaların kategori bazlı listesi.
     * @return Yapılandırılmış tasarruf önerilerinin listesi.
     * @throws com.finera.exception.AiProcessingException AI işlemi sırasında hata olursa.
     */
    List<SavingsRecommendationDto> generateRecommendations(
            BigDecimal desiredSavings,
            List<SpendingCategoryDto> currentSpending
    );
}