package com.finera.service.ai.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finera.dto.ExtractedDataDto;
import com.finera.entities.Category;
import com.finera.exception.AiProcessingException;
import com.finera.repository.CategoryRepository;
import com.finera.service.ai.AiExtractorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier; // Qualifier importu
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("deepseekExtractor") // Bean adı enum ile eşleşiyor (küçük harf)
@RequiredArgsConstructor
public class DeepseekExtractorService implements AiExtractorService {

    // DeepSeek için özel olarak yapılandırılmış ChatClient'ı inject et
    @Qualifier("deepseekChatClient") // Doğru bean'i seçmek için Qualifier kullan
    private final ChatClient deepseekChatClient;
    private final ObjectMapper objectMapper;
    private final CategoryRepository categoryRepository;

    // Prompt, diğer modellerle aynı olabilir veya DeepSeek için ince ayar gerektirebilir
    private final String basePromptTemplate =  """
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
        List<String> validCategoryNames = categoryRepository.findAll() // Tüm kategorileri al
                .stream()
                .map(Category::getCategoryNameTr) // Türkçe isimleri al
                .collect(Collectors.toList());
        String categoryListString = String.join(", ", validCategoryNames); // Virgülle ayır
        String defaultCategory = "Diğer / Belirsiz"; // Varsayılan kategori adı

        PromptTemplate template = new PromptTemplate(basePromptTemplate, Map.of());
        Prompt prompt = template.create(Map.of(
                "statement_text", statementText,
                "category_list", categoryListString,
                "default_category", defaultCategory
        ));


        log.debug("Generated Prompt for AI: {}", prompt.getContents());

        try {
            // Inject edilen DeepSeek ChatClient'ını kullan
            String jsonResponse = deepseekChatClient.prompt(prompt)
                    .call()
                    .content();

            log.debug("Received JSON response from DeepSeek: {}", jsonResponse);
            String cleanedJson = cleanJsonResponse(jsonResponse);
            ExtractedDataDto extractedData = objectMapper.readValue(cleanedJson, ExtractedDataDto.class);

            if (extractedData == null || extractedData.getTransactions() == null) {
                throw new AiProcessingException("DeepSeek returned empty or invalid data structure.");
            }
            log.info("Successfully extracted {} transactions using DeepSeek for period {}-{}",
                    extractedData.getTransactions().size(), extractedData.getPeriodYear(), extractedData.getPeriodMonth());
            return extractedData;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON response from DeepSeek: {}", e.getMessage());
            throw new AiProcessingException("Failed to parse DeepSeek response.", e);
        } catch (Exception e) {
            log.error("Error during DeepSeek processing: {}", e.getMessage(), e);
            throw new AiProcessingException("Error calling DeepSeek service.", e);
        }
    }

    private String cleanJsonResponse(String response) {
        // Diğer servislerdeki ile aynı temizleme mantığı
        if (response == null) return "{}";
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