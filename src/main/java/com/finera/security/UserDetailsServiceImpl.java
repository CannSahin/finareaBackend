package com.finera.security;

import com.finera.entities.User;
import com.finera.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Collections; // Basit yetkilendirme için

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Spring Security'nin UserDetails arayüzünü kullanın
        // Şimdilik roller/yetkiler olmadan basit bir implementasyon
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                Collections.emptyList() // Roller/Authorities buraya eklenebilir (örn: user.getRoles()...)
        );
    }

    // JWT Filter'ın User entity'sine erişmesi için helper method (isteğe bağlı)
    @Transactional(readOnly = true)
    public UserDetails loadUserDetailsById(String email) {
        // Yukarıdaki metodun aynısı gibi ama direkt UserDetails döner
        return loadUserByUsername(email);
    }
}