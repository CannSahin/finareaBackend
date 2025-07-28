package com.finera.service;

import com.finera.dto.LoginRequestDto;
import com.finera.dto.LoginResponseDto;
import com.finera.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService; // UserDetails'i almak için
    private final JwtService jwtService;

    public LoginResponseDto login(LoginRequestDto loginRequestDto) {
        // 1. Kullanıcıyı doğrula (AuthenticationManager bunu UserDetailsService ve PasswordEncoder ile yapar)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDto.getEmail(),
                        loginRequestDto.getPassword()
                )
        );

        // Authentication başarılıysa Principal UserDetails olur
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 2. JWT Token oluştur
        String jwtToken = jwtService.generateToken(userDetails);

        // 3. Yanıtı döndür
        return new LoginResponseDto(jwtToken, "Bearer", userDetails.getUsername());
    }
}