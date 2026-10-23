package com.example.fandoom_backend.preference.repository;

import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.preference.entity.PreferenceKey;
import com.example.fandoom_backend.preference.entity.UserPreference;
import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Gerçek MySQL, her test rollback: native upsert (ON DUPLICATE KEY UPDATE) ve V7 şeması burada doğrulanır.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class UserPreferenceRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private UserPreferenceRepository repository;

    private Long newUserId() {
        String token = "pf" + (System.nanoTime() % 1_000_000_000L);
        return em.persist(User.builder().username(token).email(token + "@example.com")
                .password("x").role(Role.USER).emailVerified(true).build()).getId();
    }

    @Test
    void upsert_ilkYazimdaEklerIkincideGunceller() {
        Long userId = newUserId();
        LocalDateTime t1 = LocalDateTime.of(2026, 10, 23, 10, 0, 0);
        LocalDateTime t2 = t1.plusMinutes(5);

        repository.upsert(userId, "COMMUNITY_LAYOUT", "{\"v\":1}", t1);
        repository.upsert(userId, "COMMUNITY_LAYOUT", "{\"v\":2}", t2);
        em.clear();

        UserPreference row = repository.findByUserIdAndPrefKey(userId, PreferenceKey.COMMUNITY_LAYOUT).orElseThrow();
        assertThat(row.getValueJson()).isEqualTo("{\"v\":2}");
        assertThat(row.getUpdatedAt()).isEqualTo(t2);
        assertThat(row.getCreatedAt()).isEqualTo(t1);
        assertThat(repository.findAll().stream().filter(p -> p.getUserId().equals(userId))).hasSize(1);
    }

    @Test
    void upsert_kullanicilarBirbirindenAyriDurur() {
        Long a = newUserId();
        Long b = newUserId();
        LocalDateTime now = LocalDateTime.of(2026, 10, 23, 10, 0, 0);

        repository.upsert(a, "COMMUNITY_LAYOUT", "{\"u\":\"a\"}", now);
        repository.upsert(b, "COMMUNITY_LAYOUT", "{\"u\":\"b\"}", now);
        em.clear();

        assertThat(repository.findByUserIdAndPrefKey(a, PreferenceKey.COMMUNITY_LAYOUT).orElseThrow().getValueJson())
                .isEqualTo("{\"u\":\"a\"}");
        assertThat(repository.findByUserIdAndPrefKey(b, PreferenceKey.COMMUNITY_LAYOUT).orElseThrow().getValueJson())
                .isEqualTo("{\"u\":\"b\"}");
    }
}
