package com.example.fandoom_backend.user.service;

import com.example.fandoom_backend.user.entity.User;
import com.example.fandoom_backend.user.repository.UserRepository;
import com.example.fandoom_backend.user.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Parametre adı Spring Security'nin UserDetailsService sözleşmesinden
    // geliyor ("username"), ama burada gerçekte kullanıcı adı VEYA e-posta
    // kabul edilir — JWT her zaman gerçek username'i taşıdığı için
    // (CustomUserDetails.getUsername()), JwtAuthenticationFilter'daki her
    // istekte bu metod zaten gerçek username ile çağrılır; email fallback'i
    // sadece login anında (AuthServiceImpl) devreye girer.
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) {
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı bulunamadı: " + usernameOrEmail));
        return new CustomUserDetails(user);
    }
}
