package com.finera.service.ai.impl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finera.dto.ExtractedDataDto;
import com.finera.entities.Category; // Category entity import
// import com.finera.entities.enums.CategoryType; // Bu import artık kullanılmıyor (findAll kullanıldığı için)
import com.finera.exception.AiProcessingException;
import com.finera.repository.CategoryRepository; // CategoryRepository importu
import com.finera.service.ai.AiExtractorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.chat.model.ChatModel; // Genel ChatModel importu yerine spesifik olanı kullanıyoruz
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel; // Spesifik OpenAI ChatModel importu
import org.springframework.ai.openai.OpenAiChatOptions; // OpenAI seçenekleri için import
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List; // List import
import java.util.Map;
import java.util.stream.Collectors; // Stream API import
@Slf4j
@Service("openaiExtractor")
@RequiredArgsConstructor
public class OpenAiExtractorService implements AiExtractorService {
    // OpenAI için spesifik ChatModel'i inject et
    private final OpenAiChatModel openAiChatModel;
    private final ObjectMapper objectMapper;
    private final CategoryRepository categoryRepository;

    // Properties'den OpenAI model ve sıcaklık değerlerini al (varsayılanlarla)
    @Value("${spring.ai.openai.chat.options.model:gpt-4o}")
    private String chatModelName;
    @Value("${spring.ai.openai.chat.options.temperature:0.2}") // Double veya Float olabilir, 0.2f daha güvenli
    private Double chatTemperature; // Double yerine Float kullanmak options ile daha uyumlu olabilir

    // Prompt şablonu (Gemini ile aynı)
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
        log.info("Sending statement text to OpenAI for extraction...");

        // --- Kategori listesini ve varsayılan kategoriyi hazırla (Çalışan Gemini koduyla aynı mantık) ---
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
            // ChatClient'ı builder ile oluşturmak (inject edilen OpenAiChatModel'i kullanarak)
            ChatClient client = ChatClient.builder(this.openAiChatModel) // OpenAI modelini kullan
                    .defaultOptions(OpenAiChatOptions.builder() // OpenAI seçeneklerini kullan
                            // .withModel(this.chatModelName) // ESKİ
                            .model(this.chatModelName)          // YENİ
                            // .withTemperature(this.chatTemperature) // ESKİ
                            .temperature(this.chatTemperature) // YENİ
                            // .withFrequencyPenalty(...) // Diğer OpenAI özel seçenekleri buraya eklenebilir
                            .build())
                    .build();

            // API çağrısı
            String jsonResponse = client.prompt(prompt)
                    .call()
                    .content();

            log.debug("Received JSON response from OpenAI: {}", jsonResponse);

            // JSON temizleme ve parse etme
            String cleanedJson = cleanJsonResponse(jsonResponse);
            ExtractedDataDto extractedData = objectMapper.readValue(cleanedJson, ExtractedDataDto.class);

            // Doğrulama ve loglama
            if (extractedData == null || extractedData.getTransactions() == null) {
                throw new AiProcessingException("OpenAI returned empty or invalid data structure.");
            }
            log.info("Successfully extracted {} transactions using OpenAI for period {}-{}",
                    extractedData.getTransactions().size(), extractedData.getPeriodYear(), extractedData.getPeriodMonth());
            return extractedData;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON response from OpenAI: {}", e.getMessage());
            throw new AiProcessingException("Failed to parse OpenAI response.", e);
        } catch (Exception e) {
            log.error("Error during OpenAI processing (manual client build): {}", e.getMessage(), e);
            throw new AiProcessingException("Error calling OpenAI service (manual client build).", e);
        }
    }

    // JSON temizleme metodu (değişiklik yok)
    private String cleanJsonResponse(String response) {
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