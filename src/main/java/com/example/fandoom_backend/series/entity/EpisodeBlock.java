package com.example.fandoom_backend.series.entity;

import com.example.fandoom_backend.content.entity.ContentBlock;
import jakarta.persistence.*;
import lombok.*;

// Bölümün editöryel derin-analiz ("story") gövdesi — sıralı, tipli blok
// listesi (BlogBlock ile aynı desen). sceneKey aynı sahneye ait blokları
// gruplar (CMS'teki orderIndex-grup desenine benzer, ama sahne ayrı bir
// tablo değil — DB'de gerçek bir ilişki yok, sadece ortak bir anahtar).
// id/orderIndex/col/row ContentBlock'tan miras.
@Entity
@Table(name = "episode_block", indexes = {
        @Index(name = "idx_episode_block_episode", columnList = "episode_id")
})
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString
public class EpisodeBlock extends ContentBlock {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_episode_block_episode"))
    @ToString.Exclude
    private Episode episode;

    @Column(name = "scene_key", nullable = false, length = 100)
    private String sceneKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "tone", length = 20)
    private EpisodeSceneTone tone;

    @Column(name = "pinned", nullable = false)
    @Builder.Default
    private boolean pinned = false;

    @Column(name = "scene_kicker_tr", length = 255)
    private String sceneKickerTr;

    @Column(name = "scene_kicker", length = 255)
    private String sceneKicker;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 20)
    private EpisodeBlockType blockType;

    // TITLE/QUOTE metni, ya da TEXT/LEAD_TEXT'in paragrafları "\n\n" ile
    // birleştirilmiş; MEDIA'da null.
    @Column(name = "content_tr", columnDefinition = "TEXT")
    private String contentTr;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "media_url", length = 500)
    private String mediaUrl;

    @Column(name = "media_alt_tr", length = 255)
    private String mediaAltTr;

    @Column(name = "media_alt", length = 255)
    private String mediaAlt;

    @Column(name = "media_ratio", length = 20)
    private String mediaRatio;
}
