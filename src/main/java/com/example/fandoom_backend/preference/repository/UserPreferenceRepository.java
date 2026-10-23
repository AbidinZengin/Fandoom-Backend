package com.example.fandoom_backend.preference.repository;

import com.example.fandoom_backend.preference.entity.PreferenceKey;
import com.example.fandoom_backend.preference.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findByUserIdAndPrefKey(Long userId, PreferenceKey prefKey);

    // Atomik upsert: iki cihazın eşzamanlı ilk PUT'u unique ihlaline düşmez, sonuncusu kazanır. created_at güncellemede korunur.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO user_preference (user_id, pref_key, value_json, created_at, updated_at)
            VALUES (:userId, :prefKey, :valueJson, :now, :now) AS incoming
            ON DUPLICATE KEY UPDATE value_json = incoming.value_json, updated_at = incoming.updated_at
            """, nativeQuery = true)
    int upsert(@Param("userId") Long userId, @Param("prefKey") String prefKey,
               @Param("valueJson") String valueJson, @Param("now") LocalDateTime now);
}
