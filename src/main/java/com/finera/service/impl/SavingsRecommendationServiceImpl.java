package com.finera.service.impl;

import com.finera.dto.SavingsRecommendationDto;
import com.finera.dto.SavingsRequestDto;
import com.finera.dto.SavingsResponseDto;
import com.finera.service.SavingsRecommendationService;
import com.finera.service.ai.AiProvider;
import com.finera.service.ai.AiSavingsAdvisorService; // Yeni AI interface'ini import et
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsRecommendationServiceImpl implements SavingsRecommendationService {

    // Map olarak tüm AiSavingsAdvisorService implementasyonlarını inject et
    private final Map<String, AiSavingsAdvisorService> aiSavingsAdvisors;

    @Override
    public SavingsResponseDto getRecommendations(SavingsRequestDto request) {
        AiProvider provider = request.getProvider(); // İstekten provider'ı al

        // Doğru AI servisini Map'ten seç
        AiSavingsAdvisorService advisor = getAiSavingsAdvisor(provider);
        log.info("Using AI savings advisor implementation: {}", advisor.getClass().getSimpleName());

        // AI servisinden yapılandırılmış önerileri al
        List<SavingsRecommendationDto> recommendations = advisor.generateRecommendations(
                request.getDesiredSavingsAmount(),
                request.getCurrentSpending()
        );

        // Önerilerden metinsel bir özet oluştur (basit örnek)
        String summary = generateSummary(request.getDesiredSavingsAmount(), recommendations);

        return new SavingsResponseDto(summary, recommendations);
    }

    /**
     * Verilen AiProvider enum'una karşılık gelen AiSavingsAdvisorService bean'ini Map'ten alır.
     */
    private AiSavingsAdvisorService getAiSavingsAdvisor(AiProvider provider) {
        String beanName = provider.name().toLowerCase() + "SavingsAdvisor";
        AiSavingsAdvisorService service = aiSavingsAdvisors.get(beanName);
        if (service == null) {
            throw new IllegalArgumentException("No AiSavingsAdvisorService implementation found for provider: " + provider + " (Expected bean name: '" + beanName + "'). Available beans: " + aiSavingsAdvisors.keySet());
        }
        log.info("Selected AI Savings Advisor bean: {}", beanName);
        return service;
    }

    /**
     * Yapılandırılmış önerilerden basit bir metinsel özet oluşturur.
     */
    private String generateSummary(BigDecimal goal, List<SavingsRecommendationDto> recommendations) {
        if (recommendations.isEmpty()) {
            return "Hedefinize (" + goal + " TL) ulaşmak için AI tarafından spesifik bir kesinti önerisi bulunamadı. Harcamalarınızı gözden geçirin.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(goal).append(" TL tasarruf hedefinize ulaşmak için öneriler:\n");

        BigDecimal totalReduction = BigDecimal.ZERO;
        for (SavingsRecommendationDto rec : recommendations) {
            sb.append("- ").append(rec.getCategoryName()).append(": ").append(rec.getSuggestedReduction()).append(" TL azaltın.");
            if(rec.getReason() != null && !rec.getReason().isBlank()){
                sb.append(" (").append(rec.getReason()).append(")");
            }
            sb.append("\n");
            totalReduction = totalReduction.add(rec.getSuggestedReduction());
        }
        sb.append("Toplam önerilen kesinti: ").append(totalReduction).append(" TL.");

        // Hedefe ne kadar yakın olunduğunu belirten bir not eklenebilir.

        return sb.toString();
    }
}