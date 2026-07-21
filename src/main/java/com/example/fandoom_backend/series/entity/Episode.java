package com.example.fandoom_backend.series.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "episode", uniqueConstraints = {
        @UniqueConstraint(name = "uk_episode_season_number", columnNames = {"season_id", "episode_number"})
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_episode_season"))
    @ToString.Exclude
    private Season season;
}
