package com.example.fandoom_backend.user.repository;

import com.example.fandoom_backend.common.config.JpaAuditingConfig;
import com.example.fandoom_backend.user.entity.Role;
import com.example.fandoom_backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// Gerçek MySQL, her test rollback. Başka testlerin/seed'in kullanıcıları karışmasın diye her test benzersiz bir
// ad belirteci (token) kullanır ve yalnız kendi kullanıcılarını arar.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class UserDirectoryRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private UserRepository userRepository;

    private final String token = "dz" + (System.nanoTime() % 1_000_000_000L);

    private User user(String name, boolean verified) {
        return em.persist(User.builder().username(token + name).email(token + name + "@example.com")
                .password("x").role(Role.USER).emailVerified(verified).build());
    }

    private Page<Long> search(String pattern) {
        em.flush();
        em.clear();
        return userRepository.findVerifiedIdsByUsernamePattern(pattern, PageRequest.of(0, 50));
    }

    @Test
    void returnsOnlyEmailVerifiedUsers() {
        User verified = user("a", true);
        user("b", false);

        assertThat(search("%" + token + "%").getContent()).containsExactly(verified.getId());
    }

    @Test
    void matchesAnywhereInUsername_caseInsensitiveOnLowercasedPattern() {
        User mixed = user("MixedCase", true);

        assertThat(search("%" + token + "mixedcase%").getContent()).containsExactly(mixed.getId());
        assertThat(search("%dcas%").getContent()).contains(mixed.getId());
    }

    @Test
    void escapedWildcards_matchLiterallyNotAsJokers() {
        User underscore = user("a_b", true);
        User other = user("axb", true);

        // '_' kaçırılmışsa yalnız gerçek alt çizgiyi bulur; kaçırılmamış '_' tek karakter jokeridir ve ikisini de bulurdu.
        assertThat(search("%" + token + "a!_b%").getContent()).containsExactly(underscore.getId());
        assertThat(search("%" + token + "a_b%").getContent())
                .containsExactlyInAnyOrder(underscore.getId(), other.getId());
    }

    @Test
    void ordersNewestMemberFirst() {
        User older = user("old", true);
        User newer = user("new", true);
        em.flush();
        // created_at @CreatedDate ile persist anında ezilir (updatable=false): yaş native UPDATE ile verilir.
        em.getEntityManager().createNativeQuery("UPDATE app_user SET created_at = :ts WHERE id = :id")
                .setParameter("ts", LocalDateTime.now().minusDays(5)).setParameter("id", older.getId())
                .executeUpdate();

        assertThat(search("%" + token + "%").getContent()).containsExactly(newer.getId(), older.getId());
    }
}
