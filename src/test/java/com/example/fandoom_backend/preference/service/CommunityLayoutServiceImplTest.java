package com.example.fandoom_backend.preference.service;

import com.example.fandoom_backend.preference.dto.CommunityLayoutRequest;
import com.example.fandoom_backend.preference.dto.CommunityLayoutResponse;
import com.example.fandoom_backend.preference.entity.PreferenceKey;
import com.example.fandoom_backend.preference.entity.UserPreference;
import com.example.fandoom_backend.preference.repository.UserPreferenceRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommunityLayoutServiceImplTest {

    private final UserPreferenceRepository repository = mock(UserPreferenceRepository.class);
    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final CommunityLayoutServiceImpl service = new CommunityLayoutServiceImpl(repository, objectMapper);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private UserPreference row(String json) {
        UserPreference row = new UserPreference();
        ReflectionTestUtils.setField(row, "userId", 7L);
        ReflectionTestUtils.setField(row, "prefKey", PreferenceKey.COMMUNITY_LAYOUT);
        ReflectionTestUtils.setField(row, "valueJson", json);
        ReflectionTestUtils.setField(row, "updatedAt", LocalDateTime.of(2026, 10, 23, 22, 41, 7));
        return row;
    }

    @Test
    void get_kayitYoksaBosDoner() {
        when(repository.findByUserIdAndPrefKey(7L, PreferenceKey.COMMUNITY_LAYOUT)).thenReturn(Optional.empty());

        assertThat(service.get(7L)).isEmpty();
    }

    @Test
    void get_bozukKayitiYokSayar() {
        when(repository.findByUserIdAndPrefKey(7L, PreferenceKey.COMMUNITY_LAYOUT))
                .thenReturn(Optional.of(row("{bozuk")));

        assertThat(service.get(7L)).isEmpty();
    }

    @Test
    void put_upsertEderVeYazilanKaydiDoner() {
        String json = "{\"leftWidth\":260,\"rightWidth\":280,\"leftMode\":\"rail\",\"rightMode\":\"hidden\"}";
        when(repository.findByUserIdAndPrefKey(7L, PreferenceKey.COMMUNITY_LAYOUT)).thenReturn(Optional.of(row(json)));

        CommunityLayoutResponse response = service.put(7L, new CommunityLayoutRequest(260, 280, "rail", "hidden"));

        verify(repository).upsert(eq(7L), eq("COMMUNITY_LAYOUT"), eq(json), any(LocalDateTime.class));
        assertThat(response.leftWidth()).isEqualTo(260);
        assertThat(response.leftMode()).isEqualTo("rail");
        assertThat(response.rightMode()).isEqualTo("hidden");
        assertThat(response.updatedAt()).hasToString("2026-10-23T22:41:07Z");
    }

    @Test
    void dogrulama_gecerliIstekHataVermez() {
        assertThat(validator.validate(new CommunityLayoutRequest(200, 420, "hidden", "full"))).isEmpty();
    }

    @Test
    void dogrulama_aralikVeEnumDisiDegerleriReddeder() {
        assertThat(validator.validate(new CommunityLayoutRequest(199, 280, "full", "full"))).hasSize(1);
        assertThat(validator.validate(new CommunityLayoutRequest(260, 239, "full", "full"))).hasSize(1);
        assertThat(validator.validate(new CommunityLayoutRequest(260, 421, "full", "full"))).hasSize(1);
        assertThat(validator.validate(new CommunityLayoutRequest(260, 280, "wide", "full"))).hasSize(1);
        // rail yalnız sol kolon için geçerli
        assertThat(validator.validate(new CommunityLayoutRequest(260, 280, "full", "rail"))).hasSize(1);
    }

    @Test
    void dogrulama_eksikAlanlariReddeder() {
        assertThat(validator.validate(new CommunityLayoutRequest(null, null, null, null))).hasSize(4);
    }
}
