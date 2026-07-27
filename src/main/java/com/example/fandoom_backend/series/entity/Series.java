package com.example.fandoom_backend.series.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "series", uniqueConstraints = {
        @UniqueConstraint(name = "uk_series_slug", columnNames = "slug"),
        @UniqueConstraint(name = "uk_series_imdb_id", columnNames = "imdb_id"),
        @UniqueConstraint(name = "uk_series_tmdb_id", columnNames = "tmdb_id")
}, indexes = {
        @Index(name = "idx_series_franchise_id", columnList = "franchise_id"),
        @Index(name = "idx_series_first_air_date_id", columnList = "first_air_date DESC, id DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Series extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "original_title", length = 255)
    private String originalTitle;

    @Column(nullable = false, unique = true, length = 280)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "first_air_date")
    private LocalDate firstAirDate;

    @Column(name = "last_air_date")
    private LocalDate lastAirDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeriesStatus status;

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
            name = "series_genres",
            joinColumns = @JoinColumn(name = "series_id",
                    foreignKey = @ForeignKey(name = "fk_series_genres_series")),
            uniqueConstraints = @UniqueConstraint(columnNames = {"series_id", "genre_id"})
    )
    @Column(name = "genre_id", nullable = false)
    @Builder.Default
    private Set<Long> genreIds = new HashSet<>();

    // Cross-module referans: sadece ID, JPA ilişkisi YOK (Person aynı modülde değil)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "series_producers",
            joinColumns = @JoinColumn(name = "series_id",
                    foreignKey = @ForeignKey(name = "fk_series_producers_series")),
            uniqueConstraints = @UniqueConstraint(columnNames = {"series_id", "person_id"})
    )
    @Column(name = "person_id", nullable = false)
    @Builder.Default
    private Set<Long> producerIds = new HashSet<>();

    // Aynı modül içi aggregate ilişkisi: gerçek JPA @OneToMany
    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("seasonNumber ASC")
    @ToString.Exclude
    @Builder.Default
    private List<Season> seasons = new ArrayList<>();

    public void addSeason(Season season) {
        seasons.add(season);
        season.setSeries(this);
    }

    public void removeSeason(Season season) {
        seasons.remove(season);
        season.setSeries(null);
    }
}
