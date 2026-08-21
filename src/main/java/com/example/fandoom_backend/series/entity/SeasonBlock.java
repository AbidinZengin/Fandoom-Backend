package com.example.fandoom_backend.series.entity;

import com.example.fandoom_backend.content.entity.ContentBlock;
import jakarta.persistence.*;
import lombok.*;

// Sezonun editöryel derin-analiz ("story") gövdesi — EpisodeBlock ile
// birebir aynı desen (sceneKey aynı sahneye ait blokları gruplar, DB'de
// gerçek bir ilişki yok, sadece ortak bir anahtar). mediaEpisodeRef de
// aynı şekilde opak: Episode.episodeNumber'a FK DEĞİL, sadece frontend'in
// season.episodes[] listesinden stillImageUrl eşleştirmesi için anahtar.
// id/orderIndex/col/row ContentBlock'tan miras.
@Entity
@Table(name = "season_block", indexes = {
        @Index(name = "idx_season_block_season", columnList = "season_id")
})
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString
public class SeasonBlock extends ContentBlock {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_season_block_season"))
    @ToString.Exclude
    private Season season;

    @Column(name = "scene_key", nullable = false, length = 100)
    private String sceneKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 20)
    private SeasonBlockType blockType;

    // MEDIA'da null.
    @Column(name = "content_tr", columnDefinition = "TEXT")
    private String contentTr;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // Yalnızca MEDIA'da dolu — FK değil, Episode.episodeNumber'a opak referans.
    @Column(name = "media_episode_ref")
    private Integer mediaEpisodeRef;

    @Column(name = "media_caption_tr", length = 255)
    private String mediaCaptionTr;

    @Column(name = "media_caption", length = 255)
    private String mediaCaption;

    @Column(name = "media_credit", length = 255)
    private String mediaCredit;
}
