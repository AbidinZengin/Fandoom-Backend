package com.example.fandoom_backend.user.config;

import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.entity.User;
import com.example.fandoom_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.username:}")
    private String bootstrapUsername;

    @Value("${admin.bootstrap.password:}")
    private String bootstrapPassword;

    @Value("${admin.bootstrap.email:}")
    private String bootstrapEmail;

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapUsername.isBlank() || bootstrapPassword.isBlank() || bootstrapEmail.isBlank()) {
            return; // env-var'lar setli değilse sessizce atla
        }
        if (userRepository.existsByRole(Role.ADMIN)) {
            return; // zaten bir ADMIN var, tekrar oluşturma
        }
        User admin = User.builder()
                .username(bootstrapUsername)
                .email(bootstrapEmail)
                .password(passwordEncoder.encode(bootstrapPassword))
                .role(Role.ADMIN)
                .emailVerified(true)
                .build();
        userRepository.save(admin);
        log.warn("Bootstrap ADMIN kullanıcısı oluşturuldu: {}", bootstrapUsername);
    }
}
