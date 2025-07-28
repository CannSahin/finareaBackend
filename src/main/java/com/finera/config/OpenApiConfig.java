package com.finera.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // Güvenlik şemasının adı (OpenAPI dökümanı içinde kullanılacak key)
        final String securitySchemeName = "bearerAuth";

        // API hakkında genel bilgiler (isteğe bağlı ama önerilir)
        Info apiInfo = new Info()
                .title("Finera API")
                .version("v1.0")
                .description("Finera Kişisel Finans Uygulaması API Dökümantasyonu");

        return new OpenAPI()
                .info(apiInfo) // API bilgilerini ekle
                // Tüm endpointler için global bir güvenlik gereksinimi ekle
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName)) // Yukarıda tanımlanan şema adını kullan
                // Components bölümüne güvenlik şemasını tanımla
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, // Şema adı (key)
                                new SecurityScheme()
                                        .name(securitySchemeName) // Tekrar şema adı
                                        .type(SecurityScheme.Type.HTTP) // Tip: HTTP
                                        .scheme("bearer") // HTTP şeması: bearer
                                        .bearerFormat("JWT") // Bearer formatı: JWT
                        )
                );
    }
}