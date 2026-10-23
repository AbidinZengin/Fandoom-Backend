package com.example.fandoom_backend.user.service;

import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.user.mapper.UserMapper;
import com.example.fandoom_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Kullanıcı dizini araması: LIKE deseninin güvenli kurulması ve sayfa boyutu sınırı.
@ExtendWith(MockitoExtension.class)
class UserServiceImplDirectoryTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @InjectMocks
    private UserServiceImpl service;

    private String patternFor(String query) {
        when(userRepository.findVerifiedIdsByUsernamePattern(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        service.searchPublicIds(query, PageRequest.of(0, 20));
        ArgumentCaptor<String> pattern = ArgumentCaptor.forClass(String.class);
        verify(userRepository).findVerifiedIdsByUsernamePattern(pattern.capture(), any(Pageable.class));
        return pattern.getValue();
    }

    @Test
    void nullOrBlankQuery_listsEveryone() {
        assertThat(patternFor(null)).isEqualTo("%");
    }

    @Test
    void query_isTrimmedLowercasedAndWrapped() {
        assertThat(patternFor("  AryA ")).isEqualTo("%arya%");
    }

    @Test
    void likeWildcardsAndEscapeCharInQuery_areEscaped() {
        // Kullanıcı '%' ya da '_' yazarsa joker değil düz karakter aranmalı; '!' kaçış karakterinin kendisi de kaçırılır.
        assertThat(patternFor("a_b%c!")).isEqualTo("%a!_b!%c!!%");
    }

    @Test
    void pageSize_isCappedAt50() {
        when(userRepository.findVerifiedIdsByUsernamePattern(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<Long> ignored = service.searchPublicIds(null, PageRequest.of(2, 5000));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findVerifiedIdsByUsernamePattern(anyString(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(ignored.content()).isEmpty();
    }
}
