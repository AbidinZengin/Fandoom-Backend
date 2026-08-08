package com.example.fandoom_backend.series.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "episode", uniqueConstraints = {
        @UniqueConstraint(name = "uk_episode_season_number", columnNames = {"season_id", "episode_number"}),
        @UniqueConstraint(name = "uk_episode_imdb_id", columnNames = "imdb_id"),
        @UniqueConstraint(name = "uk_episode_tmdb_id", columnNames = "tmdb_id")
}, indexes = {
        @Index(name = "idx_episode_season_id", columnList = "season_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Episode extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "synopsis", columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "air_date")
    private LocalDate airDate;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "still_image_url", length = 500)
    private String stillImageUrl;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_episode_season"))
    @ToString.Exclude
    private Season season;

    // Editöryel derin-analiz ("story") başlığı — tüm bölümlerde dolu olmak
    // zorunda değil, yazılmamış bölümlerde null (bkz. EpisodeServiceImpl).
    @Column(name = "story_kicker", length = 255)
    private String storyKicker;

    @Column(name = "story_title", length = 255)
    private String storyTitle;

    @Column(name = "story_thesis", columnDefinition = "TEXT")
    private String storyThesis;

    @OneToMany(mappedBy = "episode", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    @ToString.Exclude
    @Builder.Default
    private List<EpisodeBlock> episodeBlocks = new ArrayList<>();

    public void addEpisodeBlock(EpisodeBlock block) {
        episodeBlocks.add(block);
        block.setEpisode(this);
    }

    public void clearEpisodeBlocks() {
        episodeBlocks.forEach(block -> block.setEpisode(null));
        episodeBlocks.clear();
    }
}
