package com.example.fandoom_backend.community.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// authorId/productionSlug: cross-module referans, sadece ID/slug (JPA
// ilişkisi YOK — CLAUDE.md cross-module ID-only kuralı). likeCount/
// commentCount/bookmarkCount denormalized sayaçlardır, ThreadRepository'deki
// native @Modifying sorgularla güncellenir (read-modify-write DEĞİL, race
// condition'a karşı).
@Entity
@Table(name = "thread", uniqueConstraints = {
        @UniqueConstraint(name = "uk_thread_slug", columnNames = "slug")
}, indexes = {
        // WHERE status=? [AND surface=?] ORDER BY <sort> DESC, id DESC — her (filtre, sıralama)
        // kombinasyonu için indeks sıralı okunur, filesort yok. InnoDB ikincil indeksler PK'yı (id) örtük
        // olarak ASC ekler: sorgunun tie-breaker'ı da id ASC olmalı (x DESC, id DESC) ters yönde filesort'a
        // düşer (EXPLAIN ile doğrulandı) — bkz. KeysetSpecification.
        @Index(name = "idx_thread_status_surface_hot", columnList = "status, surface, hot_score DESC"),
        @Index(name = "idx_thread_status_surface_created", columnList = "status, surface, created_at DESC"),
        @Index(name = "idx_thread_status_surface_likes", columnList = "status, surface, like_count DESC"),
        // surface filtresi OLMAYAN akış (GET /api/community/feed, surface=null): (status, surface, x)
        // indeksleri ORDER BY'ı karşılayamaz (araya surface girer) -> ayrı (status, x) indeksleri.
        @Index(name = "idx_thread_status_hot", columnList = "status, hot_score DESC"),
        @Index(name = "idx_thread_status_created", columnList = "status, created_at DESC"),
        @Index(name = "idx_thread_status_likes", columnList = "status, like_count DESC"),
        @Index(name = "idx_thread_status_production_hot", columnList = "status, production_slug, hot_score DESC"),
        @Index(name = "idx_thread_status_production_created", columnList = "status, production_slug, created_at DESC"),
        // countByAuthorIdAndSurfaceAndStatus (profil sayaçları) + author_id önek kullanımı
        @Index(name = "idx_thread_author_surface_status", columnList = "author_id, surface, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Thread extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 255)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ThreadSurface surface;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "spoiler_flagged", nullable = false)
    @Builder.Default
    private boolean spoilerFlagged = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ThreadStatus status = ThreadStatus.PUBLISHED;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "production_slug", length = 255)
    private String productionSlug;

    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    @Builder.Default
    private int commentCount = 0;

    @Column(name = "bookmark_count", nullable = false)
    @Builder.Default
    private int bookmarkCount = 0;

    @Column(name = "hot_score", nullable = false)
    @Builder.Default
    private double hotScore = 0;

    @Column(name = "quality_score", nullable = false)
    @Builder.Default
    private double qualityScore = 0;
}
