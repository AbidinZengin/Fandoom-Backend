package com.example.fandoom_backend.common.config;

import com.example.fandoom_backend.account.entity.FollowState;
import com.example.fandoom_backend.notification.dto.NotificationResponse;
import com.example.fandoom_backend.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UtcLocalDateTimeConfigTest {

    private final JsonMapper mapper = JsonMapper.builder()
            .addModule(new UtcLocalDateTimeConfig().utcLocalDateTimeModule())
            .build();

    @Test
    void serialize_writesUtcInstantWithZ() {
        String json = mapper.writeValueAsString(LocalDateTime.of(2026, 10, 29, 17, 38, 47));

        assertThat(json).isEqualTo("\"2026-10-29T17:38:47Z\"");
    }

    @Test
    void serialize_keepsFractionalSeconds() {
        String json = mapper.writeValueAsString(LocalDateTime.of(2026, 10, 29, 17, 38, 47, 123_000_000));

        assertThat(json).isEqualTo("\"2026-10-29T17:38:47.123Z\"");
    }

    @Test
    void serialize_nestedRecordField_usesZ() {
        NotificationResponse response = new NotificationResponse(1L, NotificationType.FOLLOW, false,
                LocalDateTime.of(2026, 10, 29, 17, 38, 47),
                new NotificationResponse.ActorSummary("arya", null, FollowState.NONE));

        assertThat(mapper.writeValueAsString(response)).contains("\"createdAt\":\"2026-10-29T17:38:47Z\"");
    }

    @Test
    void deserialize_acceptsValueWithoutZone() {
        LocalDateTime value = mapper.readValue("\"2026-10-29T17:38:47\"", LocalDateTime.class);

        assertThat(value).isEqualTo(LocalDateTime.of(2026, 10, 29, 17, 38, 47));
    }

    @Test
    void deserialize_acceptsValueWithZ_sameWallClockAsUtc() {
        // İstemci GET'ten gelen "…Z" değerini PUT'ta geri gönderebilir; reddedilmemeli.
        LocalDateTime value = mapper.readValue("\"2026-10-29T17:38:47Z\"", LocalDateTime.class);

        assertThat(value).isEqualTo(LocalDateTime.of(2026, 10, 29, 17, 38, 47));
    }
}
