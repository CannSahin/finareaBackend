package com.finera.service.ai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference; // List<T> parse için
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
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel; // Modeli import et
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("geminiSavingsAdvisor") // Bean adı görevini yansıtmalı
@RequiredArgsConstructor
public class GeminiSavingsAdvisorService implements AiSavingsAdvisorService {

    // Direkt Vertex AI Modelini inject et
    private final VertexAiGeminiChatModel vertexAiChatModel;
    private final ObjectMapper objectMapper;

    // Prompt template aynı kalabilir
    private final String basePromptTemplate = """
            Bir kişisel finans danışmanı olarak hareket et.
            Kullanıcının hedeflediği aylık tasarruf tutarı: {desired_savings} TL.
            Kullanıcının mevcut aylık harcama dökümü aşağıdaki gibidir:
            {spending_details}

            Bu bilgilere dayanarak, kullanıcının hedeflediği tasarruf miktarına ulaşması için hangi harcama kategorilerinden ne kadar kesinti yapabileceğine dair gerçekçi ve uygulanabilir önerilerde bulun.
            Önerilen toplam kesinti miktarı, uygulanabilir ve gerçekçi olmalı mümkün olduğunca hedeflenen tasarruf miktarına yakın olmalıdır.
            Bir kategoriden önerilen kesinti, o kategorideki mevcut harcamayı geçmemelidir.
            Özellikle esnek harcama kalemlerine (Yeme-İçme, Eğlence, Giyim vb.) odaklanmaya çalış, zorunlu giderlere (Faturalar, Kira vb. varsa) daha az dokun.

            Yanıt olarak SADECE aşağıdaki JSON formatında bir liste ver. Başka hiçbir açıklama veya metin ekleme.
            Her öneri için isteğe bağlı kısa bir 'reason' alanı ekleyebilirsin.

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
            """;

    @Override
    public List<SavingsRecommendationDto> generateRecommendations(
            BigDecimal desiredSavings,
            List<SpendingCategoryDto> currentSpending) {

        // Harcama detaylarını prompt için okunabilir bir formata getir
        String spendingDetails = currentSpending.stream()
                .map(s -> "- " + s.getCategoryName() + ": " + s.getAmount().toPlainString() + " TL")
                .collect(Collectors.joining("\n"));

        log.info("Sending savings request to Gemini. Goal: {}, Spending Count: {}", desiredSavings, currentSpending.size());

        PromptTemplate template = new PromptTemplate(basePromptTemplate, Map.of());
        Prompt prompt = template.create(Map.of(
                "desired_savings", desiredSavings.toPlainString(),
                "spending_details", spendingDetails
        ));

        log.debug("Generated Prompt for AI Savings: {}", prompt.getContents());

        try {
            // ChatClient'ı manuel olarak inject edilen model ile oluştur
            ChatClient client = ChatClient.builder(this.vertexAiChatModel).build();

            // Manuel oluşturulan client ile AI modelini çağır
            String jsonResponse = client.prompt(prompt)
                    .call()
                    .content();

            log.debug("Received JSON response from AI for savings: {}", jsonResponse);
            String cleanedJson = cleanJsonResponse(jsonResponse);

            // JSON listesini doğrudan List<SavingsRecommendationDto>'ya parse et
            List<SavingsRecommendationDto> recommendations = objectMapper.readValue(cleanedJson, new TypeReference<>() {});

            if (recommendations == null) {
                throw new AiProcessingException("AI returned null recommendations.");
            }

            log.info("Successfully generated {} savings recommendations using Gemini.", recommendations.size());
            return recommendations;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON savings recommendations from AI: {}", e.getMessage());
            throw new AiProcessingException("Failed to parse AI response for savings.", e);
        } catch (Exception e) {
            log.error("Error during AI savings recommendation processing (manual client build): {}", e.getMessage(), e);
            throw new AiProcessingException("Error calling AI service for savings (manual client build).", e);
        }
    }

    // JSON temizleme metodu (Diğer servislerdeki ile aynı)
    private String cleanJsonResponse(String response) {
        if (response == null) return "[]"; // Boş liste döndür
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