package com.finera.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient; // Bunu kullanmayabiliriz
import org.springframework.web.client.RestTemplate; // RestTemplate kullanmayı deneyelim

@Configuration
@ConditionalOnProperty(prefix = "finera.ai.deepseek", name = "api-key")
public class DeepseekAiConfiguration {

    // OpenAiApi bean'ini API anahtarı ve base URL ile oluşturmayı deneyelim
    @Bean
    @Qualifier("deepseekOpenAiApi")
    public OpenAiApi deepseekOpenAiApi(
            @Value("${finera.ai.deepseek.base-url}") String baseUrl,
            @Value("${finera.ai.deepseek.api-key}") String apiKey
            // RestTemplateBuilder inject edilebilir veya yeni oluşturulabilir
            // RestTemplateBuilder restTemplateBuilder
    ) {
        // OpenAiApi constructor'larını kontrol edin.
        // Genellikle (baseUrl, apiKey) veya (baseUrl, apiKey, restTemplate) gibi seçenekler olur.

        // Seçenek A: Sadece baseUrl ve apiKey alıyorsa:
        return new OpenAiApi(baseUrl, apiKey);

        // Seçenek B: RestTemplate de alıyorsa (Header eklemek için):
        // RestTemplate restTemplate = new RestTemplate();
        // restTemplate.getInterceptors().add((request, body, execution) -> {
        //     request.getHeaders().setBearerAuth(apiKey);
        //     return execution.execute(request, body);
        // });
        // return new OpenAiApi(baseUrl, apiKey, restTemplate); // Bu constructor varsa

        // Seçenek C: Sadece RestClient alıyorsa (önceki) - Hata verdi ama versiyon farkı olabilir
        // RestClient restClient = RestClient.builder()
        //       .baseUrl(baseUrl)
        //       .defaultHeader("Authorization", "Bearer " + apiKey)
        //       .build();
        // return new OpenAiApi(restClient); // Bu hata veriyordu
    }

    // deepseekChatModel ve deepseekChatClient bean tanımları önceki gibi kalabilir
    // (Sadece OpenAiApi bean'inin doğru oluşturulduğundan emin olmamız gerekiyor)

    @Bean
    @Qualifier("deepseekChatModel")
    public OpenAiChatModel deepseekChatModel(
            @Qualifier("deepseekOpenAiApi") OpenAiApi openAiApi,
            @Value("${finera.ai.deepseek.chat.model}") String model,
            @Value("${finera.ai.deepseek.chat.temperature:0.2}") Double temperature
    ) {
        var chatOptions = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();
        return new OpenAiChatModel(openAiApi, chatOptions);
    }

    @Bean
    @Qualifier("deepseekChatClient")
    public ChatClient deepseekChatClient(
            @Qualifier("deepseekChatModel") OpenAiChatModel deepseekChatModel
    ) {
        return ChatClient.builder(deepseekChatModel).build();
    }
}