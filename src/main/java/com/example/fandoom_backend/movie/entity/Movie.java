package com.example.fandoom_backend.movie.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "movie", uniqueConstraints = {
        @UniqueConstraint(name = "uk_movie_slug", columnNames = "slug"),
        @UniqueConstraint(name = "uk_movie_imdb_id", columnNames = "imdb_id"),
        @UniqueConstraint(name = "uk_movie_tmdb_id", columnNames = "tmdb_id")
}, indexes = {
        @Index(name = "idx_movie_franchise_id", columnList = "franchise_id"),
        @Index(name = "idx_movie_release_date_id", columnList = "release_date DESC, id DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Movie extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "title_tr", length = 255)
    private String titleTr;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "original_title", length = 255)
    private String originalTitle;

    @Column(name = "slug", nullable = false, length = 280)
    private String slug;

    @Column(name = "synopsis_tr", columnDefinition = "TEXT")
    private String synopsisTr;

    @Column(name = "synopsis", columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "tagline_tr", length = 500)
    private String taglineTr;

    @Column(name = "tagline", length = 500)
    private String tagline;

    // Nullable: mevcut satırlar için movie_lifecycle_migration.sql backfill eder,
    // yeni kayıtta servis null gelirse releaseDate'ten türetir.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private MovieStatus status;

    // USD (TMDB budget/revenue ile aynı birim), tam sayı. null = bilinmiyor
    // (servis 0'ı null'a çevirir — TMDB "bilinmiyor" için 0 döner).
    @Column(name = "budget")
    private Long budget;

    @Column(name = "box_office")
    private Long boxOffice;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "runtime_minutes")
    private Integer runtimeMinutes;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "trailer_url", length = 500)
    private String trailerUrl;

    @Column(name = "content_rating", length = 10)
    private String contentRating;

    @Column(name = "origin_country", length = 2)
    private String originCountry;

    @Column(name = "original_language", length = 2)
    private String originalLanguage;

    @Column(name = "external_rating", precision = 3, scale = 1)
    private BigDecimal externalRating;

    @Column(name = "external_vote_count")
    private Integer externalVoteCount;

    @Column(name = "external_rating_updated_at")
    private LocalDateTime externalRatingUpdatedAt;

    @Column(name = "imdb_id", length = 15)
    private String imdbId;

    @Column(name = "tmdb_id")
    private Integer tmdbId;

    // Cross-module referans: sadece ID, JPA ilişkisi YOK
    @Column(name = "franchise_id")
    private Long franchiseId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id",
                    foreignKey = @ForeignKey(name = "fk_movie_genres_movie")),
            uniqueConstraints = @UniqueConstraint(columnNames = {"movie_id", "genre_id"})
    )
    @Column(name = "genre_id", nullable = false)
    @Builder.Default
    private Set<Long> genreIds = new HashSet<>();

    // Cross-module referans: sadece ID, JPA ilişkisi YOK (Person aynı modülde değil)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "movie_producers",
            joinColumns = @JoinColumn(name = "movie_id",
                    foreignKey = @ForeignKey(name = "fk_movie_producers_movie")),
            uniqueConstraints = @UniqueConstraint(columnNames = {"movie_id", "person_id"})
    )
    @Column(name = "person_id", nullable = false)
    @Builder.Default
    private Set<Long> producerIds = new HashSet<>();

    // Cross-module referans: sadece ID, JPA ilişkisi YOK (producerIds ile aynı desen)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "movie_directors",
            joinColumns = @JoinColumn(name = "movie_id",
                    foreignKey = @ForeignKey(name = "fk_movie_directors_movie")),
            uniqueConstraints = @UniqueConstraint(columnNames = {"movie_id", "person_id"}),
            // "bu yönetmenin filmleri" (person → movie) yönü için
            indexes = @Index(name = "idx_movie_directors_person", columnList = "person_id, movie_id")
    )
    @Column(name = "person_id", nullable = false)
    @Builder.Default
    private Set<Long> directorIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "movie_writers",
            joinColumns = @JoinColumn(name = "movie_id",
                    foreignKey = @ForeignKey(name = "fk_movie_writers_movie")),
            uniqueConstraints = @UniqueConstraint(columnNames = {"movie_id", "person_id"}),
            indexes = @Index(name = "idx_movie_writers_person", columnList = "person_id, movie_id")
    )
    @Column(name = "person_id", nullable = false)
    @Builder.Default
    private Set<Long> writerIds = new HashSet<>();
}
