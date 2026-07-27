package com.example.fandoom_backend.user.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.common.exception.DuplicateResourceException;
import com.example.fandoom_backend.common.exception.InvalidReferenceException;
import com.example.fandoom_backend.common.exception.ResourceNotFoundException;
import com.example.fandoom_backend.user.dto.RegisterRequest;
import com.example.fandoom_backend.user.dto.UserDetailResponse;
import com.example.fandoom_backend.user.dto.UserSummaryResponse;
import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.entity.User;
import com.example.fandoom_backend.user.mapper.UserMapper;
import com.example.fandoom_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.email.verification-expiration-ms}")
    private long verificationExpirationMs;

    @Value("${app.email.verification-link-base}")
    private String verificationLinkBase;

    @Override
    @Transactional
    public UserDetailResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Kullanıcı adı zaten kullanımda: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("E-posta adresi zaten kullanımda: " + request.email());
        }
        String verificationToken = UUID.randomUUID().toString();
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .emailVerified(false)
                .emailVerificationToken(verificationToken)
                .emailVerificationTokenExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(verificationExpirationMs)))
                .build();
        User saved = userRepository.save(user);
        emailService.sendVerificationEmail(saved.getEmail(), saved.getUsername(),
                verificationLinkBase + "?token=" + verificationToken);
        return userMapper.toDetailResponse(saved);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new InvalidReferenceException("Geçersiz veya süresi dolmuş doğrulama tokenı"));
        if (user.getEmailVerificationTokenExpiresAt() == null
                || user.getEmailVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidReferenceException("Geçersiz veya süresi dolmuş doğrulama tokenı");
        }
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiresAt(null);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        // Kayıtsız e-posta için de aynı (sessiz) yanıt döner — kayıtlı/kayıtsız e-posta
        // ayrımı 404 vs 2xx üzerinden dışarıya sızdırılmaz (user enumeration önlemi).
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isEmailVerified()) {
                return; // idempotent — zaten doğrulanmış, tekrar göndermeye gerek yok
            }
            String verificationToken = UUID.randomUUID().toString();
            user.setEmailVerificationToken(verificationToken);
            user.setEmailVerificationTokenExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(verificationExpirationMs)));
            emailService.sendVerificationEmail(user.getEmail(), user.getUsername(),
                    verificationLinkBase + "?token=" + verificationToken);
        });
    }

    @Override
    public PageResponse<UserSummaryResponse> list(Pageable pageable) {
        Page<UserSummaryResponse> page = userRepository.findAll(pageable).map(userMapper::toSummaryResponse);
        return PageResponse.from(page);
    }

    @Override
    public UserDetailResponse getById(Long id) {
        return userMapper.toDetailResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public UserDetailResponse updateRole(Long id, Role role) {
        User user = findEntityById(id);
        user.setRole(role);
        return userMapper.toDetailResponse(user);
    }

    @Override
    @Transactional
    public UserDetailResponse updatePremium(Long id, LocalDateTime premiumExpiresAt) {
        User user = findEntityById(id);
        user.setPremiumExpiresAt(premiumExpiresAt);
        return userMapper.toDetailResponse(user);
    }

    @Override
    public boolean isPremiumActive(Long id) {
        return userRepository.findById(id)
                .map(u -> u.getPremiumExpiresAt() != null && u.getPremiumExpiresAt().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Override
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    private User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı: id=" + id));
    }
}
