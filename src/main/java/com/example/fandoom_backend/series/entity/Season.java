package com.example.fandoom_backend.series.entity;

import com.example.fandoom_backend.common.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "season", uniqueConstraints = {
        @UniqueConstraint(name = "uk_season_series_number", columnNames = {"series_id", "season_number"})
}, indexes = {
        @Index(name = "idx_season_series_id", columnList = "series_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString
public class Season extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(length = 255)
    private String title;

    @Column(name = "air_date")
    private LocalDate airDate;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_season_series"))
    @ToString.Exclude
    private Series series;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("episodeNumber ASC")
    @ToString.Exclude
    @Builder.Default
    private List<Episode> episodes = new ArrayList<>();

    public void addEpisode(Episode episode) {
        episodes.add(episode);
        episode.setSeason(this);
    }

    public void removeEpisode(Episode episode) {
        episodes.remove(episode);
        episode.setSeason(null);
    }
}
