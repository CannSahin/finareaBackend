package com.finera.service.ai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finera.dto.SavingsRecommendationDto;
import com.finera.dto.SpendingCategoryDto;
import com.finera.exception.AiProcessingException;
import com.finera.service.ai.AiSavingsAdvisorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier; // Qualifier importu
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("deepseekaiSavingsAdvisor")
@RequiredArgsConstructor
public class DeepSeekAiSavingsAdvisorService implements AiSavingsAdvisorService {

    // DeepSeek için özel olarak yapılandırılmış ChatClient'ı inject et
    // Bu bean'in başka bir yerde "deepseekChatClient" adıyla tanımlanmış olması gerekir.
    @Qualifier("deepseekChatClient")
    private final ChatClient deepseekChatClient;
    private final ObjectMapper objectMapper;

    // Prompt, DeepSeek için ayarlanabilir ama şimdilik aynı
    private final String basePromptTemplate = """
            Bir kişisel finans danışmanı olarak hareket et.
            Kullanıcının hedeflediği aylık tasarruf tutarı: {desired_savings} TL.
            Kullanıcının mevcut aylık harcama dökümü aşağıdaki gibidir:
            {spending_details}
            Bu bilgilere dayanarak, gerçekçi tasarruf önerileri sun. JSON listesi olarak yanıt ver.
            ```json
            [
               {{
                "categoryName": "Kategori Adi Ornek",
                "suggestedReduction": 100.00,
                "reason": "Kisa Aciklama Ornek (Opsiyonel)"
              }},
              {{
                "categoryName": "Diger Kategori Ornek",
                "suggestedReduction": 50.50
              }}
            ]
            ```
            """; // Prompt metnini kısa tuttum, önceki gibi detaylı olabilir

    @Override
    public List<SavingsRecommendationDto> generateRecommendations(
            BigDecimal desiredSavings,
            List<SpendingCategoryDto> currentSpending) {

        String spendingDetails = currentSpending.stream()
                .map(s -> "- " + s.getCategoryName() + ": " + s.getAmount().toPlainString() + " TL")
                .collect(Collectors.joining("\n"));

        log.info("Sending savings request to DeepSeekAI. Goal: {}, Spending Count: {}", desiredSavings, currentSpending.size());

        PromptTemplate template = new PromptTemplate(basePromptTemplate, Map.of());
        Prompt prompt = template.create(Map.of(
                "desired_savings", desiredSavings.toPlainString(),
                "spending_details", spendingDetails
        ));

        log.debug("Generated Prompt for AI Savings (DeepSeekAI): {}", prompt.getContents());

        try {
            // Inject edilen DeepSeek ChatClient'ını kullan
            String jsonResponse = deepseekChatClient.prompt(prompt)
                    .call()
                    .content();

            log.debug("Received JSON response from DeepSeekAI for savings: {}", jsonResponse);
            String cleanedJson = cleanJsonResponse(jsonResponse);
            List<SavingsRecommendationDto> recommendations = objectMapper.readValue(cleanedJson, new TypeReference<>() {});

            if (recommendations == null) {
                throw new AiProcessingException("DeepSeekAI returned null recommendations.");
            }

            log.info("Successfully generated {} savings recommendations using DeepSeekAI.", recommendations.size());
            return recommendations;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON savings recommendations from DeepSeekAI: {}", e.getMessage());
            throw new AiProcessingException("Failed to parse DeepSeekAI response for savings.", e);
        } catch (Exception e) {
            log.error("Error during DeepSeekAI savings recommendation processing: {}", e.getMessage(), e);
            throw new AiProcessingException("Error calling DeepSeekAI service for savings.", e);
        }
    }

    private String cleanJsonResponse(String response) {
        if (response == null) return "[]";
        response = response.trim();
        if (response.startsWith("```json")) {
            response = response.substring(7);
        }
        if (response.endsWith("```")) {
            response = response.substring(0, response.length() - 3);
        }
        return response.trim();
    }
}