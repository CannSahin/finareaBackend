package com.finera.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;

@Component // Spring bileşeni olarak tanımla
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService; // Spring Security'nin UserDetailsService'i inject edilir

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Header yoksa veya "Bearer " ile başlamıyorsa sonraki filtreye geç
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Token'ı header'dan çıkar ("Bearer " kısmını atla)
        jwt = authHeader.substring(7);

        try {
            userEmail = jwtService.extractUsername(jwt);

            // Kullanıcı adı varsa ve SecurityContext'te authentication yoksa
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Token geçerli ise SecurityContext'i güncelle
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // Credentials (password) JWT ile gerekli değil
                            userDetails.getAuthorities() // Rol/Yetkiler
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // Token geçersizse (ExpiredJwtException, MalformedJwtException, SignatureException vb.)
            // Spring Security'nin default error handling'i genellikle 401 veya 403 döndürür.
            // İsterseniz burada loglama yapabilir veya response'u özelleştirebilirsiniz.
            logger.warn("JWT Token processing error: {}",e);
            SecurityContextHolder.clearContext(); // Hata durumunda context'i temizle
            // İsteğe bağlı: Direkt 401/403 döndürmek için:
            // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            // response.getWriter().write("Invalid JWT Token");
            // return; // Filter chain'i burada durdur
            filterChain.doFilter(request, response); // Veya devam et (Spring halleder)
        }
    }
}