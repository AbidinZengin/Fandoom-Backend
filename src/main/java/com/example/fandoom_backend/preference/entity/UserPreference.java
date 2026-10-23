package com.example.fandoom_backend.preference.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Salt-okunur: yazma UserPreferenceRepository.upsert (INSERT ... ON DUPLICATE KEY UPDATE) ile yapılır, eşzamanlı
// ilk PUT'ta unique ihlali (409) oluşmasın diye. Zamanlar UTC.
@Entity
@Table(name = "user_preference")
@Getter
@NoArgsConstructor
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pref_key", nullable = false, length = 64)
    private PreferenceKey prefKey;

    @Column(name = "value_json", nullable = false, length = 1024)
    private String valueJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
