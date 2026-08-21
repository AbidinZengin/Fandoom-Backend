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

    @Column(name = "title_tr", length = 255)
    private String titleTr;

    @Column(name = "title", length = 255)
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

    // Editöryel derin-analiz ("story") başlığı — tüm sezonlarda dolu olmak
    // zorunda değil, yazılmamış sezonlarda null (Episode.storyKicker/Title
    // ile aynı konvansiyon).
    @Column(name = "story_kicker_tr", length = 255)
    private String storyKickerTr;

    @Column(name = "story_kicker", length = 255)
    private String storyKicker;

    @Column(name = "story_title_tr", length = 255)
    private String storyTitleTr;

    @Column(name = "story_title", length = 255)
    private String storyTitle;

    // Episode'daki storyThesis'in sezon karşılığı — bilinçli farklı isim
    // (tek cümlelik alt başlık, "dek").
    @Column(name = "story_dek_tr", columnDefinition = "TEXT")
    private String storyDekTr;

    @Column(name = "story_dek", columnDefinition = "TEXT")
    private String storyDek;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("episodeNumber ASC")
    @ToString.Exclude
    @Builder.Default
    private List<Episode> episodes = new ArrayList<>();

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    @ToString.Exclude
    @Builder.Default
    private List<SeasonBlock> seasonBlocks = new ArrayList<>();

    public void addEpisode(Episode episode) {
        episodes.add(episode);
        episode.setSeason(this);
    }

    public void removeEpisode(Episode episode) {
        episodes.remove(episode);
        episode.setSeason(null);
    }

    public void addSeasonBlock(SeasonBlock block) {
        seasonBlocks.add(block);
        block.setSeason(this);
    }

    public void clearSeasonBlocks() {
        seasonBlocks.forEach(block -> block.setSeason(null));
        seasonBlocks.clear();
    }
}
