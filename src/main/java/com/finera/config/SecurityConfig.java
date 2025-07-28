package com.finera.config;

import com.finera.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration; // CORS importu
import org.springframework.web.cors.CorsConfigurationSource; // CORS importu
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // CORS importu

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity // Spring Security'yi etkinleştir
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter; // Oluşturduğumuz filtreyi inject et
    private final UserDetailsService userDetailsService;
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService); // UserDetailsService'i kullan
        authProvider.setPasswordEncoder(passwordEncoder()); // PasswordEncoder'ı kullan
        return authProvider;
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Frontend'inizin çalıştığı origin'i (veya geliştirme için *) ekleyin
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://192.168.1.9:3000")); // Geliştirme için frontend adresleri
        // İzin verilen HTTP metotları
        configuration.setAllowedMethods(Arrays.asList("GET","POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // İzin verilen Header'lar (Authorization dahil)
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        // Cookie gibi credential'ların gönderilmesine izin ver (gerekirse)
        configuration.setAllowCredentials(true); // JWT genellikle Authorization header'ı ile taşınır, bu false olabilir.
        // Preflight (OPTIONS) isteklerinin ne kadar süreyle cache'leneceği (saniye)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // Tüm path'ler için bu konfigürasyonu uygula
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable) // CSRF devre dışı (Stateless API için)
                .authorizeHttpRequests(auth -> auth
                        // Herkese açık endpoint'ler
                        .requestMatchers(
                                "/api/v1/auth/**", // Login endpoint'i
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"

                                // Gerekirse diğer public endpoint'ler (örn: /register)
                        ).permitAll()
                        // Diğer tüm istekler kimlik doğrulaması gerektirir
                        .requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                        .anyRequest().authenticated()
                )
                // Session yönetimini STATELESS yap (JWT ile session'a ihtiyaç yok)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Authentication Provider'ı ayarla
                .authenticationProvider(authenticationProvider())
                // Kendi JWT filtremizi UsernamePasswordAuthenticationFilter'dan ÖNCE ekle
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}