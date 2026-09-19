package com.example.fandoom_backend.common.config;

import com.example.fandoom_backend.common.dto.KeysetPageResponse;
import com.example.fandoom_backend.common.dto.PageResponse;
import com.example.fandoom_backend.community.dto.AuthorSummary;
import com.example.fandoom_backend.community.dto.ThreadSummaryResponse;
import com.example.fandoom_backend.community.entity.ThreadSurface;
import com.example.fandoom_backend.movie.dto.MovieSummaryResponse;
import com.example.fandoom_backend.series.dto.SeasonSummaryResponse;
import com.example.fandoom_backend.series.dto.SeriesDetailResponse;
import com.example.fandoom_backend.series.entity.SeriesStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Redis'e gerçekten yazılan/okunan şeyi (JSON + tip bilgisi) gerçek DTO'larla doğrular. Redis
// gerektirmez: asıl risk "record + generic zarf + java.time'ın cache'ten LinkedHashMap olarak
// geri gelip ClassCastException vermesi"dir ve bu serializer düzeyinde yakalanır.
class RedisCacheSerializerTest {

    private final RedisSerializer<Object> serializer = RedisConfig.cacheValueSerializer();

    private Object roundTrip(Object value) {
        return serializer.deserialize(serializer.serialize(value));
    }

    @Test
    void pageResponseOfRecords_withLocalDateAndBigDecimal_roundTripsToSameTypes() {
        PageResponse<MovieSummaryResponse> page = new PageResponse<>(
                List.of(new MovieSummaryResponse(1L, "Title", "Başlık", "title", "p.jpg",
                        LocalDate.of(2024, 5, 17), new BigDecimal("8.4"))),
                0, 20, 1L, 1, true);

        Object restored = roundTrip(page);

        assertThat(restored).isEqualTo(page);
        assertThat(((PageResponse<?>) restored).content().get(0)).isInstanceOf(MovieSummaryResponse.class);
    }

    @Test
    void seriesDetail_withEnumSetsAndNestedRecordsAndDateTimes_roundTrips() {
        SeriesDetailResponse detail = new SeriesDetailResponse(
                1L, "T", "Tr", "Orig", "t", "syn", "özet",
                LocalDate.of(2020, 1, 1), null, SeriesStatus.ONGOING,
                "p.jpg", "c.jpg", null, "TV-MA", "US", "en",
                new BigDecimal("9.1"), 1000, LocalDateTime.of(2026, 9, 19, 10, 15, 30),
                "tt1", 42, 7L, Set.of(1L, 2L), Set.of(3L),
                List.of(new SeasonSummaryResponse(5L, 1, "S1", "S1tr", LocalDate.of(2020, 1, 1), null, "dek")),
                LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2026, 2, 2, 0, 0));

        assertThat(roundTrip(detail)).isEqualTo(detail);
    }

    @Test
    void keysetPageOfThreadSummaries_roundTrips() {
        KeysetPageResponse<ThreadSummaryResponse> page = new KeysetPageResponse<>(
                List.of(new ThreadSummaryResponse(10L, "slug", ThreadSurface.DISCUSSION, "title", "excerpt", null,
                        false, 1L, new AuthorSummary(1L, "abidin", null), "got",
                        3, 2, 1, false, true, List.of("theory", "s1"), LocalDateTime.of(2026, 9, 19, 12, 0))),
                true, "abc");

        assertThat(roundTrip(page)).isEqualTo(page);
    }

    @Test
    void storedValue_isReadableJsonWithTypeInfo() {
        String json = new String(serializer.serialize(
                new AuthorSummary(1L, "abidin", null)), StandardCharsets.UTF_8);

        assertThat(json).contains("\"username\":\"abidin\"").contains("@class");
    }

    @Test
    void classesOutsideAllowList_areRejectedOnDeserialize() {
        // Saldırgan Redis'e "javax.script.ScriptEngineManager" gibi bir gadget yazsa bile okunmaz.
        byte[] malicious = "{\"@class\":\"javax.script.ScriptEngineManager\"}".getBytes(StandardCharsets.UTF_8);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> serializer.deserialize(malicious));
    }
}
