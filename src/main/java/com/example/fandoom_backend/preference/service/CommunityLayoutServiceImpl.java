package com.example.fandoom_backend.preference.service;

import com.example.fandoom_backend.preference.dto.CommunityLayoutRequest;
import com.example.fandoom_backend.preference.dto.CommunityLayoutResponse;
import com.example.fandoom_backend.preference.entity.PreferenceKey;
import com.example.fandoom_backend.preference.entity.UserPreference;
import com.example.fandoom_backend.preference.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommunityLayoutServiceImpl implements CommunityLayoutService {

    private static final Logger log = LoggerFactory.getLogger(CommunityLayoutServiceImpl.class);
    private static final PreferenceKey KEY = PreferenceKey.COMMUNITY_LAYOUT;

    private final UserPreferenceRepository repository;
    private final ObjectMapper objectMapper;

    // value_json'a yazılan şekil; updatedAt satırın kendi kolonundan gelir.
    private record Stored(int leftWidth, int rightWidth, String leftMode, String rightMode) {
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommunityLayoutResponse> get(Long userId) {
        return repository.findByUserIdAndPrefKey(userId, KEY).flatMap(this::toResponse);
    }

    @Override
    @Transactional
    public CommunityLayoutResponse put(Long userId, CommunityLayoutRequest request) {
        Stored stored = new Stored(request.leftWidth(), request.rightWidth(), request.leftMode(), request.rightMode());
        String json = objectMapper.writeValueAsString(stored);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        repository.upsert(userId, KEY.name(), json, now);
        return repository.findByUserIdAndPrefKey(userId, KEY)
                .flatMap(this::toResponse)
                .orElseThrow(() -> new IllegalStateException("Tercih yazıldı ama okunamadı: " + KEY));
    }

    private Optional<CommunityLayoutResponse> toResponse(UserPreference row) {
        try {
            Stored s = objectMapper.readValue(row.getValueJson(), Stored.class);
            return Optional.of(new CommunityLayoutResponse(s.leftWidth(), s.rightWidth(), s.leftMode(),
                    s.rightMode(), row.getUpdatedAt().toInstant(ZoneOffset.UTC)));
        } catch (JacksonException e) {
            // Bozuk kayıt kullanıcıyı 500'e düşürmesin: yok say, FE yerel kayda döner.
            log.warn("Bozuk tercih kaydı yok sayıldı: userId={}, key={}", row.getUserId(), KEY);
            return Optional.empty();
        }
    }
}
