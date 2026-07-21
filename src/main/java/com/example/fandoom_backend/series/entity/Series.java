package com.example.fandoom_backend.series.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "series", uniqueConstraints = {
        @UniqueConstraint(name = "uk_series_slug", columnNames = "slug")
}, indexes = {
        @Index(name = "idx_series_franchise_id", columnList = "franchise_id")
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

    @Column(nullable = false, unique = true, length = 280)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "first_air_date")
    private LocalDate firstAirDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeriesStatus status;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

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

    // Aynı modül içi aggregate ilişkisi: gerçek JPA @OneToMany
    @OneToMany(mappedBy = "series", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
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
