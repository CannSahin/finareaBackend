package com.finera.service.ai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finera.dto.ExtractedDataDto;
import com.finera.entities.Category; // Category importu
import com.finera.exception.AiProcessingException;
import com.finera.repository.CategoryRepository; // CategoryRepository importu
import com.finera.service.ai.AiExtractorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("geminiExtractor")
@RequiredArgsConstructor
public class GeminiExtractorService implements AiExtractorService {

    private final VertexAiGeminiChatModel vertexAiChatModel;
    // ChatClient.Builder ile farklı modeller/ayarlar için client'lar oluşturulabilir
  //  private final ChatClient geminiChatClient; // Bean olarak inject edilir (Spring AI starter sağlar)
    private final ObjectMapper objectMapper; // JSON parse için

    private final CategoryRepository categoryRepository;

    // Dikkat: Bu prompt çok önemlidir ve modelinize/verilerinize göre iyileştirilmelidir!
    private final String basePromptTemplate = """
        Aşağıdaki banka ekstresi metnini analiz et.
        Metinden ekstrenin ait olduğu dönemi (YIL ve AY olarak) ve tüm harcama/gelir işlemlerini çıkar.
        Her işlem için, aşağıdaki listede verilen Türkçe kategori isimlerinden en uygun olanını belirle ve "categoryName" alanına yaz.
        Eğer işlem açıklaması listedeki hiçbir kategoriyle anlamlı bir şekilde eşleşmiyorsa veya emin değilsen, kategori adı olarak "{default_category}" kullan.

        Geçerli Türkçe Kategori İsimleri:
        {category_list}

        İşlem tarihi YYYY-MM-DD formatında olmalı. İşlem tutarı sayısal bir değer olmalı (harcamalar için negatif, gelirler için pozitif olabilir).
        Sadece ve sadece aşağıdaki JSON formatında bir yanıt ver, başka hiçbir metin ekleme:
        ```json
        {{
          "periodYear": "YIL",
          "periodMonth": "AY_NUMARASI",
          "transactions": [
            {{
              "date": "YYYY-MM-DD",
              "description": "İŞLEM AÇIKLAMASI",
              "amount": -123.45,
              "categoryName": "KATEGORİ_ADI"
            }}
          ]
        }}
        ```
        Analiz edilecek metin:
        >>>
        {statement_text}
        >>>
        """;




    @Override
    public ExtractedDataDto extractData(String statementText) {
        log.info("Sending statement text to Gemini for extraction...");
        List<String> validCategoryNames = categoryRepository.findAll()
                .stream()
                .map(Category::getCategoryNameTr) // Türkçe isimleri alıyoruz
                .collect(Collectors.toList());
        String categoryListString = String.join(", ", validCategoryNames);
        String defaultCategory = "Diğer / Belirsiz"; // Varsayılan kategori adı


        PromptTemplate template = new PromptTemplate(basePromptTemplate, Map.of());
        Prompt prompt = template.create(Map.of(
                "statement_text", statementText,
                "category_list", categoryListString,
                "default_category", defaultCategory
        ));

        log.debug("Generated Prompt for AI: {}", prompt.getContents());

        try {
            ChatClient client = ChatClient.builder(this.vertexAiChatModel).build();

            // JSON yanıtını temizle (başıdaki/sondaki ```json vb. işaretleri kaldır)
            String jsonResponse = client.prompt(prompt)
                    .call()
                    .content();
            log.debug("Received JSON response from AI: {}", jsonResponse);

            String cleanedJson = cleanJsonResponse(jsonResponse);
            ExtractedDataDto extractedData = objectMapper.readValue(cleanedJson, ExtractedDataDto.class);

            if (extractedData == null || extractedData.getTransactions() == null) {
                throw new AiProcessingException("AI returned empty or invalid data structure.");
            }
            log.info("Successfully extracted {} transactions for period {}-{}",
                    extractedData.getTransactions().size(), extractedData.getPeriodYear(), extractedData.getPeriodMonth());
            return extractedData;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON response from AI: {}", e.getMessage());
            throw new AiProcessingException("Failed to parse AI response.", e);
        } catch (Exception e) {
            // Burada oluşan exception'lar daha anlamlı olabilir (örn: AuthenticationException)
            log.error("Error during AI processing (manual client build): {}", e.getMessage(), e);
            throw new AiProcessingException("Error calling AI service (manual client build).", e);
        }
    }

    private String cleanJsonResponse(String response) {
        if (response == null) return "{}"; // Veya hata fırlat
        // ```json ve ``` gibi işaretleri kaldır
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