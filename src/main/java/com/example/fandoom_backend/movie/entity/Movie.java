package com.example.fandoom_backend.movie.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "movie", uniqueConstraints = {
        @UniqueConstraint(name = "uk_movie_slug", columnNames = "slug")
}, indexes = {
        @Index(name = "idx_movie_franchise_id", columnList = "franchise_id")
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

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, length = 280)
    private String slug;

    @Column(name = "synopsis", columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "runtime_minutes")
    private Integer runtimeMinutes;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

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
}
